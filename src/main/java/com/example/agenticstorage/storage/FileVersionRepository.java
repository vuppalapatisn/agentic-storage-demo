package com.example.agenticstorage.storage;

import com.example.agenticstorage.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    @Query("SELECT f FROM FileVersion f WHERE f.sandbox = :sandbox AND f.filePath = :path " +
           "AND f.archived = false ORDER BY f.version DESC LIMIT 1")
    Optional<FileVersion> findLatest(@Param("sandbox") String sandbox,
                                     @Param("path") String path);

    Optional<FileVersion> findBySandboxAndFilePathAndVersion(
            String sandbox, String filePath, int version);

    List<FileVersion> findBySandboxAndFilePathOrderByVersionAsc(
            String sandbox, String filePath);

    @Query("SELECT DISTINCT f.filePath FROM FileVersion f WHERE f.sandbox = :sandbox " +
           "AND f.archived = false")
    List<String> findActiveFilesInSandbox(@Param("sandbox") String sandbox);

    @Query("SELECT COALESCE(MAX(f.version), 0) FROM FileVersion f " +
           "WHERE f.sandbox = :sandbox AND f.filePath = :path")
    int findMaxVersion(@Param("sandbox") String sandbox, @Param("path") String path);
}
