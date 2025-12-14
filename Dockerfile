# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the entire project
COPY pom.xml .
COPY src ./src
COPY frontend ./frontend

# Build both frontend and backend together
# Maven will: 1) Install Node/npm, 2) Build React app, 3) Copy to static/, 4) Build Spring Boot jar
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar (contains both backend and frontend)
COPY --from=build /app/target/*.jar app.jar

# Create data directory for H2 database
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
