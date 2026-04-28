FROM eclipse-temurin:25-jdk-jammy

WORKDIR /app

COPY target/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
