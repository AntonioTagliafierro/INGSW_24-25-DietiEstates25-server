FROM eclipse-temurin:17-jre

WORKDIR /app

COPY app.jar app.jar
COPY entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["./entrypoint.sh"]