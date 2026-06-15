FROM amazoncorretto:24-alpine-jdk
LABEL authors="Wertiba"

WORKDIR /app

COPY . .

RUN ./gradlew clean build -x test

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "./build/libs/fish-0.0.1-SNAPSHOT.jar"]
