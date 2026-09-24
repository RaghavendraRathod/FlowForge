package com.flowforge.workflow;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowExecutionService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final JobRepository jobRepository;

    public WorkflowExecutionService(
            WorkflowRepository workflowRepository,
            WorkflowStepRepository workflowStepRepository,
            WorkflowRunRepository workflowRunRepository,
            JobRepository jobRepository) {

        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Job executeWorkflow(UUID workflowId) {

        workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new RuntimeException("Workflow not found"));

        List<WorkflowStep> steps =
                workflowStepRepository
                        .findByWorkflowIdOrderByStepOrder(workflowId);

        if (steps.isEmpty()) {
            throw new RuntimeException(
                    "Workflow has no steps"
            );
        }

        WorkflowRun workflowRun =
                new WorkflowRun(workflowId);

        workflowRun.setStatus(
                WorkflowRunStatus.RUNNING
        );

        workflowRun.setStartedAt(
                java.time.Instant.now()
        );

        workflowRun =
                workflowRunRepository.save(workflowRun);

        WorkflowStep firstStep = steps.get(0);

        Job job = new Job(workflowId);

        job.setWorkflowRunId(workflowRun.getId());
        job.setWorkflowStepId(firstStep.getId());
        job.setTaskType(firstStep.getTaskType());
        job.setPayload(firstStep.getPayload());

        return jobRepository.save(job);
    }
}