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

ENV DATABASE_URL #R2DBC 연결 URL (Mysql)
ENV FFMPEG_PATH #docker container 내부의 ffmpeg 실행 파일 경로
ENV FFPROBE_PATH # docker container 내부의 ffprobe 실행 파일 경로
ENV MEDIA_STORAGE_PATH # docker container 내부의 미디어 파일 저장 경로
ENV LOG_LEVEL #로그 레벨
ENV SERVER_PORT #서버 포트

VOLUME ["/app/media"]
#EXPOSE 9000 12342
EXPOSE 9000


#ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:12342","-jar","app.jar","-Dspring.profiles.active=prod","--server.port=9000"]
ENTRYPOINT ["java","-jar","/app/app.jar","-Dspring.profiles.active=prod","--server.port=9000"]


