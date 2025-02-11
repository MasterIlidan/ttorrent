package ru.students.service;

import com.turn.ttorrent.common.TorrentMetadata;
import com.turn.ttorrent.common.TorrentParser;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.students.entity.InactiveTorrent;
import ru.students.entity.Torrent;
import ru.students.repository.InactiveTorrentsRepository;
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
    private final InactiveTorrentsRepository inactiveTorrentsRepository;
    private final TorrentRepository torrentRepository;

    public TrackerServiceImpl(Tracker tracker, InactiveTorrentsRepository inactiveTorrentsRepository, TorrentRepository torrentRepository) {
        this.tracker = tracker;
        this.inactiveTorrentsRepository = inactiveTorrentsRepository;
        this.torrentRepository = torrentRepository;
    }

    /**
     * @param torrentFile Полученный торрент-файл от сервиса форума
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
    public void getInactiveTorrents() {
        List<TrackedTorrent> inactiveTorrents = tracker.getInactiveTorrents();
        removeActiveTorrentsFromInactiveDatabase(inactiveTorrents);

        log.info("Found {} inactive torrents", inactiveTorrents.size());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (TrackedTorrent trackedTorrent : inactiveTorrents) {
            if (!inactiveTorrentsRepository.existsByHash(trackedTorrent.getHexInfoHash())) {
                log.debug("Found inactive torrent. Hash: {}", trackedTorrent.getHexInfoHash());
                InactiveTorrent inactiveTorrent = new InactiveTorrent();
                inactiveTorrent.setInactiveSince(new Timestamp(new Date().getTime()));
                inactiveTorrent.setHash(trackedTorrent.getHexInfoHash());
                inactiveTorrentsRepository.save(inactiveTorrent);
                postNewStatusToForum(trackedTorrent, Status.INACTIVE);
                continue;
            }
            Optional<InactiveTorrent> optionalInactiveTorrent = inactiveTorrentsRepository.findById(trackedTorrent.getHexInfoHash());
            if (optionalInactiveTorrent.isEmpty()) {
                log.warn("Torrent inactive, existed in database, but now gone...");
                continue;
            }
            InactiveTorrent inactiveTorrent = optionalInactiveTorrent.get();

            deleteTorrentByInactiveTimeout(trackedTorrent, sdf, inactiveTorrent, 2);

        }


    }

    private void deleteTorrentByInactiveTimeout(TrackedTorrent trackedTorrent, SimpleDateFormat sdf, InactiveTorrent inactiveTorrent, int timeout) {
        try {
            //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(inactiveTorrent.getInactiveSince().toString());
            Date now = new Date();
            //long diff = TimeUnit.HOURS.convert(now.compareTo(date), TimeUnit.MILLISECONDS);
            long diff = TimeUnit.MINUTES.convert(now.getTime() - date.getTime(), TimeUnit.MILLISECONDS);

            if (diff > timeout) {

                postNewStatusToForum(trackedTorrent, Status.ARCHIVE);


                log.info("Torrent {} inactive for {} hours and removed", trackedTorrent.getHexInfoHash(), diff);
                tracker.unregisterTorrent(trackedTorrent);
                inactiveTorrentsRepository.deleteById(trackedTorrent.getHexInfoHash());
            }
            log.info("Torrent {} inactive for {} hours", trackedTorrent.getHexInfoHash(), diff);

        } catch (ParseException e) {
            log.error("Error parse timestamp", e);
        }
    }

    private void postNewStatusToForum(TrackedTorrent trackedTorrent, Status status) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("hash", trackedTorrent.getHexInfoHash());
        body.add("status", status.toString());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body);

        ResponseEntity responseEntity = restTemplate.postForEntity(
                "http://localhost:8080/inactivePosts",requestEntity , ResponseEntity.class);
    }

    private void removeActiveTorrentsFromInactiveDatabase(List<TrackedTorrent> inactiveTorrents) {
        List<InactiveTorrent> inactiveTorrentDatabase = inactiveTorrentsRepository.findAll();
        for (InactiveTorrent inactiveTorrentInDatabase : inactiveTorrentDatabase) {
            boolean found = false;
            for (TrackedTorrent inactiveTorrent : inactiveTorrents) {
                if (inactiveTorrentInDatabase.getHash().equals(inactiveTorrent.getHexInfoHash())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                postNewStatusToForum(tracker.getTrackedTorrent(inactiveTorrentInDatabase.getHash()), Status.ACTIVE);
                inactiveTorrentsRepository.delete(inactiveTorrentInDatabase);
                log.debug("Torrent {} become active and removed from inactive database...", inactiveTorrentInDatabase.getHash());
                continue;
            }
            log.debug("Torrent {} don't have peers. Keep this torrent in inactive database", inactiveTorrentInDatabase.getHash());
        }
    }


    enum Status {
        INACTIVE, ACTIVE, ARCHIVE
    }

}
