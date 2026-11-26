# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system appgroup \
    && useradd --system --gid appgroup --create-home appuser \
    && mkdir -p /app/data \
    && chown -R appuser:appgroup /app

COPY --from=build /workspace/build/libs/birthday-scheduler-*.jar /app/app.jar
RUN rm -f /app/*-plain.jar && chown appuser:appgroup /app/app.jar

USER appuser

ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/birthday-scheduler.db

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
