package ru.students.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;
import ru.students.utils.Peers;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Map;

public interface TrackerService {


    String announce(String hashInfo) throws IOException;

    Map<String, Peers> getCountOfPeers(String hash);
    void getPeers(String hash);

    String getHash(MultipartFile torrentFile) throws IOException;

    double getAllTorrentsSize() throws IOException;

    boolean deleteTorrent(String hash) throws IOException;

    FileSystemResource getTorrentByHashInfo(String hashInfo);
}
