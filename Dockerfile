FROM amazoncorretto:17

WORKDIR /app/prebid-cache

COPY src/main/docker/run.sh ./
COPY target/prebid-cache.jar ./prebid-cache.jar

EXPOSE 8080

ENTRYPOINT [ "/app/prebid-cache/run.sh" ]
