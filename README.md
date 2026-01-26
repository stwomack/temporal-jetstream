# Temporal Jetstream - Airline Flight Lifecycle Orchestration Demo

A demonstration application showing how Temporal provides durability, reliability, scalability, and consistency for multi-day airline flight lifecycle orchestration. Complements existing Kafka/Flink streaming architecture by providing durable execution guarantees for long-running flight processes.

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or use SDKMAN
- **Docker & Docker Compose** - For running Kafka and MongoDB
- **Temporal Server** - Running locally via `temporal server start-dev`

### Installing Temporal CLI

```bash
# macOS
brew install temporal

# Linux
curl -sSf https://temporal.download/cli.sh | sh

# Windows (via Scoop)
scoop install temporal
```

## Quick Start

### 1. Start Temporal Server

In a separate terminal, start the Temporal development server:

```bash
temporal server start-dev
```

This will start Temporal on `localhost:7233` with the Web UI available at `http://localhost:8233`.

### 2. Start Supporting Services

Start Kafka and MongoDB using Docker Compose:

```bash
docker-compose up -d
```

Verify services are running:
```bash
docker-compose ps
```

### 3. Build the Application

```bash
./mvnw clean install
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Verifying the Setup

Once all services are running, you should see:

- Temporal Web UI: `http://localhost:8233`
- Application: `http://localhost:8080`
- Kafka: `localhost:9092`
- MongoDB: `localhost:27017`

Check the application logs for:
```
Started Application in X seconds
```

## Technology Stack

- **Java 21** - Modern Java LTS version
- **Spring Boot 4.0.1** - Latest stable Spring Boot framework
- **Temporal Java SDK 1.32.1** - Workflow orchestration
- **Apache Kafka 7.8.0** - Event streaming
- **MongoDB 8.0** - Document persistence
- **WebSockets** - Real-time UI updates

## Project Structure

```
temporal-jetstream/
├── src/
│   ├── main/
│   │   ├── java/com/temporal/jetstream/
│   │   │   └── Application.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/temporal/jetstream/
├── pom.xml
├── docker-compose.yml
└── README.md
```

## Stopping the Services

Stop the application with `Ctrl+C`, then stop supporting services:

```bash
# Stop Docker services
docker-compose down

# Stop Temporal server
# Press Ctrl+C in the Temporal server terminal
```

## Next Steps

This is the initial project setup. Future stories will add:
- Flight workflow implementation
- Signal and query handling
- REST API endpoints
- Real-time web UI
- Kafka integration
- Failure recovery demonstrations

## Troubleshooting

### Application won't start

- Verify Java version: `java -version` (should be 21)
- Check Temporal is running: `temporal server start-dev`
- Ensure ports are available: 8080 (app), 7233 (Temporal), 9092 (Kafka), 27017 (MongoDB)

### Docker services won't start

```bash
# Remove old containers and volumes
docker-compose down -v
docker-compose up -d
```

## License

MIT
