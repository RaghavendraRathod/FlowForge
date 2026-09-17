package com.flowforge.job;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID workflowId;

    private String taskType;

    private String payload;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    private int retryCount;

    public Job() {
    }

    public Job(UUID workflowId) {
        this.workflowId = workflowId;
        this.status = JobStatus.QUEUED;
        this.createdAt = Instant.now();
        this.retryCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getTaskType() {
       return taskType;
   }

    public void setTaskType(String taskType) {
       this.taskType = taskType;
    }

    public String getPayload() {
       return payload;
 }

    public void setPayload(String payload) {
       this.payload = payload;
   }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
