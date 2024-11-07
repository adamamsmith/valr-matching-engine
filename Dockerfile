FROM gradle:8.4-jdk21 AS builder

WORKDIR /builder

COPY build.gradle.kts settings.gradle.kts /builder/
COPY src /builder/src

RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21.0.5_11-jdk-ubi9-minimal

WORKDIR /matching-engine

COPY --from=builder /builder/build/libs/*.jar engine.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "engine.jar"]