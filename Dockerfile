FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon \
    && JAR_FILE="$(find build/libs -maxdepth 1 -type f \
         -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /workspace/app.jar


FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --system app \
    && useradd \
       --system \
       --gid app \
       --home-dir /app \
       --shell /usr/sbin/nologin \
       app \
    && mkdir -p /app/runtime/keys \
    && chown -R app:app /app

WORKDIR /app

COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar
COPY --chown=app:app docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod 500 /app/docker-entrypoint.sh

USER app

EXPOSE 8081

ENTRYPOINT ["/app/docker-entrypoint.sh"]
