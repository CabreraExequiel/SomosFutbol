FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -B verify

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build /workspace/target/api-*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Argentina/Buenos_Aires", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
