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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        WorkerService.class,
        JobClaimService.class,
        TaskExecutor.class,
        WorkflowProgressService.class,
        JobRecoveryService.class
})
class JobMaxRetryFailureTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkerService workerService;

    @Test
    void shouldFailJobAfterMaximumRetries() {

        Job job = new Job(
                java.util.UUID.randomUUID()
        );

        job.setTaskType("INVALID_TASK");
        job.setPayload("This task should fail");
        job.setMaxRetries(3);
        job.setRetryCount(3);
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(java.time.Instant.now());
        job.setLeaseUntil(
                java.time.Instant.now().plusSeconds(60)
        );

        job = jobRepository.save(job);

        workerService.failJob(job);

        Job failedJob =
                jobRepository
                        .findById(job.getId())
                        .orElseThrow();

        assertThat(failedJob.getStatus())
                .isEqualTo(JobStatus.FAILED);

        assertThat(failedJob.getRetryCount())
                .isEqualTo(3);

        assertThat(failedJob.getCompletedAt())
                .isNotNull();

        assertThat(failedJob.getLeaseUntil())
                .isNull();
    }
}
