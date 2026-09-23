package com.flowforge.workflow;

import com.flowforge.job.Job;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowExecutionController {

    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowExecutionController(
            WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    @PostMapping("/{workflowId}/execute")
    @ResponseStatus(HttpStatus.CREATED)
    public Job executeWorkflow(
            @PathVariable UUID workflowId) {

        return workflowExecutionService.executeWorkflow(workflowId);
    }
}
