FROM eclipse-temurin:17-jre

WORKDIR /app

COPY app.jar app.jar

RUN mkdir -p /app/uploads/listings

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]