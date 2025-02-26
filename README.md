# Brokerage Service

A scalable backend service for a brokerage firm that allows employees to manage stock orders for their customers.

## Architecture

This application follows a modern microservice-ready architecture with the following key design patterns:

- **CQRS Pattern**: Command Query Responsibility Segregation separates read and write operations for performance and scalability
- **Event-Driven Architecture**: Uses Kafka for asynchronous processing of order events
- **Resilience Patterns**: Implements circuit breaker and retry mechanisms for enhanced system reliability
- **Outbox Pattern**: Ensures reliable event publishing through an event outbox
- **RESTful API**: Clean API endpoints with proper authorization and validation

## Resilience Patterns

The application implements two key resilience patterns to enhance system reliability:

### Circuit Breaker
- Implemented using Resilience4j's CircuitBreaker
- Monitors the health of event publishing to Kafka
- Prevents system overload during temporary failures
- Automatically transitions between open, half-open, and closed states
- Configurable failure rate threshold and recovery mechanisms

### Retry Mechanism
- Automatic retry for transient failures
- Exponential backoff strategy
- Configurable maximum retry attempts
- Handles specific exception types like Kafka exceptions and network timeouts

### Outbox Pattern
- Ensures reliable event publishing by storing **only failed events**.
- Events are first attempted to be published to Kafka with **circuit breaker and retry mechanisms**.
- If all retries fail or the circuit breaker is open, the event is **saved to the outbox** instead of being lost.
- A separate process can then retry publishing these outbox events later, ensuring **eventual consistency**.
- Prevents data loss in case of temporary Kafka failures or network issues.

## Technologies

- **Liquibase**: Database migration and version control

- **Spring Boot 3.x**: Core framework for the application
- **Spring Security + JWT**: Authentication and authorization
- **Spring Data JPA**: Data access layer
- **H2 Database**: In-memory database (configurable for production databases)
- **Kafka**: Messaging system for event processing
- **Resilience4j**: Circuit breaker and retry patterns
- **Gradle**: Build tool
- **Lombok & MapStruct**: Reduce boilerplate code
- **Swagger/OpenAPI**: API documentation

## Features

- Create, list, and cancel stock orders
- Manage customer assets
- Built-in security with role-based access control
- Asset inventory management with proper locking
- Order lifecycle management (PENDING, MATCHED, CANCELED)
- Admin functionality for order matching
- Resilient event publishing with circuit breaker and retry
- Event outbox for guaranteed message delivery

## Project Structure

The application follows a clean architecture with clear separation of concerns:

- `api`: Controllers and DTOs
- `config`: Configuration classes
- `domain`: Domain models (JPA entities)
- `repository`: Data access layer
- `service`: Business logic with command/query separation
- `event`: Event definitions and publishers
- `security`: JWT authentication and security utilities
- `exception`: Exception handling

## Getting Started

### Prerequisites

- Java 17+
- Gradle 8.x
- Kafka (optional for local development)


### Database Migrations with Liquibase

The application uses **Liquibase** to manage database migrations. When the application starts, Liquibase automatically applies necessary schema changes.

To manually run migrations, use:

```bash
./gradlew update
```

To check pending changes before applying:

```bash
./gradlew diffChangeLog
```

### Building the Application

```bash
./gradlew build
```

### Running the Application

```bash
./gradlew bootRun
```

By default, the application runs with the `dev` profile which initializes:
- An admin user (username: `admin`, password: `admin`)
- A regular user (username: `user`, password: `password`)
- In-memory H2 database
- H2 console available at `/h2-console`

### API Documentation

When the application is running, access the Swagger UI documentation at:

```
http://localhost:8080/api/swagger-ui.html
```

## API Endpoints

### Authentication

- `POST /api/auth/login`: User login
- `POST /api/auth/register`: User registration

### Orders

- `POST /api/orders`: Create an order
- `GET /api/orders`: List orders with optional filters
- `GET /api/orders/{orderId}`: Get order details
- `DELETE /api/orders/{orderId}`: Cancel a pending order

### Assets

- `GET /api/assets`: List assets with optional filters
- `GET /api/assets/{assetName}`: Get specific asset details

### Admin Operations

- `GET /api/admin/customers`: List all customers
- `POST /api/admin/customers`: Create a customer
- `POST /api/admin/customers/admin`: Create an admin user
- `GET /api/admin/orders/pending`: List all pending orders
- `POST /api/admin/orders/match`: Match a pending order

## Security

The application uses JWT tokens for authentication. All API endpoints except `/api/auth/*` require authentication.

Admin-specific endpoints under `/api/admin/*` require ADMIN role.

## Production Deployment

For production deployment, configure the following:

1. Use a production database (PostgreSQL, MySQL, etc.) by updating application-prod.yml
2. Configure a proper secret key for JWT token generation
3. Set up Kafka cluster configuration in application-prod.yml
4. Run with the prod profile: `./gradlew bootRun --args='--spring.profiles.active=prod'`

## Testing

Run the tests with:

```bash
./gradlew test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.