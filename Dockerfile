# 학습자 Spring Boot Gradle 프로젝트를 빌드해 실행 (--scale app=N 용)
FROM gradle:8-jdk21 AS build
WORKDIR /src
COPY . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
