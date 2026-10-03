package com.flowforge.workflow;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import com.flowforge.job.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WorkflowProgressService.class)
class WorkflowCompletionTest {

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
    void shouldMarkWorkflowRunSucceededWhenFinalStepSucceeds() {

        Workflow workflow = new Workflow(
                "Completion Test Workflow",
                "Test workflow completion"
        );

        workflow = workflowRepository.save(workflow);

        WorkflowStep finalStep =
                workflowStepRepository.save(
                        new WorkflowStep(
                                workflow.getId(),
                                1,
                                "ECHO",
                                "Final step"
                        )
                );

        WorkflowRun workflowRun =
                workflowRunRepository.save(
                        new WorkflowRun(workflow.getId())
                );

        workflowRun.setStatus(
                WorkflowRunStatus.RUNNING
        );

        workflowRun.setStartedAt(
                java.time.Instant.now()
        );

        workflowRun =
                workflowRunRepository.save(workflowRun);

        final java.util.UUID testRunId = workflowRun.getId();

        Job finalJob = new Job(workflow.getId());

        finalJob.setWorkflowRunId(workflowRun.getId());
        finalJob.setWorkflowStepId(finalStep.getId());
        finalJob.setTaskType(finalStep.getTaskType());
        finalJob.setPayload(finalStep.getPayload());
        finalJob.setStatus(JobStatus.SUCCEEDED);

        finalJob = jobRepository.save(finalJob);

        workflowProgressService.handleJobSuccess(finalJob);

        WorkflowRun completedRun =
                workflowRunRepository
                        .findById(workflowRun.getId())
                        .orElseThrow();

        assertThat(completedRun.getStatus())
                .isEqualTo(WorkflowRunStatus.SUCCEEDED);

        assertThat(completedRun.getCompletedAt())
                .isNotNull();

        assertThat(
                jobRepository.findAll().stream()
                        .filter(job ->
                                testRunId.equals(job.getWorkflowRunId()))
                        .count()
        ).isEqualTo(1);
    }
}
