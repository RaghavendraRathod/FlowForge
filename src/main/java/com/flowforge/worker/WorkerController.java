package com.flowforge.worker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Worker registerWorker(@RequestParam String name) {

        return workerService.registerWorker(name);
    }

    @PostMapping("/{id}/heartbeat")
    public Worker heartbeat(@PathVariable UUID id) {

        return workerService.heartbeat(id);
    }

    @GetMapping
    public List<Worker> getAllWorkers() {

        return workerService.getAllWorkers();
    }

    @GetMapping("/{id}/health")
    public WorkerStatus getWorkerHealth(@PathVariable UUID id) {

        Worker worker = workerService.getWorker(id);

        return workerService.determineHealth(worker);
    }
}
