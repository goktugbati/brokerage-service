#!/bin/bash

# Start supporting services in Docker
echo "Starting Kafka and supporting services in Docker..."
docker-compose -f docker/docker-compose-dev.yml up -d

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 10

# Run the Spring Boot application locally with the local profile
#echo "Starting Spring Boot application locally..."
#./gradlew bootRun --args='--spring.profiles.active=local'