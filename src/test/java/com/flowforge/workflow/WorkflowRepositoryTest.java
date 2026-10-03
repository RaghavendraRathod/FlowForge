package com.flowforge.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.test.database.replace=NONE"
})
class WorkflowRepositoryTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Test
    void shouldSaveAndFindWorkflow() {

        Workflow workflow = new Workflow(
                "Test Workflow",
                "Workflow created by automated test"
        );

        Workflow savedWorkflow =
                workflowRepository.save(workflow);

        assertThat(savedWorkflow.getId()).isNotNull();

        java.util.Optional<Workflow> foundWorkflow =
                workflowRepository.findById(savedWorkflow.getId());

        assertThat(foundWorkflow).isPresent();

        assertThat(foundWorkflow.get().getName())
                .isEqualTo("Test Workflow");

        assertThat(foundWorkflow.get().getDescription())
                .isEqualTo("Workflow created by automated test");
    }
}