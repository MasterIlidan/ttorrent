package ru.students.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.students.entity.Torrent;

@Repository
public interface TorrentRepository extends JpaRepository<Torrent, Long> {

    Torrent findByHashInfoAndStatus(String hashInfo, Torrent.Status status);
}
