package com.flowforge.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRunRepository
        extends JpaRepository<WorkflowRun, UUID> {

    List<WorkflowRun> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);
}
