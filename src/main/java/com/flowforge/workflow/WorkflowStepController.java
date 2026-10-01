package com.flowforge.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows/{workflowId}/steps")
public class WorkflowStepController {

    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowStepController(
            WorkflowStepRepository workflowStepRepository,
            WorkflowRepository workflowRepository) {

        this.workflowStepRepository = workflowStepRepository;
        this.workflowRepository = workflowRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowStep createStep(
            @PathVariable UUID workflowId,
            @RequestBody WorkflowStep step) {

        workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        if (step.getStepOrder() < 1) {
            throw new BadRequestException(
            "Step order must be greater than or equal to 1"
            );
        }
        
        step.setWorkflowId(workflowId);

        return workflowStepRepository.save(step);
    }

    @GetMapping
    public List<WorkflowStep> getSteps(
            @PathVariable UUID workflowId) {

        workflowRepository.findById(workflowId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Workflow not found"));

        return workflowStepRepository
                .findByWorkflowIdOrderByStepOrder(workflowId);
    }
}
