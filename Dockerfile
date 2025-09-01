# build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Xms64m -Xmx128m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -Dspring.main.lazy-initialization=true -Dserver.tomcat.max-threads=20 -Dspring.datasource.hikari.maximum-pool-size=3 -Dspring.datasource.hikari.minimum-idle=0"
ENTRYPOINT ["java","-jar","app.jar"]
