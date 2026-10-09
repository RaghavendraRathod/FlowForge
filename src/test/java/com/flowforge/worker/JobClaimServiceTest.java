package com.flowforge.worker;

import java.time.temporal.ChronoUnit;
import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JobClaimService.class)
class JobClaimServiceTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private JobClaimService jobClaimService;

    private Worker createActiveWorker() {
        Worker worker = new Worker("test-worker-" + UUID.randomUUID());
        worker.setStatus(WorkerStatus.ACTIVE);
        return workerRepository.saveAndFlush(worker);
    }

    @Test
    void shouldClaimQueuedJobForWorker() {

        UUID workerId = createActiveWorker().getId();

        Job job = new Job(UUID.randomUUID());

        job.setTaskType("ECHO");
        job.setPayload("Claim this job");
        job.setStatus(JobStatus.QUEUED);

        job = jobRepository.save(job);

        Instant beforeClaim = Instant.now();

        var claimedJob =
                jobClaimService.claimNextJob(workerId);

        Instant afterClaim = Instant.now();

        assertThat(claimedJob)
                .isPresent();

        assertThat(claimedJob.get().getId())
                .isEqualTo(job.getId());

        Job jobFromDatabase =
                jobRepository
                        .findById(job.getId())
                        .orElseThrow();

        assertThat(jobFromDatabase.getStatus())
                .isEqualTo(JobStatus.RUNNING);

        assertThat(jobFromDatabase.getWorkerId())
                .isEqualTo(workerId);

        assertThat(jobFromDatabase.getStartedAt())
                .isNotNull();

        assertThat(jobFromDatabase.getStartedAt())
                .isBetween(beforeClaim, afterClaim);

        assertThat(jobFromDatabase.getLeaseUntil())
                .isNotNull();

        assertThat(jobFromDatabase.getLeaseUntil())
                .isAfter(jobFromDatabase.getStartedAt());
    }

    @Test
    void shouldReturnEmptyWhenNoQueuedJobsExist() {

        UUID workerId = createActiveWorker().getId();

        var claimedJob =
                jobClaimService.claimNextJob(workerId);

        assertThat(claimedJob).isEmpty();
    }

    @Test
    void shouldClaimOldestQueuedJobFirst() {

        UUID workerId = createActiveWorker().getId();
        UUID workflowId = UUID.randomUUID();

        Instant olderTime = Instant.now()
                .minus(10, ChronoUnit.MINUTES);

        Instant newerTime = Instant.now()
                .minus(5, ChronoUnit.MINUTES);

        Job olderJob = new Job(workflowId);
        olderJob.setTaskType("ECHO");
        olderJob.setPayload("Older job");
        olderJob.setStatus(JobStatus.QUEUED);
        olderJob.setCreatedAt(olderTime);

        olderJob = jobRepository.saveAndFlush(olderJob);

        Job newerJob = new Job(workflowId);
        newerJob.setTaskType("ECHO");
        newerJob.setPayload("Newer job");
        newerJob.setStatus(JobStatus.QUEUED);
        newerJob.setCreatedAt(newerTime);

        newerJob = jobRepository.saveAndFlush(newerJob);

        var claimedJob =
                jobClaimService.claimNextJob(workerId);

        assertThat(claimedJob)
                .isPresent();

        assertThat(claimedJob.get().getId())
                .isEqualTo(olderJob.getId());

        Job olderJobFromDatabase =
                jobRepository.findById(olderJob.getId())
                        .orElseThrow();

        Job newerJobFromDatabase =
                jobRepository.findById(newerJob.getId())
                        .orElseThrow();

        assertThat(olderJobFromDatabase.getStatus())
                .isEqualTo(JobStatus.RUNNING);

        assertThat(newerJobFromDatabase.getStatus())
                .isEqualTo(JobStatus.QUEUED);
    }

    @Test
    void shouldNotClaimJobThatIsAlreadyRunning() {

        UUID workerId = createActiveWorker().getId();
        UUID originalWorkerId = UUID.randomUUID();

        Job job = new Job(UUID.randomUUID());

        job.setTaskType("ECHO");
        job.setPayload("Already running");
        job.setStatus(JobStatus.RUNNING);
        job.setWorkerId(originalWorkerId);
        job.setStartedAt(Instant.now());
        job.setLeaseUntil(Instant.now().plusSeconds(60));

        jobRepository.saveAndFlush(job);

        var claimedJob =
                jobClaimService.claimNextJob(workerId);

        assertThat(claimedJob).isEmpty();

        Job jobFromDatabase =
                jobRepository.findById(job.getId())
                        .orElseThrow();

        assertThat(jobFromDatabase.getStatus())
                .isEqualTo(JobStatus.RUNNING);

        assertThat(jobFromDatabase.getWorkerId())
                .isEqualTo(originalWorkerId);
    }

    @Test
    void shouldRejectNullWorkerId() {

        assertThatThrownBy(() ->
                jobClaimService.claimNextJob(null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Worker ID must not be null");
    }

    @Test
    void shouldRejectUnregisteredWorker() {

        UUID unknownWorkerId = UUID.randomUUID();

        assertThatThrownBy(() ->
                jobClaimService.claimNextJob(unknownWorkerId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Worker not found: " + unknownWorkerId);
    }

    @Test
    void shouldRejectInactiveWorker() {

        Worker worker = new Worker(
                "inactive-worker-" + UUID.randomUUID()
        );

        worker.setStatus(WorkerStatus.OFFLINE);

        Worker savedWorker = workerRepository.saveAndFlush(worker);

        assertThatThrownBy(() ->
                jobClaimService.claimNextJob(savedWorker.getId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Worker is not active: " + savedWorker.getId());
    }

    @Test
    void shouldNotChangeQueuedJobWhenWorkerIsUnregistered() {

        UUID unknownWorkerId = UUID.randomUUID();

        Job job = new Job(UUID.randomUUID());
        job.setTaskType("ECHO");
        job.setPayload("Must remain queued");
        job.setStatus(JobStatus.QUEUED);

        job = jobRepository.saveAndFlush(job);

        assertThatThrownBy(() ->
                jobClaimService.claimNextJob(unknownWorkerId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Worker not found: " + unknownWorkerId);

        Job jobFromDatabase = jobRepository
                .findById(job.getId())
                .orElseThrow();

        assertThat(jobFromDatabase.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(jobFromDatabase.getWorkerId())
                .isNull();
    }

    @Test
    void shouldNotChangeQueuedJobWhenWorkerIsInactive() {

        Worker worker = new Worker(
                "offline-worker-" + UUID.randomUUID()
        );
        worker.setStatus(WorkerStatus.OFFLINE);

        Worker savedWorker = workerRepository.saveAndFlush(worker);

        Job job = new Job(UUID.randomUUID());
        job.setTaskType("ECHO");
        job.setPayload("Must remain queued");
        job.setStatus(JobStatus.QUEUED);

        job = jobRepository.saveAndFlush(job);

        assertThatThrownBy(() ->
                jobClaimService.claimNextJob(savedWorker.getId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Worker is not active: " + savedWorker.getId());

        Job jobFromDatabase = jobRepository
                .findById(job.getId())
                .orElseThrow();

        assertThat(jobFromDatabase.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(jobFromDatabase.getWorkerId())
                .isNull();
    }
}