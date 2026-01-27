package com.temporal.jetstream.service;

import com.temporal.jetstream.workflow.FlightWorkflowImpl;
import com.temporal.jetstream.workflow.MultiLegFlightWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkerManagementService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerManagementService.class);

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private WorkerFactory workerFactory;

    @Value("${temporal.task-queue}")
    private String taskQueue;

    /**
     * Simulates a worker failure by stopping and restarting the worker.
     * This demonstrates Temporal's durability - workflows will resume from their last checkpoint.
     */
    public synchronized void restartWorker() throws InterruptedException {
        logger.info("Stopping worker to simulate failure...");

        // Shutdown the worker factory (simulates process death)
        workerFactory.shutdown();
        logger.info("Worker stopped");

        // Wait a moment to simulate downtime
        Thread.sleep(1000);

        // Create a new worker factory and worker
        logger.info("Restarting worker...");
        WorkerFactory newWorkerFactory = WorkerFactory.newInstance(workflowClient);
        Worker newWorker = newWorkerFactory.newWorker(taskQueue);
        newWorker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class, MultiLegFlightWorkflowImpl.class);

        // Start the new worker
        newWorkerFactory.start();
        logger.info("Worker restarted. Workflows will resume from last checkpoint.");

        // Update the reference (note: in a production system, this would need proper lifecycle management)
        // For this demo, we're accepting that the old factory reference in TemporalConfig won't be updated
        // The workflows will resume on the new worker
    }
}
