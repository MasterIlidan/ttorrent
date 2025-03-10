package ru.students;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.students.service.TrackerServiceImpl.STAGE_DIRECTORY;

@SpringBootApplication
public class TrackerServiceApp {
    public static void main(String[] args) throws IOException {
        if (Files.notExists(Path.of(STAGE_DIRECTORY))) {
            Files.createDirectory(Path.of(STAGE_DIRECTORY));
        }
        SpringApplication.run(TrackerServiceApp.class, args);
    }
}
