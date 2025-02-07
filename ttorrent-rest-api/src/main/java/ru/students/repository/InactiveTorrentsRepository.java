package ru.students.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.students.entity.InactiveTorrent;

@Repository
public interface InactiveTorrentsRepository extends JpaRepository<InactiveTorrent, String> {
    boolean existsByHash(String hash);
}
