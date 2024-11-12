FROM bellsoft/liberica-openjdk-alpine:17.0.12

WORKDIR /app

COPY build/libs/queue-server-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
