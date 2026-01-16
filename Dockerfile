FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia il fat jar
COPY build/libs/app.jar app.jar

# Cartella per immagini
RUN mkdir -p /app/uploads

# Porta Ktor
EXPOSE 8080

# Avvio
ENTRYPOINT ["java", "-jar", "app.jar"]