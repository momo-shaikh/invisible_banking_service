FROM node:20-bookworm-slim AS frontend-build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY src/frontend ./src/frontend
COPY index.html vite.config.js ./
RUN npm run build

FROM eclipse-temurin:23-jdk AS backend-build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src/main ./src/main
COPY src/test ./src/test
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static

RUN ./gradlew test bootJar --no-daemon

FROM eclipse-temurin:23-jre
WORKDIR /app

ENV SERVER_PORT=8080
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/bank.db
ENV SPRING_SQL_INIT_MODE=always

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build /app/build/libs/*.jar app.jar

EXPOSE 8080
VOLUME ["/data"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD curl --fail --silent http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
