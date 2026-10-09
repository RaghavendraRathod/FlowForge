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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JobClaimService.class)
class JobClaimServiceTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobClaimService jobClaimService;

    @Test
    void shouldClaimQueuedJobForWorker() {

        UUID workerId = UUID.randomUUID();

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

        UUID workerId = UUID.randomUUID();

        var claimedJob =
                jobClaimService.claimNextJob(workerId);

        assertThat(claimedJob).isEmpty();
    }

    @Test
    void shouldClaimOldestQueuedJobFirst() {

        UUID workerId = UUID.randomUUID();
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

        UUID workerId = UUID.randomUUID();
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
}