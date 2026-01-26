package com.temporal.jetstream.controller;

import com.temporal.jetstream.dto.ErrorResponse;
import com.temporal.jetstream.service.WorkerManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private WorkerManagementService workerManagementService;

    @PostMapping("/restart-worker")
    public ResponseEntity<?> restartWorker() {
        try {
            logger.info("Received request to restart worker - simulating failure scenario");

            workerManagementService.restartWorker();

            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Worker restarted successfully. Workflows will resume from last checkpoint.");

            logger.info("Worker restart completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error restarting worker: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("RESTART_ERROR", e.getMessage()));
        }
    }
}
