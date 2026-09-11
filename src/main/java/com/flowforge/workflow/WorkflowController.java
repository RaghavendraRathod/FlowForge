package com.flowforge.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;

    public WorkflowController(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Workflow createWorkflow(@RequestBody Workflow workflow) {
        return workflowRepository.save(workflow);
    }

    @GetMapping
    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    @GetMapping("/{id}")
    public Workflow getWorkflow(@PathVariable UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
    }
}
