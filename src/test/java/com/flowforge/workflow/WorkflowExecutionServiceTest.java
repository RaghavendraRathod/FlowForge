package com.flowforge.workflow;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WorkflowExecutionService.class)
class WorkflowExecutionServiceTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    @Test
    void shouldCreateWorkflowRunAndFirstJob() {

        Workflow workflow = new Workflow(
                "Execution Test Workflow",
                "Test workflow execution"
        );

        workflow = workflowRepository.save(workflow);

        WorkflowStep firstStep = new WorkflowStep(
                workflow.getId(),
                1,
                "ECHO",
                "First test step"
        );

        WorkflowStep secondStep = new WorkflowStep(
                workflow.getId(),
                2,
                "ECHO",
                "Second test step"
        );

        workflowStepRepository.save(firstStep);
        workflowStepRepository.save(secondStep);

        Job job =
                workflowExecutionService.executeWorkflow(
                        workflow.getId()
                );

        assertThat(job.getId()).isNotNull();

        assertThat(job.getWorkflowId())
                .isEqualTo(workflow.getId());

        assertThat(job.getWorkflowStepId())
                .isEqualTo(firstStep.getId());

        assertThat(job.getTaskType())
                .isEqualTo("ECHO");

        assertThat(job.getPayload())
                .isEqualTo("First test step");

        assertThat(job.getWorkflowRunId())
                .isNotNull();

        WorkflowRun workflowRun =
                workflowRunRepository
                        .findById(job.getWorkflowRunId())
                        .orElseThrow();

        assertThat(workflowRun.getWorkflowId())
                .isEqualTo(workflow.getId());

        assertThat(workflowRun.getStatus())
                .isEqualTo(WorkflowRunStatus.RUNNING);

        assertThat(workflowRun.getStartedAt())
                .isNotNull();
    }
}
