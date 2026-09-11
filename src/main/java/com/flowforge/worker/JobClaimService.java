package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<Job> claimNextJob() {

        Optional<Job> optionalJob =
        jobRepository.findFirstByStatusOrderByCreatedAtAsc(JobStatus.QUEUED);

        if (optionalJob.isEmpty()) {
            return Optional.empty();
        }

        Job job = optionalJob.get();

        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        jobRepository.save(job);

        return Optional.of(job);
    }
}
