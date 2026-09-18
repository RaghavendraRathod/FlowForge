package com.flowforge.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query(value = """
            SELECT *
            FROM job
            WHERE status = 'QUEUED'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<Job> findNextQueuedJobForUpdate();
}