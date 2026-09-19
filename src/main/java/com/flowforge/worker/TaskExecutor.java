package com.flowforge.worker;

import com.flowforge.job.Job;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskExecutor {

    private final List<TaskHandler> taskHandlers;

    public TaskExecutor(List<TaskHandler> taskHandlers) {
        this.taskHandlers = taskHandlers;
    }

    public String execute(Job job) {

        String taskType = job.getTaskType();

        if (taskType == null || taskType.isBlank()) {
            throw new IllegalArgumentException("Task type is required");
        }

        return taskHandlers.stream()
                .filter(handler ->
                        handler.getTaskType().equalsIgnoreCase(taskType)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unsupported task type: " + taskType
                        )
                )
                .execute(job);
    }
}