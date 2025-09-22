# Build stage
FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
# Copy Maven wrapper first and make it executable
COPY mvnw .
RUN chmod +x mvnw
COPY .mvn/ .mvn

# Copy the rest of the files
COPY pom.xml .
COPY src/ src/

# Add a cache for .m2 directory
# Download dependencies only
RUN ./mvnw dependency:go-offline


# Build the application
RUN ./mvnw clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /cardcost
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]