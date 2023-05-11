# Notice
## if active profile is 'prod'
시스템 환경변수를 스캔하여 설정 값을 덮어 씌웁니다. 

시스템 환경 변수 리스트
- DATABASE_URL : R2DBC 연결 URL (Mysql)
- FFMPEG_PATH : docker container 내부의 ffmpeg 실행 파일 경로
- FFPROBE_PATH : docker container 내부의 ffprobe 실행 파일 경로
- MEDIA_STORAGE_PATH : docker container 내부의 미디어 파일 저장 경로
- LOG_LEVEL : 로그 레벨
- SERVER_PORT : 서버 포트

```java
    // 환경 변수와 매핑할 프로퍼티 이름
    enum PropertyName {
        DATABASE_URL("spring.r2dbc.url"),
        FFMPEG_PATH("path.ffmpeg"),
        FFPROBE_PATH("path.ffprobe"),
        MEDIA_STORAGE_PATH("volume.media"),
        LOG_LEVEL("logging.level.root"),
        SERVER_PORT("server.port");

        private final String propertyName;

        PropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }
```