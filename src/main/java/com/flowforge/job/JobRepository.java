package com.flowforge.job;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Job> findFirstByStatusOrderByCreatedAtAsc(JobStatus status);
}