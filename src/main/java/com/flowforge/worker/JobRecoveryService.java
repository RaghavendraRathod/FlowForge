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

        job.setRetryCount(job.getRetryCount() + 1);
        job.setStartedAt(null);
        job.setLeaseUntil(null);
        job.setWorkerId(null);

        if (job.getRetryCount() > job.getMaxRetries()) {

            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());

            if (job.getErrorMessage() == null
                    || job.getErrorMessage().isBlank()) {
                job.setErrorMessage(
                        "Job lease expired after maximum retries"
                );
            }

            System.out.println(
                    "Expired job permanently failed: " + job.getId()
            );

        } else {

            job.setStatus(JobStatus.QUEUED);
            job.setCompletedAt(null);

            System.out.println(
                    "Recovered expired job: " + job.getId()
                            + " (retry " + job.getRetryCount()
                            + "/" + job.getMaxRetries() + ")"
            );
        }

        Job savedJob = jobRepository.save(job);

        return Optional.of(savedJob);
    }
}
