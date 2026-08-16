FROM eclipse-temurin:17-jre
COPY sentinel-dashboard-1.8.9.jar /app/sentinel-dashboard.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-jar", "/app/sentinel-dashboard.jar"]