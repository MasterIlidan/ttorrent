package ru.students.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Torrent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotNull
    private String hashInfo;
    @NotNull
    private String fileName;
    @NotNull
    private Status status;
    @NotNull
    private Timestamp registered;
    private Timestamp inactiveSince;

    public enum Status {
        NEW, ACTIVE, INACTIVE, ARCHIVE
    }
}
