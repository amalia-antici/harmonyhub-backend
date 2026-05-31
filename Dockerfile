FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY harmonyhub-backend/.mvn/ .mvn
COPY harmonyhub-backend/mvnw harmonyhub-backend/pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline
COPY harmonyhub-backend/src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/harmonyhub-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
