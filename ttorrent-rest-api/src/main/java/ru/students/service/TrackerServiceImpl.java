package ru.students.service;

import com.turn.ttorrent.common.TorrentMetadata;
import com.turn.ttorrent.common.TorrentParser;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.students.utils.Peers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class TrackerServiceImpl implements TrackerService {
    private final Tracker tracker;
    public static String STAGE_DIRECTORY = System.getProperty("user.dir") + "/staging";

    public TrackerServiceImpl(Tracker tracker) {
        this.tracker = tracker;
    }

    /**
     *
     * @param torrentFile
     * Полученный торрент-файл от сервиса форума
     * @return
     * Хеш сумма торрента
     * @throws IOException
     * Ошибка при чтении торрент-файла
     */
    public String announce(MultipartFile torrentFile) throws IOException {
        Path fileNameAndPath = Paths.get(STAGE_DIRECTORY, torrentFile.getOriginalFilename());
        Files.write(fileNameAndPath, torrentFile.getBytes());

        File file = new File(String.valueOf(fileNameAndPath));

        TrackedTorrent trackedTorrent = TrackedTorrent.load(file);
        TorrentMetadata torrentMetadata = new TorrentParser().parseFromFile(file);
        log.info("Torrent name {} torrent count of pieces {} piece size {}",
                torrentMetadata.getDirectoryName(),
                torrentMetadata.getPiecesCount(),
                torrentMetadata.getPieceLength());

        tracker.announce(trackedTorrent);
        return trackedTorrent.getHexInfoHash();
    }

    /**
     *
     * @param hash
     * Хеш сумма торрента
     * @return
     * Количество активных пиров (сидеров и личеров)
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
        Map<String,Peers> peersMap = new HashMap<>();

        for (TrackedTorrent trackedTorrent : tracker.getTrackedTorrents()) {
                Peers peers = new Peers();
                peers.setLeechers(trackedTorrent.leechers());
                peers.setSeeders(trackedTorrent.seeders());

                peersMap.put(trackedTorrent.getHexInfoHash(), peers);
        }
        return peersMap;
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


}
