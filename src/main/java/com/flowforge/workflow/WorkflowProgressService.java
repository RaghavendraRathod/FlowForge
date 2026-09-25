package com.flowforge.workflow;

import com.flowforge.job.Job;
import com.flowforge.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowProgressService {

    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final JobRepository jobRepository;

    public WorkflowProgressService(
            WorkflowStepRepository workflowStepRepository,
            WorkflowRunRepository workflowRunRepository,
            JobRepository jobRepository) {

        this.workflowStepRepository = workflowStepRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.jobRepository = jobRepository;
        }

    @Transactional
    public void handleJobSuccess(Job job) {

        if (job.getWorkflowStepId() == null) {
            return;
        }

        List<WorkflowStep> steps =
                workflowStepRepository
                        .findByWorkflowIdOrderByStepOrder(
                                job.getWorkflowId()
                        );

        WorkflowStep currentStep =
                workflowStepRepository
                        .findById(job.getWorkflowStepId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Workflow step not found"
                                ));

        WorkflowStep nextStep = steps.stream()
                .filter(step ->
                        step.getStepOrder()
                                > currentStep.getStepOrder()
                )
                .findFirst()
                .orElse(null);

        if (nextStep == null) {

            WorkflowRun workflowRun =
                    workflowRunRepository
                            .findById(job.getWorkflowRunId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                     "Workflow run not found"
                                    ));

           workflowRun.markSucceeded();

           workflowRunRepository.save(workflowRun);

           System.out.println(
                   "Workflow completed: "
                           + job.getWorkflowId()
                           + " | run: "
                           + job.getWorkflowRunId()
                );

                return;
        }

        if (jobRepository.existsByWorkflowRunIdAndWorkflowStepId(
                job.getWorkflowRunId(),
                nextStep.getId())) {

            System.out.println(
                    "Job already exists for workflow run "
                            + job.getWorkflowRunId()
                            + " and workflow step "
                            + nextStep.getId()
                );

                return;
        }

        Job nextJob = new Job(job.getWorkflowId());

        nextJob.setWorkflowRunId(job.getWorkflowRunId());
        nextJob.setWorkflowStepId(nextStep.getId());
        nextJob.setTaskType(nextStep.getTaskType());
        nextJob.setPayload(nextStep.getPayload());

        jobRepository.save(nextJob);

        System.out.println(
                "Created next workflow job: "
                        + nextJob.getId()
                        + " for step "
                        + nextStep.getStepOrder()
        );
    }
}