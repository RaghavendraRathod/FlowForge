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
    private final JobRepository jobRepository;

    public WorkerService(JobClaimService jobClaimService,
                          JobRepository jobRepository) {
        this.jobClaimService = jobClaimService;
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void processNextJob() {

        Optional<Job> optionalJob = jobClaimService.claimNextJob();

        if (optionalJob.isEmpty()) {
            return;
        }

        Job job = optionalJob.get();

        try {
            System.out.println("Worker executing job: " + job.getId());

            Thread.sleep(2000);

            completeJob(job);

            System.out.println("Worker completed job: " + job.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failJob(job);
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