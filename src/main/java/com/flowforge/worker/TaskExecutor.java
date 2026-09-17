package com.flowforge.worker;

import com.flowforge.job.Job;
import org.springframework.stereotype.Service;

@Service
public class TaskExecutor {

    public String execute(Job job) {

        String taskType = job.getTaskType();

        if (taskType == null || taskType.isBlank()) {
            throw new IllegalArgumentException("Task type is required");
        }

        return switch (taskType.toUpperCase()) {

            case "DEMO" -> executeDemoTask(job);

            default -> throw new IllegalArgumentException(
                    "Unsupported task type: " + taskType
            );
        };
    }

    private String executeDemoTask(Job job) {

        String payload = job.getPayload();

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload is required");
        }

        System.out.println("Executing DEMO task with payload: " + payload);

        return "Executed successfully: " + payload;
    }
}
