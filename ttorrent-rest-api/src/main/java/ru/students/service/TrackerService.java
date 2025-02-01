package ru.students.service;

import org.springframework.web.multipart.MultipartFile;
import ru.students.utils.Peers;

import java.io.IOException;
import java.util.Map;

public interface TrackerService {

    String announce(MultipartFile torrentFile) throws IOException;
    Map<String, Peers> getCountOfPeers(String hash);
    void getPeers(String hash);

    double getAllTorrentsSize() throws IOException;
}
