# Build stage
FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app

# Copy Maven wrapper files first and make it executable
COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

# Copy only pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .

# Download dependencies offline (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /cardcost
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]