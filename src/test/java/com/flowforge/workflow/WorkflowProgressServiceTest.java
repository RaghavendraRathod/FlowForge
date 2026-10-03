package com.flowforge.workflow;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WorkflowProgressService.class)
class WorkflowProgressServiceTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkflowProgressService workflowProgressService;

    @Test
    void shouldCreateNextJobWhenCurrentStepSucceeds() {

        Workflow workflow = new Workflow(
                "Progression Test Workflow",
                "Test sequential workflow progression"
        );

        workflow = workflowRepository.save(workflow);

        WorkflowStep step1 = workflowStepRepository.save(
                new WorkflowStep(
                        workflow.getId(),
                        1,
                        "ECHO",
                        "Step 1"
                )
        );

        WorkflowStep step2 = workflowStepRepository.save(
                new WorkflowStep(
                        workflow.getId(),
                        2,
                        "ECHO",
                        "Step 2"
                )
        );

        WorkflowStep step3 = workflowStepRepository.save(
                new WorkflowStep(
                        workflow.getId(),
                        3,
                        "ECHO",
                        "Step 3"
                )
        );

        WorkflowRun workflowRun =
                workflowRunRepository.save(
                        new WorkflowRun(workflow.getId())
                );

        workflowRun.setStatus(WorkflowRunStatus.RUNNING);
        workflowRun.setStartedAt(java.time.Instant.now());

        workflowRun =
                workflowRunRepository.save(workflowRun);

        final java.util.UUID testRunId = workflowRun.getId();

        Job firstJob = new Job(workflow.getId());

        firstJob.setWorkflowRunId(workflowRun.getId());
        firstJob.setWorkflowStepId(step1.getId());
        firstJob.setTaskType(step1.getTaskType());
        firstJob.setPayload(step1.getPayload());
        firstJob.setStatus(JobStatus.SUCCEEDED);

        firstJob = jobRepository.save(firstJob);

        workflowProgressService.handleJobSuccess(firstJob);

        List<Job> jobs =
                jobRepository.findAll().stream()
                        .filter(job ->
                                testRunId.equals(job.getWorkflowRunId()))
                        .toList();

        assertThat(jobs).hasSize(2);

        Job secondJob = jobs.stream()
                .filter(job ->
                        step2.getId().equals(job.getWorkflowStepId()))
                .findFirst()
                .orElseThrow();

        assertThat(secondJob.getWorkflowId())
                .isEqualTo(workflow.getId());

        assertThat(secondJob.getWorkflowRunId())
                .isEqualTo(workflowRun.getId());

        assertThat(secondJob.getTaskType())
                .isEqualTo("ECHO");

        assertThat(secondJob.getPayload())
                .isEqualTo("Step 2");

        assertThat(secondJob.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(
                jobs.stream()
                        .filter(job ->
                                step3.getId()
                                        .equals(job.getWorkflowStepId()))
                        .count()
        ).isZero();
    }
}
