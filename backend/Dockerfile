FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src/main ./src/main
# These two reviewed CSV resources are included by pom.xml; no database export is copied.
COPY data/match-lineups-2026-10-04/clubs.csv data/match-lineups-2026-10-04/players.csv ./data/match-lineups-2026-10-04/
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod,render \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50.0 -XX:InitialRAMPercentage=10.0 -Duser.timezone=UTC"
COPY --from=build --chown=10001:10001 /build/target/premierhub-backend-0.1.0-SNAPSHOT.jar /app/app.jar
COPY --chown=10001:10001 docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0755 /app/docker-entrypoint.sh
USER 10001:10001
EXPOSE 10000
ENTRYPOINT ["/app/docker-entrypoint.sh"]
