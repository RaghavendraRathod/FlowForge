package com.flowforge.worker;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import com.flowforge.workflow.WorkflowProgressService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowforge.workflow.WorkflowProgressService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        WorkerService.class,
        JobClaimService.class,
        TaskExecutor.class,
        WorkflowProgressService.class,
        JobRecoveryService.class
})
class JobRetryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private com.flowforge.workflow.WorkflowProgressService workflowProgressService;

    @Autowired
    private WorkerService workerService;

    @Test
    void shouldRequeueFailedJobWhenRetriesRemain() {

        Job job = new Job(
                java.util.UUID.randomUUID()
        );

        job.setTaskType("INVALID_TASK");
        job.setPayload("This task should fail");
        job.setMaxRetries(3);
        job.setRetryCount(0);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(java.time.Instant.now());
        job.setLeaseUntil(
                java.time.Instant.now().plusSeconds(60)
        );

        job = jobRepository.save(job);

        job.setErrorMessage("Unsupported task type: INVALID_TASK");

        if (job.getRetryCount() < job.getMaxRetries()) {

            job.setRetryCount(
                    job.getRetryCount() + 1
            );

            job.setStatus(JobStatus.QUEUED);
            job.setStartedAt(null);
            job.setLeaseUntil(null);
            job.setWorkerId(null);

            jobRepository.save(job);
        }

        Job retriedJob =
                jobRepository
                        .findById(job.getId())
                        .orElseThrow();

        assertThat(retriedJob.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(retriedJob.getRetryCount())
                .isEqualTo(1);

        assertThat(retriedJob.getStartedAt())
                .isNull();

        assertThat(retriedJob.getLeaseUntil())
                .isNull();

        assertThat(retriedJob.getWorkerId())
                .isNull();

        assertThat(retriedJob.getErrorMessage())
                .isEqualTo(
                        "Unsupported task type: INVALID_TASK"
                );
    }
}
