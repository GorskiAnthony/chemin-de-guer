FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# Exécution sans privilèges root
# Bibliothèques de rendu des polices, nécessaires à la génération des images de partage (Java2D)
RUN apt-get update && apt-get install -y --no-install-recommends libfreetype6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --no-create-home --uid 10001 app
COPY --from=build /app/target/*.jar app.jar
USER app
# Limite la JVM à une part de la RAM du conteneur et la fait s'arrêter proprement en cas de saturation (restart: unless-stopped)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
