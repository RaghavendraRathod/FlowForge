package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class JobRecoveryService {

    private final JobRepository jobRepository;

    public JobRecoveryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<Job> recoverExpiredJob() {

        Optional<Job> optionalJob =
                jobRepository.findExpiredRunningJobForUpdate();

        if (optionalJob.isEmpty()) {
            return Optional.empty();
        }

        Job job = optionalJob.get();

        job.setStatus(JobStatus.QUEUED);
        job.setStartedAt(null);
        job.setLeaseUntil(null);

        jobRepository.save(job);

        System.out.println(
                "Recovered expired job: " + job.getId()
        );

        return Optional.of(job);
    }
}
