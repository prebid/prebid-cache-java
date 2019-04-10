FROM frolvlad/alpine-java:jdk8-slim
VOLUME /tmp
ADD prebid-cache.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -server -XX:+AlwaysPreTouch -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.security.egd=file:/dev/./urandom -jar /app.jar
