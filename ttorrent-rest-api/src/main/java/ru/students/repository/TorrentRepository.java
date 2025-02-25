package ru.students.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.students.entity.Torrent;

import java.util.List;

@Repository
public interface TorrentRepository extends JpaRepository<Torrent, Long> {

    Torrent findByHashInfoAndStatus(String hashInfo, Torrent.Status status);

    List<Torrent> findAllByStatus(Torrent.Status status);

    void deleteByHashInfo(String hashInfo);

    List<Torrent> findAllByHashInfo(String hashInfo);

    Torrent findByHashInfo(String hashInfo);

    void deleteAllByHashInfo(String hashInfo);

    boolean existsByHashInfo(String hashInfo);
}
