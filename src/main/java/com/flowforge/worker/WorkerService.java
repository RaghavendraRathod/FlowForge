package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class WorkerService {

    private final JobClaimService jobClaimService;
    private final JobRecoveryService jobRecoveryService;
    private final JobRepository jobRepository;
    private final TaskExecutor taskExecutor;

    public WorkerService(JobClaimService jobClaimService,
                         JobRecoveryService jobRecoveryService,
                         JobRepository jobRepository,
                         TaskExecutor taskExecutor) {
       this.jobClaimService = jobClaimService;
       this.jobRecoveryService = jobRecoveryService;
       this.jobRepository = jobRepository;
       this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelay = 5000)
    public void processNextJob() {

        jobRecoveryService.recoverExpiredJob();

        Optional<Job> optionalJob = jobClaimService.claimNextJob();

        if (optionalJob.isEmpty()) {
            return;
        }

        Job job = optionalJob.get();

        try {
            System.out.println("Worker executing job: " + job.getId());

            String result = taskExecutor.execute(job);

            job.setResult(result);
            job.setErrorMessage(null);

            completeJob(job);

            System.out.println("Worker completed job: " + job.getId());
            System.out.println("Job result: " + result);

        } catch (Exception e) {
            System.out.println("Job failed: " + e.getMessage());

            job.setResult(null);
            job.setErrorMessage(e.getMessage());

            if (job.getRetryCount() < job.getMaxRetries()) {

                job.setRetryCount(job.getRetryCount() + 1);
                job.setStatus(JobStatus.QUEUED);
                job.setStartedAt(null);

                jobRepository.save(job);

                System.out.println(
                        "Retrying job " + job.getId()
                                + " (retry " + job.getRetryCount()
                                + "/" + job.getMaxRetries() + ")"
              );

            } else {

                failJob(job);

                System.out.println(
                        "Job permanently failed after "
                                + job.getRetryCount()
                                + " retries: " + job.getId()
              );
            }
        }
    }

    @Transactional
    public void completeJob(Job job) {

        job.setStatus(JobStatus.SUCCEEDED);
        job.setCompletedAt(Instant.now());

        jobRepository.save(job);
    }

    @Transactional
    public void failJob(Job job) {

        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());

        jobRepository.save(job);
    }
}