package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;
    private final WorkerRepository workerRepository;

    public JobClaimService(
            JobRepository jobRepository,
            WorkerRepository workerRepository
    ) {
        this.jobRepository = jobRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public Optional<Job> claimNextJob(UUID workerId) {

        if (workerId == null) {
            throw new IllegalArgumentException(
                    "Worker ID must not be null"
            );
        }

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Worker not found: " + workerId
                        )
                );

        if (worker.getStatus() != WorkerStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Worker is not active: " + workerId
            );
        }

        Optional<Job> optionalJob =
                jobRepository.findNextQueuedJobForUpdate();

        if (optionalJob.isEmpty()) {
            return Optional.empty();
        }

        Job job = optionalJob.get();

        Instant now = Instant.now();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(now);
        job.setLeaseUntil(now.plusSeconds(60));
        job.setWorkerId(workerId);

        jobRepository.save(job);

        return Optional.of(job);
    }
}