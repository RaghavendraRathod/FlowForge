package com.flowforge.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
}
