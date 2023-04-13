#base
FROM gradle:jdk11-focal

MAINTAINER xodks672@gmail.com

WORKDIR /

EXPOSE 8000 12342

# 변수 (빌드파일 경로)
ARG JAR_FILE=build/libs/*.jar
# 컨테이너로 복사
COPY $JAR_FILE app.jar

# 인코딩 툴인 ffmpeg를 설치 /usr/bin/ffmpeg 기본경로
RUN apt-get update
RUN apt-get install -y ffmpeg

# debug를 위해 12342번 포트 사용함, 배포시에는 반드시 바꾸세요
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:12342","-jar","app.jar"]


