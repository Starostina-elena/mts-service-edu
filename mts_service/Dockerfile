FROM gradle:9.3-jdk17 AS builder
WORKDIR /workspace
COPY --chown=gradle:gradle . /workspace
RUN gradle --no-daemon clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
