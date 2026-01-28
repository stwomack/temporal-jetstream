package com.temporal.jetstream.controller;

import com.temporal.jetstream.dto.ErrorResponse;
import com.temporal.jetstream.service.WorkerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "Administrative operations for worker management")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private WorkerManagementService workerManagementService;

    @Operation(summary = "Restart the Temporal worker", description = "Restarts the Temporal worker to simulate a failure/recovery scenario. Running workflows will resume from their last checkpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worker restarted successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to restart worker")
    })
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
