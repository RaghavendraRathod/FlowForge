package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import java.util.UUID;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
public class WorkerService {

    private final JobClaimService jobClaimService;
    private final JobRecoveryService jobRecoveryService;
    private final JobRepository jobRepository;
    private final TaskExecutor taskExecutor;
    private final WorkerRepository workerRepository;
    private UUID workerId;

    @PostConstruct
    public void initializeWorker() {

        String workerName = "worker-1";

        Optional<Worker> existingWorker =
                workerRepository.findByName(workerName);

        if (existingWorker.isPresent()) {

            Worker worker = existingWorker.get();

            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setLastHeartbeat(Instant.now());

            Worker savedWorker = workerRepository.save(worker);

            this.workerId = savedWorker.getId();

            System.out.println(
                    "FlowForge worker reconnected: " + this.workerId
            );

        } else {

            Worker worker = new Worker(workerName);

            Worker savedWorker = workerRepository.save(worker);

            this.workerId = savedWorker.getId();

            System.out.println(
                    "FlowForge worker registered: " + this.workerId
            );
        } 
    }

    public WorkerService(JobClaimService jobClaimService,
                         JobRecoveryService jobRecoveryService,
                         JobRepository jobRepository,
                         TaskExecutor taskExecutor,
                         WorkerRepository workerRepository) {
        this.jobClaimService = jobClaimService;
        this.jobRecoveryService = jobRecoveryService;
        this.jobRepository = jobRepository;
        this.taskExecutor = taskExecutor;
        this.workerRepository = workerRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void processNextJob() {

        jobRecoveryService.recoverExpiredJob();

        Optional<Job> optionalJob = jobClaimService.claimNextJob(workerId);

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
                job.setLeaseUntil(null);
                job.setWorkerId(null);

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
        job.setLeaseUntil(null);

        jobRepository.save(job);
    }

    @Transactional
    public void failJob(Job job) {

        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setLeaseUntil(null);

        jobRepository.save(job);
    }

    public Worker registerWorker(String name) {

        Worker worker = new Worker(name);

        return workerRepository.save(worker);
    }

    public Worker heartbeat(UUID workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        worker.setLastHeartbeat(Instant.now());
        worker.setStatus(WorkerStatus.ACTIVE);

        return workerRepository.save(worker);
    }

    public java.util.List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public Worker getWorker(UUID workerId) {

        return workerRepository.findById(workerId)
                .orElseThrow(() ->
                        new RuntimeException("Worker not found"));
   }

    public WorkerStatus determineHealth(Worker worker) {

       Instant now = Instant.now();

       long secondsSinceHeartbeat =
               java.time.Duration.between(
                    worker.getLastHeartbeat(),
                    now
                ).getSeconds();

        if (secondsSinceHeartbeat <= 15) {
            return WorkerStatus.ACTIVE;
        }

        if (secondsSinceHeartbeat <= 30) {
           return WorkerStatus.STALE;
        }

        return WorkerStatus.OFFLINE;
    }

    @Scheduled(fixedDelay = 5000)
    public void sendHeartbeat() {

        if (workerId == null) {
            return;
        }

        heartbeat(workerId);

        System.out.println(
               "Worker heartbeat: " + workerId
        );
    }

    @Scheduled(fixedDelay = 10000)
    public void monitorWorkerHealth() {

        List<Worker> workers = workerRepository.findAll();

        for (Worker worker : workers) {

            WorkerStatus health = determineHealth(worker);

            if (worker.getStatus() != health) {

                worker.setStatus(health);
                workerRepository.save(worker);

                System.out.println(
                        "Worker " + worker.getId() +
                        " status changed to " + health
               );
            }
        }
    }
}