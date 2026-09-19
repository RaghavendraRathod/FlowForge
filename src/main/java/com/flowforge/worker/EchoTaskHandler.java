package com.flowforge.worker;

import com.flowforge.job.Job;
import org.springframework.stereotype.Component;

@Component
public class EchoTaskHandler implements TaskHandler {

    @Override
    public String getTaskType() {
        return "ECHO";
    }

    @Override
    public String execute(Job job) {

        String payload = job.getPayload();

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload is required");
        }

        System.out.println(
                "Executing ECHO task with payload: " + payload
        );

        return payload;
    }
}