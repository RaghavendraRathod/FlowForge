package com.flowforge.worker;

import com.flowforge.job.Job;

public interface TaskHandler {

    String getTaskType();

    String execute(Job job);
}
