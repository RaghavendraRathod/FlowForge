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
class MultipleWorkflowRunsTest {

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
    void shouldKeepMultipleWorkflowRunsIndependent() {

        Workflow workflow = workflowRepository.save(
                new Workflow(
                        "Multiple Runs Test",
                        "Test independent workflow runs"
                )
        );

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

        WorkflowRun run1 = workflowRunRepository.save(
                new WorkflowRun(workflow.getId())
        );

        WorkflowRun run2 = workflowRunRepository.save(
                new WorkflowRun(workflow.getId())
        );

        run1.setStatus(WorkflowRunStatus.RUNNING);
        run1.setStartedAt(java.time.Instant.now());

        run2.setStatus(WorkflowRunStatus.RUNNING);
        run2.setStartedAt(java.time.Instant.now());

        run1 = workflowRunRepository.save(run1);
        run2 = workflowRunRepository.save(run2);

        final java.util.UUID run1Id = run1.getId();
        final java.util.UUID run2Id = run2.getId();

        Job run1Step1 = new Job(workflow.getId());

        run1Step1.setWorkflowRunId(run1.getId());
        run1Step1.setWorkflowStepId(step1.getId());
        run1Step1.setTaskType(step1.getTaskType());
        run1Step1.setPayload(step1.getPayload());
        run1Step1.setStatus(JobStatus.SUCCEEDED);

        run1Step1 = jobRepository.save(run1Step1);

        Job run2Step1 = new Job(workflow.getId());

        run2Step1.setWorkflowRunId(run2.getId());
        run2Step1.setWorkflowStepId(step1.getId());
        run2Step1.setTaskType(step1.getTaskType());
        run2Step1.setPayload(step1.getPayload());
        run2Step1.setStatus(JobStatus.SUCCEEDED);

        run2Step1 = jobRepository.save(run2Step1);

        workflowProgressService.handleJobSuccess(run1Step1);

        List<Job> run1Jobs =
                jobRepository.findAll().stream()
                        .filter(job ->
                                run1Id.equals(job.getWorkflowRunId()))
                        .toList();

        List<Job> run2Jobs =
                jobRepository.findAll().stream()
                        .filter(job ->
                                run2Id.equals(job.getWorkflowRunId()))
                        .toList();

        assertThat(run1Jobs).hasSize(2);
        assertThat(run2Jobs).hasSize(1);

        Job run1Step2 = run1Jobs.stream()
                .filter(job ->
                        step2.getId().equals(job.getWorkflowStepId()))
                .findFirst()
                .orElseThrow();

        assertThat(run1Step2.getStatus())
                .isEqualTo(JobStatus.QUEUED);

        assertThat(run2Jobs.stream()
                .filter(job ->
                        step2.getId().equals(job.getWorkflowStepId()))
                .count()
        ).isZero();
    }
}
