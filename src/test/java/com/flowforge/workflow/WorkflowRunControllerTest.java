package com.flowforge.workflow;

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
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowRunController.class)
@ContextConfiguration(classes = WorkflowRunControllerTest.TestConfig.class)
@Import(GlobalExceptionHandler.class)
class WorkflowRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @BeforeEach
    void resetMocks() {
        reset(workflowRunRepository);
    }

    @Configuration
    static class TestConfig {

        @Bean
        WorkflowRunRepository workflowRunRepository() {
            return mock(WorkflowRunRepository.class);
        }

        @Bean
        WorkflowRunController workflowRunController(
                WorkflowRunRepository workflowRunRepository
        ) {
            return new WorkflowRunController(workflowRunRepository);
        }
    }

    @Test
    void shouldGetWorkflowRuns() throws Exception {

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        WorkflowRun run = new WorkflowRun(
                workflowId
        );

        when(workflowRunRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId))
                .thenReturn(List.of(run));

        mockMvc.perform(
                get("/api/workflows/{workflowId}/runs", workflowId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].workflowId")
                .value(workflowId.toString()));

        verify(workflowRunRepository)
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    @Test
    void shouldGetWorkflowRunById() throws Exception {

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        UUID runId = UUID.fromString(
                "1f052cef-1ee2-4815-ba75-32655e02ec52"
        );

        WorkflowRun run = new WorkflowRun(workflowId);

        when(workflowRunRepository.findById(runId))
                .thenReturn(java.util.Optional.of(run));

        mockMvc.perform(
                get(
                        "/api/workflows/{workflowId}/runs/{runId}",
                        workflowId,
                        runId
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workflowId")
                .value(workflowId.toString()));

        verify(workflowRunRepository).findById(runId);
    }

    @Test
    void shouldReturn404WhenWorkflowRunDoesNotExist() throws Exception {

        UUID workflowId = UUID.fromString(
                "394562b9-53fb-4353-bb10-643275dd5078"
        );

        UUID runId = UUID.fromString(
                "00000000-0000-0000-0000-000000000001"
        );

        when(workflowRunRepository.findById(runId))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(
                get(
                        "/api/workflows/{workflowId}/runs/{runId}",
                        workflowId,
                        runId
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
                .value("Workflow run not found"));

        verify(workflowRunRepository).findById(runId);
    }
}
