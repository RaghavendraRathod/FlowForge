package com.flowforge.worker;

import com.flowforge.workflow.Workflow;
import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import com.flowforge.workflow.WorkflowProgressService;

import com.flowforge.workflow.WorkflowRun;
import com.flowforge.workflow.WorkflowRunRepository;
import com.flowforge.workflow.WorkflowRunStatus;
import com.flowforge.workflow.WorkflowRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JobRecoveryService.class,
        WorkflowProgressService.class
})
class JobRecoveryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRecoveryService jobRecoveryService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @Test
    void shouldRecoverExpiredRunningJob() {

        // Remove existing jobs inside this test's transaction
        // so old expired jobs cannot interfere with recovery.
        jobRepository.deleteAll();
        jobRepository.flush();

        Job job = new Job(UUID.randomUUID());

        job.setTaskType("ECHO");
        job.setPayload("Expired job");
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(
                Instant.now().minusSeconds(120)
        );
        job.setLeaseUntil(
                Instant.now().minusSeconds(60)
        );
        job.setWorkerId(UUID.randomUUID());

        job = jobRepository.save(job);

        Optional<Job> recoveredJob =
                jobRecoveryService.recoverExpiredJob();

        assertThat(recoveredJob)
                .isPresent();

        Job jobFromDatabase =
                jobRepository
                        .findById(job.getId())
                        .orElseThrow();

        assertThat(jobFromDatabase.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(jobFromDatabase.getStartedAt())
                .isNull();

        assertThat(jobFromDatabase.getLeaseUntil())
                .isNull();

        assertThat(jobFromDatabase.getWorkerId())
                .isNull();
    }

    @Test
    void shouldRequeueExpiredJobWhenRetriesRemain() {

        jobRepository.deleteAll();
        jobRepository.flush();

        Job job = new Job(UUID.randomUUID());

        job.setTaskType("ECHO");
        job.setPayload("Retry this job");
        job.setStatus(JobStatus.RUNNING);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setStartedAt(Instant.now().minusSeconds(120));
        job.setLeaseUntil(Instant.now().minusSeconds(60));
        job.setWorkerId(UUID.randomUUID());

        job = jobRepository.save(job);

        UUID jobId = job.getId();

        Optional<Job> recoveredJob =
                jobRecoveryService.recoverExpiredJob();

        assertThat(recoveredJob).isPresent();
        assertThat(recoveredJob.get().getId()).isEqualTo(jobId);

        Job recoveredJobFromDatabase =
                jobRepository.findById(jobId).orElseThrow();

        assertThat(recoveredJobFromDatabase.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(recoveredJobFromDatabase.getRetryCount())
                .isEqualTo(1);

        assertThat(recoveredJobFromDatabase.getStartedAt()).isNull();
        assertThat(recoveredJobFromDatabase.getLeaseUntil()).isNull();
        assertThat(recoveredJobFromDatabase.getWorkerId()).isNull();
        assertThat(recoveredJobFromDatabase.getCompletedAt()).isNull();
   }

   @Test
   void shouldFailExpiredJobWhenRetryLimitIsReached() {

       jobRepository.deleteAll();
       jobRepository.flush();

       Job job = new Job(UUID.randomUUID());

       job.setTaskType("ECHO");
       job.setPayload("Job has exhausted retries");
       job.setStatus(JobStatus.RUNNING);
       job.setRetryCount(3);
       job.setMaxRetries(3);
       job.setStartedAt(Instant.now().minusSeconds(120));
       job.setLeaseUntil(Instant.now().minusSeconds(60));
       job.setWorkerId(UUID.randomUUID());

       job = jobRepository.save(job);

       UUID jobId = job.getId();

       Optional<Job> recoveredJob =
               jobRecoveryService.recoverExpiredJob();

       assertThat(recoveredJob).isPresent();
       assertThat(recoveredJob.get().getId()).isEqualTo(jobId);

       Job failedJob =
               jobRepository.findById(jobId).orElseThrow();

        assertThat(failedJob.getStatus())
                .isEqualTo(JobStatus.FAILED);

        assertThat(failedJob.getRetryCount())
                .isEqualTo(4);

        assertThat(failedJob.getCompletedAt()).isNotNull();
        assertThat(failedJob.getLeaseUntil()).isNull();
        assertThat(failedJob.getWorkerId()).isNull();

        assertThat(failedJob.getErrorMessage())
                .isEqualTo("Job lease expired after maximum retries");
    }

    @Test
    void shouldFailWorkflowRunWhenExpiredJobExhaustsRetries() {

        jobRepository.deleteAll();
        jobRepository.flush();

        Workflow workflow = workflowRepository.save(
                new Workflow(
                        "Recovery Failure Test",
                        "Verify workflow failure propagation"
                )
        );

        WorkflowRun workflowRun =
                 workflowRunRepository.save(
                        new WorkflowRun(workflow.getId())
                );

        workflowRun.setStatus(WorkflowRunStatus.RUNNING);
        workflowRun.setStartedAt(Instant.now());

        workflowRun = workflowRunRepository.save(workflowRun);

        UUID workflowRunId = workflowRun.getId();

        Job job = new Job(workflow.getId());

        job.setWorkflowRunId(workflowRunId);
        job.setTaskType("ECHO");
        job.setPayload("Expired workflow job");
        job.setStatus(JobStatus.RUNNING);
        job.setRetryCount(3);
        job.setMaxRetries(3);
        job.setStartedAt(Instant.now().minusSeconds(120));
        job.setLeaseUntil(Instant.now().minusSeconds(60));
        job.setWorkerId(UUID.randomUUID());

        job = jobRepository.save(job);

        UUID jobId = job.getId();

        Optional<Job> recoveredJob =
                jobRecoveryService.recoverExpiredJob();

        assertThat(recoveredJob).isPresent();
        assertThat(recoveredJob.get().getId()).isEqualTo(jobId);
        assertThat(recoveredJob.get().getStatus())
                .isEqualTo(JobStatus.FAILED);

        WorkflowRun failedRun =
                workflowRunRepository.findById(workflowRunId)
                        .orElseThrow();

        assertThat(failedRun.getStatus())
                .isEqualTo(WorkflowRunStatus.FAILED);

        assertThat(failedRun.getCompletedAt())
                .isNotNull();
   }
}
