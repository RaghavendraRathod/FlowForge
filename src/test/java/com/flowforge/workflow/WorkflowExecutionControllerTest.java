package com.flowforge.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import com.flowforge.job.Job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.reset;

@WebMvcTest(WorkflowExecutionController.class)
@ContextConfiguration(
        classes = WorkflowExecutionControllerTest.TestConfig.class
)
@Import(GlobalExceptionHandler.class)
class WorkflowExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    @BeforeEach
    void resetMocks() {
        reset(workflowExecutionService);
    }

    @Configuration
    static class TestConfig {

        @Bean
        WorkflowExecutionService workflowExecutionService() {
            return mock(WorkflowExecutionService.class);
        }

        @Bean
        WorkflowExecutionController workflowExecutionController(
                WorkflowExecutionService workflowExecutionService
        ) {
            return new WorkflowExecutionController(
                    workflowExecutionService
            );
        }
    }

    @Test
    void shouldExecuteWorkflow() throws Exception {

        UUID workflowId =
                UUID.fromString(
                    "394562b9-53fb-4353-bb10-643275dd5078"
                );

        Job job = new Job(workflowId);

        when(workflowExecutionService.executeWorkflow(workflowId))
                .thenReturn(job);

        mockMvc.perform(
                post("/api/workflows/{workflowId}/execute", workflowId)
                        .contentType(APPLICATION_JSON)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.workflowId")
                .value(workflowId.toString()));
    }

    @Test
    void shouldReturn400WhenExecutingEmptyWorkflow() throws Exception {

        UUID workflowId =
                UUID.fromString(
                    "394562b9-53fb-4353-bb10-643275dd5078"
                );

        when(workflowExecutionService.executeWorkflow(workflowId))
                .thenThrow(
                        new BadRequestException("Workflow has no steps")
                );

        mockMvc.perform(
                post("/api/workflows/{workflowId}/execute", workflowId)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
                .value("Workflow has no steps"));
    }
}
