# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S stockpro && adduser -S stockpro -G stockpro
COPY --from=build /app/target/*.jar app.jar
USER stockpro
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["java","-Xmx192m","-Xms64m","-XX:+UseContainerSupport","-Dspring.profiles.active=docker","-jar","app.jar"]
