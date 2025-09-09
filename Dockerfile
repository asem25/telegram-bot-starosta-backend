# build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# run
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENV JAVA_TOOL_OPTIONS=" \
 -XX:ActiveProcessorCount=1 \
 -XX:+UseSerialGC \
 -XX:MaxRAMPercentage=60 -XX:InitialRAMPercentage=24 \
 -Xss256k \
 -XX:MaxMetaspaceSize=108m \
 -XX:ReservedCodeCacheSize=16m -XX:MaxDirectMemorySize=24m \
 -XX:+ClassUnloading -XX:+ExitOnOutOfMemoryError \
 -Dspring.jmx.enabled=false -Dspring.main.lazy-initialization=true \
 -Dserver.port=8081 \
 -Dserver.tomcat.max-threads=16 -Dserver.tomcat.accept-count=50 \
 -Dspring.datasource.hikari.maximum-pool-size=3 -Dspring.datasource.hikari.minimum-idle=0 \
"
ENTRYPOINT ["java","-jar","app.jar"]
