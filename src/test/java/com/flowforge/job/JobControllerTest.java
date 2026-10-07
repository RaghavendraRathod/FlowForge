package com.flowforge.job;

import static org.mockito.ArgumentMatchers.argThat;
import com.flowforge.workflow.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@ContextConfiguration(classes = JobControllerTest.TestConfig.class)
@Import(GlobalExceptionHandler.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void resetMocks() {
        reset(jobRepository);
    }

    @Configuration
    static class TestConfig {

        @Bean
        JobRepository jobRepository() {
            return mock(JobRepository.class);
        }

        @Bean
        JobController jobController(
                JobRepository jobRepository
        ) {
            return new JobController(jobRepository);
        }
    }

    @Test
    void shouldGetJobById() throws Exception {

        UUID jobId = UUID.fromString(
                "0ea8d663-6fd5-4cd1-89f6-260ac7009bb3"
        );

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        Job job = new Job(workflowId);
        job.setTaskType("ECHO");
        job.setPayload("Step 1 - Start workflow");

        when(jobRepository.findById(jobId))
                .thenReturn(Optional.of(job));

        mockMvc.perform(
                get("/api/jobs/{id}", jobId)
        )
        .andExpect(status().isOk())
        .andExpect(content()
                .contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.workflowId")
                .value(workflowId.toString()))
        .andExpect(jsonPath("$.taskType")
                .value("ECHO"))
        .andExpect(jsonPath("$.payload")
                .value("Step 1 - Start workflow"));

        verify(jobRepository).findById(jobId);
    }

    @Test
    void shouldReturn404WhenJobDoesNotExist() throws Exception {

        UUID jobId = UUID.fromString(
                "00000000-0000-0000-0000-000000000001"
        );

        when(jobRepository.findById(jobId))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/api/jobs/{id}", jobId)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
                .value("Job not found"));

        verify(jobRepository).findById(jobId);
    }

    @Test
    void shouldGetAllJobs() throws Exception {

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        Job job1 = new Job(workflowId);
        job1.setTaskType("ECHO");
        job1.setPayload("First job");

        Job job2 = new Job(workflowId);
        job2.setTaskType("ECHO");
        job2.setPayload("Second job");

        when(jobRepository.findAll())
                .thenReturn(List.of(job1, job2));

        mockMvc.perform(
                get("/api/jobs")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].taskType")
                .value("ECHO"))
        .andExpect(jsonPath("$[1].payload")
                .value("Second job"));

        verify(jobRepository).findAll();
    }

    @Test
    void shouldCreateJob() throws Exception {

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                 "workflowId": "394562b9-53fb-4353-bb10-643275dd5078",
                                 "taskType": "ECHO",
                                 "payload": "API test job"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(content()
                .contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.workflowId")
                .value(workflowId.toString()))
        .andExpect(jsonPath("$.taskType")
                .value("ECHO"))
        .andExpect(jsonPath("$.payload")
                .value("API test job"))
        .andExpect(jsonPath("$.status")
                .value("QUEUED"));

        verify(jobRepository).save(argThat(savedJob ->
                savedJob.getWorkflowId().equals(workflowId)
                        && savedJob.getTaskType().equals("ECHO")
                        && savedJob.getPayload().equals("API test job")
                        && savedJob.getStatus() == JobStatus.QUEUED
                        && savedJob.getRetryCount() == 0
                        && savedJob.getMaxRetries() == 3
        ));
    }

    @Test
    void shouldRejectJobWithoutWorkflowId() throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "taskType": "ECHO",
                                  "payload": "API test job"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());

        verify(jobRepository, org.mockito.Mockito.never())
                .save(any(Job.class));
    }

    @Test
    void shouldRejectJobWithoutTaskType() throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                       .contentType(APPLICATION_JSON)
                       .content("""
                               {
                                 "workflowId": "394562b9-53fb-4353-bb10-643275dd5078",
                                 "payload": "API test job"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());

        verify(jobRepository, org.mockito.Mockito.never())
            .save(any(Job.class));
    }

    @Test
    void shouldRejectJobWithoutPayload() throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "workflowId": "394562b9-53fb-4353-bb10-643275dd5078",
                                  "taskType": "ECHO"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());

        verify(jobRepository, org.mockito.Mockito.never())
            .save(any(Job.class));
   }
}
