package com.flowforge.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(WorkflowController.class)
@Import(GlobalExceptionHandler.class)
@ContextConfiguration(classes = WorkflowControllerTest.TestConfig.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Configuration
    static class TestConfig {

        @Bean
        WorkflowRepository workflowRepository() {
            return mock(WorkflowRepository.class);
        }

        @Bean
        WorkflowController workflowController(
                WorkflowRepository workflowRepository
        ) {
            return new WorkflowController(workflowRepository);
        }
    }

    @Test
    void shouldCreateWorkflow() throws Exception {

        when(workflowRepository.save(any(Workflow.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        mockMvc.perform(
                post("/api/workflows")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "API Test Workflow",
                                    "description": "Workflow created through API test"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(content()
                .contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.name")
                .value("API Test Workflow"))
        .andExpect(jsonPath("$.description")
                .value("Workflow created through API test"));
    }

    @Test
    void shouldGetWorkflowById() throws Exception {

        UUID workflowId =
                UUID.fromString("394562b9-53fb-4353-bb10-643275dd5078");

        Workflow workflow =
                new Workflow(
                    "API Test Workflow",
                    "Workflow returned through API test"
                );

        when(workflowRepository.findById(workflowId))
                .thenReturn(java.util.Optional.of(workflow));

        mockMvc.perform(
                get("/api/workflows/{id}", workflowId)
        )
        .andExpect(status().isOk())
        .andExpect(content()
                .contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.name")
                .value("API Test Workflow"))
        .andExpect(jsonPath("$.description")
                .value("Workflow returned through API test"));

        verify(workflowRepository).findById(workflowId);
    }

    @Test
    void shouldReturn404WhenWorkflowDoesNotExist() throws Exception {

        UUID workflowId =
                UUID.fromString("00000000-0000-0000-0000-000000000001");

        when(workflowRepository.findById(workflowId))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(
                get("/api/workflows/{id}", workflowId)
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message")
                .value("Workflow not found"));

        verify(workflowRepository).findById(workflowId);
    }
}