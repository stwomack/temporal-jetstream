package com.temporal.jetstream.config;

import com.temporal.jetstream.activity.FlightEventActivityImpl;
import com.temporal.jetstream.activity.PersistenceActivityImpl;
import com.temporal.jetstream.workflow.FlightWorkflow;
import com.temporal.jetstream.workflow.FlightWorkflowImpl;
import com.temporal.jetstream.workflow.MultiLegFlightWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    private static final Logger logger = LoggerFactory.getLogger(TemporalConfig.class);

    @Value("${temporal.task-queue}")
    private String taskQueue;

    @Autowired
    private FlightEventActivityImpl flightEventActivity;

    @Autowired
    private PersistenceActivityImpl persistenceActivity;

    private WorkerFactory workerFactory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        this.workerFactory = WorkerFactory.newInstance(workflowClient);
        return this.workerFactory;
    }

    @Bean
    public Worker worker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker(taskQueue);
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class, MultiLegFlightWorkflowImpl.class);
        logger.info("Registered FlightWorkflowImpl and MultiLegFlightWorkflowImpl for task queue: {}", taskQueue);

        // Register activities
        worker.registerActivitiesImplementations(flightEventActivity, persistenceActivity);
        logger.info("Registered FlightEventActivity and PersistenceActivity for task queue: {}", taskQueue);

        // Start the worker factory immediately after registration
        workerFactory.start();
        logger.info("Worker started for task queue: {}", taskQueue);

        return worker;
    }

    @PreDestroy
    public void stopWorker() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            logger.info("Worker stopped");
        }
    }
}
