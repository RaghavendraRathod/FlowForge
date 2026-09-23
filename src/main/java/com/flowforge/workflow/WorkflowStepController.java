package com.flowforge.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows/{workflowId}/steps")
public class WorkflowStepController {

    private final WorkflowStepRepository workflowStepRepository;

    public WorkflowStepController(
            WorkflowStepRepository workflowStepRepository) {
        this.workflowStepRepository = workflowStepRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowStep createStep(
            @PathVariable UUID workflowId,
            @RequestBody WorkflowStep step) {

        step.setWorkflowId(workflowId);

        return workflowStepRepository.save(step);
    }

    @GetMapping
    public List<WorkflowStep> getSteps(
            @PathVariable UUID workflowId) {

        return workflowStepRepository
                .findByWorkflowIdOrderByStepOrder(workflowId);
    }
}
