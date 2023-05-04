# build
FROM gradle:latest as builder

WORKDIR /app
COPY . .
RUN ./gradlew bootJar

# run
FROM gradle:jdk11-focal

# 인코딩 툴인 ffmpeg를 설치 /usr/bin/ffmpeg 기본경로
RUN apt-get update
RUN apt-get install -y ffmpeg

WORKDIR /
COPY --from=builder /app/build/libs/*.jar /app/app.jar

VOLUME ["/app/secret", "/app/media"]
#EXPOSE 9000 12342
EXPOSE 9000


#ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:12342","-jar","app.jar","-Dspring.profiles.active=prod","--server.port=9000"]
ENTRYPOINT ["java","-jar","/app/app.jar","-Dspring.profiles.active=prod","--server.port=9000"]


