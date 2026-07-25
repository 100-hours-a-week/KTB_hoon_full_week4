# 1단계: 애플리케이션 빌드
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2단계: 애플리케이션 실행
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]