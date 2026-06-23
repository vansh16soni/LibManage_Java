# Stage 1: Build with Maven
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/libmanage.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
