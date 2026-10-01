package com.flowforge.workflow;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowRunController {

    private final WorkflowRunRepository workflowRunRepository;

    public WorkflowRunController(
            WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    @GetMapping("/{workflowId}/runs")
    public List<WorkflowRun> getWorkflowRuns(
            @PathVariable UUID workflowId) {

        return workflowRunRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    @GetMapping("/{workflowId}/runs/{runId}")
    public WorkflowRun getWorkflowRun(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId) {

        return workflowRunRepository
                .findById(runId)
                .filter(run -> run.getWorkflowId().equals(workflowId))
                .orElseThrow(() ->
                        new RuntimeException("Workflow run not found"));
    }
}
