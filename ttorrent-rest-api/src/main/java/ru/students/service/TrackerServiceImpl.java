package ru.students.service;

import com.turn.ttorrent.common.TorrentMetadata;
import com.turn.ttorrent.common.TorrentParser;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.students.entity.Torrent;
import ru.students.repository.TorrentRepository;
import ru.students.utils.Peers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TrackerServiceImpl implements TrackerService {
    private final Tracker tracker;
    public static String STAGE_DIRECTORY = System.getProperty("user.dir") + "/staging";
    private final TorrentRepository torrentRepository;

    public TrackerServiceImpl(Tracker tracker, TorrentRepository torrentRepository) {
        this.tracker = tracker;
        this.torrentRepository = torrentRepository;
    }

    /**
     * @param hashInfo Хеш раздачи, которую нужно анонсировать. Она должна быть в базе со статусом NEW
     * @return Хеш сумма торрента
     * @throws IOException Ошибка при чтении торрент-файла
     */
    @Override
    public String announce(String hashInfo) throws IOException {
        Torrent torrent = torrentRepository.findByHashInfoAndStatus(hashInfo, Torrent.Status.NEW);
        File file = new File(String.valueOf(Paths.get(STAGE_DIRECTORY, torrent.getFileName())));
        TorrentMetadata torrentMetadata = new TorrentParser().parseFromFile(file);
        log.info("Torrent name {} torrent count of pieces {} piece size {} announcing...",
                torrentMetadata.getDirectoryName(),
                torrentMetadata.getPiecesCount(),
                torrentMetadata.getPieceLength());

        TrackedTorrent trackedTorrent = TrackedTorrent.load(file);
        torrent.setStatus(Torrent.Status.ACTIVE);

        tracker.announce(trackedTorrent);

        torrentRepository.save(torrent);
        return trackedTorrent.getHexInfoHash();
    }

    public String registerNewTorrent(MultipartFile torrentFile, String name) throws IOException {
        TorrentMetadata torrentMetadata = new TorrentParser().parse(torrentFile.getBytes());

        if (torrentRepository.existsByHashInfo(torrentMetadata.getHexInfoHash())) {
            log.warn("Раздача с таким хешем уже есть на трекере, регистрация отклонена");
            return "";
        }

        Path fileNameAndPath = Paths.get(STAGE_DIRECTORY, name + ".torrent");
        Files.write(fileNameAndPath, torrentFile.getBytes());

        Torrent torrent = new Torrent();
        torrent.setStatus(Torrent.Status.NEW);
        torrent.setHashInfo(torrentMetadata.getHexInfoHash());
        torrent.setFileName(name + ".torrent");
        torrent.setRegistered(new Timestamp(new Date().getTime()));

        torrentRepository.save(torrent);

        return torrentMetadata.getHexInfoHash();
    }

    /**
     * @param hash Хеш сумма торрента
     * @return Количество активных пиров (сидеров и личеров)
     */
    @Override
    public Map<String, Peers> getCountOfPeers(String hash) {

        TrackedTorrent trackedTorrent = tracker.getTrackedTorrent(hash);
        if (trackedTorrent == null) {
            throw new NoSuchElementException("Раздачи не существует");
        }
        Map<String, Peers> peers = new HashMap<>();

        Peers peer = new Peers(trackedTorrent.leechers(), trackedTorrent.seeders());

        peers.put(hash, peer);
        return peers;
    }

    @Override
    public void getPeers(String hash) {

    }

    public Map<String, Peers> getAllCountOfPeers() {
        Map<String, Peers> peersMap = new HashMap<>();

        for (TrackedTorrent trackedTorrent : tracker.getTrackedTorrents()) {
            Peers peers = new Peers();
            peers.setLeechers(trackedTorrent.leechers());
            peers.setSeeders(trackedTorrent.seeders());

            peersMap.put(trackedTorrent.getHexInfoHash(), peers);
        }
        return peersMap;
    }

    @Override
    public String getHash(MultipartFile torrentFile) throws IOException {
        TorrentMetadata torrentMetadata = new TorrentParser().parse(torrentFile.getBytes());
        return torrentMetadata.getHexInfoHash();
    }

    @Override
    public double getAllTorrentsSize() throws IOException {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".torrent");
            }
        };

        double size = 0;
        File[] files = new File("C:\\Users\\MasterIlidan\\IdeaProjects\\ttorrent\\staging").listFiles(filter);
        if (files != null) {
            for (File f : files) {
                TorrentMetadata torrentMetadata = new TorrentParser().parseFromFile(f);
                size += ((long) torrentMetadata.getPiecesCount() * ((double) torrentMetadata.getPieceLength() / 1024 / 1024)); //мегабайты
                log.debug("Torrent name {} torrent count of pieces {} piece size {}",
                        torrentMetadata.getDirectoryName(),
                        torrentMetadata.getPiecesCount(),
                        torrentMetadata.getPieceLength());
            }
        }
        return size;
    }

    @Scheduled(cron = "*/20 * * * * *")
    @Async
    public void updateTorrentStatus() {
        List<TrackedTorrent> inactiveTorrents = tracker.getInactiveTorrents();
        log.info("Found {} inactive torrents", inactiveTorrents.size());

        makeInactiveTorrentsActive(inactiveTorrents);

        List<Torrent> torrents = torrentRepository.findAllByStatus(Torrent.Status.ACTIVE);

        for (Torrent torrent : torrents) {
            boolean found = inactiveTorrents.stream().anyMatch(
                    inactiveTorrent -> inactiveTorrent.getHexInfoHash().equals(torrent.getHashInfo()));
            if (found) {
                torrent.setInactiveSince(new Timestamp(new Date().getTime()));
                torrent.setStatus(Torrent.Status.INACTIVE);
                postNewStatusToForum(torrent, Torrent.Status.INACTIVE);
                torrentRepository.save(torrent);
            }
        }
    }
    @Scheduled(cron = "*/30 * * * * *")
    @Async
    public void deleteTorrentByInactiveTimeout() {
        int timeout = 24;
        List<Torrent> inactiveTorrents = torrentRepository.findAllByStatus(Torrent.Status.INACTIVE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Torrent inactiveTorrent:inactiveTorrents) {
            try {
                //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date inactiveSince = sdf.parse(inactiveTorrent.getInactiveSince().toString());
                Date now = new Date();
                long diff = TimeUnit.HOURS.convert(now.getTime() - inactiveSince.getTime(), TimeUnit.MILLISECONDS);
                //long diff = TimeUnit.MINUTES.convert(now.getTime() - inactiveSince.getTime(), TimeUnit.MILLISECONDS);

                if (diff > timeout) {

                    postNewStatusToForum(inactiveTorrent, Torrent.Status.ARCHIVE);

                    log.info("Torrent {} inactive for {} hours and removed", inactiveTorrent.getHashInfo(), diff);
                    tracker.unregisterTorrent(inactiveTorrent.getHashInfo());
                    torrentRepository.delete(inactiveTorrent);
                }
                log.debug("Torrent {} inactive for {} hours", inactiveTorrent.getHashInfo(), diff);

            } catch (ParseException e) {
                log.error("Error parse timestamp", e);
            }
        }
    }

    private void postNewStatusToForum(Torrent trackedTorrent, Torrent.Status status) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("hash", trackedTorrent.getHashInfo());
        body.add("status", status.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body);

        ResponseEntity responseEntity = restTemplate.postForEntity(
                "http://localhost:8080/inactivePosts",requestEntity , ResponseEntity.class);
    }

    private void makeInactiveTorrentsActive(List<TrackedTorrent> inactiveTorrents) {
        List<Torrent> inactiveTorrentDatabase = torrentRepository.findAllByStatus(Torrent.Status.INACTIVE);
        log.info("Total count of inactive torrents is {}", inactiveTorrentDatabase.size());

        for (Torrent inactiveTorrentInDatabase : inactiveTorrentDatabase) {
            boolean found = inactiveTorrents.stream().anyMatch(
                    inactiveTorrent -> inactiveTorrent.getHexInfoHash().equals(inactiveTorrentInDatabase.getHashInfo()));

            if (!found) {
                postNewStatusToForum(inactiveTorrentInDatabase, Torrent.Status.ACTIVE);
                //inactiveTorrentsRepository.delete(inactiveTorrentInDatabase);
                inactiveTorrentInDatabase.setStatus(Torrent.Status.ACTIVE);
                inactiveTorrentInDatabase.setInactiveSince(null);
                torrentRepository.save(inactiveTorrentInDatabase);
                log.debug("Torrent {} become active", inactiveTorrentInDatabase.getHashInfo());
                continue;
            }
            log.debug("Torrent {} don't have peers. Keep this torrent inactive", inactiveTorrentInDatabase.getHashInfo());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = Exception.class)
    public boolean deleteTorrent(String hash) throws IOException {
        Torrent torrent = torrentRepository.findByHashInfo(hash);
        if (torrent == null) return false;
        torrentRepository.deleteAllByHashInfo(torrent.getHashInfo());
        boolean deleteSuccess = Files.deleteIfExists(Paths.get("C:\\Users\\MasterIlidan\\IdeaProjects\\ttorrent\\staging", torrent.getFileName()));
        log.info("Удаление файла {} торрента {} {}", torrent.getFileName(), torrent.getHashInfo(), deleteSuccess);
        return true;
    }

    @Override
    public FileSystemResource getTorrentByHashInfo(String hashInfo) {
        Torrent torrent = torrentRepository.findByHashInfo(hashInfo);
        return new FileSystemResource(Paths.get("C:\\Users\\MasterIlidan\\IdeaProjects\\ttorrent\\staging", torrent.getFileName()).toFile());
    }

    @PreDestroy
    public void shutdown(){
        log.info("Tracker stopped");
        tracker.stop();
    }
}
