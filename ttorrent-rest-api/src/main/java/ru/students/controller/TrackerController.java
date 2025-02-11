package ru.students.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.students.service.TrackerServiceImpl;
import ru.students.utils.Peers;

import java.io.IOException;
import java.util.Map;


@Controller
public class TrackerController {

    private final TrackerServiceImpl trackerService;


    public TrackerController(TrackerServiceImpl trackerService) {
        this.trackerService = trackerService;
    }

    @PostMapping("/announce")
    public ResponseEntity startTracker(@RequestParam String hashInfo) throws IOException {
        trackerService.announce(hashInfo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerNewTorrent(@RequestParam("torrentFile") MultipartFile torrentFile,
                                                     @RequestParam("name") String name) throws IOException {

        return new ResponseEntity<>(trackerService.registerNewTorrent(torrentFile, name),
                HttpStatus.OK);
    }

    @PostMapping("/getHash")
    public ResponseEntity<String> getHash(@RequestParam("torrentFile") MultipartFile torrentFile) throws IOException {
        String hash = trackerService.getHash(torrentFile);
        return new ResponseEntity<>(hash, HttpStatus.OK);
    }

    @GetMapping("/getPeers")
    public ResponseEntity<Map<String, Peers>> getPeers() {
        Map<String, Peers> peers = trackerService.getAllCountOfPeers();
        return new ResponseEntity<>(peers, HttpStatus.OK);
    }
    @GetMapping("/getSizeOfTracker")
    public ResponseEntity<Double> getSizeOfTracker() throws IOException {
        Double trackerSize = trackerService.getAllTorrentsSize();
        return new ResponseEntity<>(trackerSize, HttpStatus.OK);
    }
}