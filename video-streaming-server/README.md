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

# api
# POST /media/vods/{videoId}

인코딩 큐에 등록하는 api, 처음 비디오를 등록할때 사용

## Request

1. URL : /media/vods/{videoId}
2. Method : POST
3. Path Param : videoId (UUID)
4. header : Content-Type → multipart/form-data

### Request Body (Multipart data)

- video (mandatory)
    - 등록하려는 “비디오” 파일
    - content-type = video/*
- thumbnail (optional)
    - 원하는 썸네일 “이미지” 파일
    - content-type = image/*

## Response

### Status

1. 200 → 성공적으로 등록됨
2. 400 (Bad Request) → 잘못된 형식의 요청
    1. 필수 필드 없음
    2. 필드의 형식이나 컨텐츠 타입이 잘못됨
    3. 존재하지 않은 비디오 아이디로 요청
    4. 이미 인코딩 중이거나 이미 등록된 비디오에 대해 등록 요청
3. 500 (Internal Server Error) → 서버에러

### Body(정상적일 경우)

JSON

- `videoId` : 요청이 성공한 videoId
- `videoLength` : 등록된 비디오의 길이 (초 단위)
- `message` : `"video successfully registered in encoding queue"`

### Body(에러 발생시)

```java
private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

public class FieldError {
    private String field;
    private String value;
    private String reason;
```

위 필드가 JSON으로 변환되어 출력됨

# DELETE /media/vods/{videoId}

비디오를 삭제하는 api

## Request

1. URL : /media/vods/{videoId}
2. Method : DELETE
3. Path Param : videoId (UUID)

## Response

### Status

1. 200 → 성공적으로 삭제됨
2. 400 (Bad Request) → 잘못된 형식의 요청
    1. 존재하지 않는 비디오 아이디
    2. 잘못된 요청
3. 500 (Internal Server Error) → 서버에러

### Body(에러 발생시)

```java
private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

public class FieldError {
    private String field;
    private String value;
    private String reason;
```

위 필드가 JSON으로 변환되어 출력됨

# PATCH /media/vods/{videoId}/thumbnail

썸네일을 교체하는 api

## Request

1. URL : /media/vods/{videoId}/thumbnail
2. Method : PATCH
3. Path Param : videoId (UUID)
4. header : Content-Type → multipart/form-data

### Request Body (Multipart data)

- thumbnail (mandatory)
    - 원하는 썸네일 “이미지” 파일
    - content-type = image/*

## Response

### Status

1. 200 → 성공적으로 등록됨
2. 400 (Bad Request) → 잘못된 형식의 요청
    1. 잘못된 비디오 아이디, 아직 등록되지 않은 (완전히 인코딩 되지 않은) 비디오
    2. 잘못된 요청
3. 500 (Internal Server Error) → 서버에러

### Body(에러 발생시)

```java
private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

public class FieldError {
    private String field;
    private String value;
    private String reason;
```

위 필드가 JSON으로 변환되어 출력됨

# GET /media/vods/{videoId}/encoding/status

단발성으로 현재 인코딩 상태를 조회하는 api

## Request

1. URL : /media/vods/{videoId}/encoding/status
2. Method : GET
3. Path Param : videoId (UUID)

## Response

### Status

1. 200 → 성공적으로 등록됨
2. 400 (Bad Request) → 잘못된 형식의 요청
    1. 필수 필드 없음
    2. 필드의 형식이나 컨텐츠 타입이 잘못됨
    3. 존재하지 않은 비디오 아이디로 요청
    4. 이미 인코딩 중이거나 이미 등록된 비디오에 대해 등록 요청
3. 500 (Internal Server Error) → 서버에러

### Body(정상적일 경우)

JSON

- `entireJobStatus` : 전체 작업의 진행 상태
    - ***READY***,***RUNNING***,***COMPLETE***,***ERROR***
- *Map*<EncodingChannel.Type , EncodingEvent.Status> : 내부 브라켓에 타입과 상태가 키-밸류로 주어짐
    - Type : ***VIDEO***, ***THUMBNAIL***
    - Status : ***READY***,***RUNNING***,***COMPLETE***,***ERROR***

### Body(에러 발생시)

```java
private String message;
    private String status;
    private List<FieldError> errors;
    private String code;

public class FieldError {
    private String field;
    private String value;
    private String reason;
```

위 필드가 JSON으로 변환되어 출력됨

# GET /media/vods/{videoId}/encoding/statuses

비디오 진행 상황을 SSE로 중계 받는 api ([https://developer.mozilla.org/ko/docs/Web/API/Server-sent_events/Using_server-sent_events](https://developer.mozilla.org/ko/docs/Web/API/Server-sent_events/Using_server-sent_events))

한줄씩 소수점 2자리 float으로 전송됨 (아래는 예시)
```text
2.33
12.87
23.75
35.07
46.50
57.71
69.81
81.47
93.34
100.00
```