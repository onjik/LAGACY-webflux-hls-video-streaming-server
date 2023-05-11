package com.oj.videostreamingserver.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 시스템 환경 변수가 존재하면 기존의 값을 덮어 씌운다.
 *
 * 시스템 환경 변수 리스트
 * - DATABASE_URL : R2DBC 연결 URL (Mysql)
 * - FFMPEG_PATH : docker container 내부의 ffmpeg 실행 파일 경로
 * - FFPROBE_PATH : docker container 내부의 ffprobe 실행 파일 경로
 * - MEDIA_STORAGE_PATH : docker container 내부의 미디어 파일 저장 경로
 * - LOG_LEVEL : 로그 레벨
 * - SERVER_PORT : 서버 포트
 *
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecretEnvironmentLoader implements EnvironmentPostProcessor {

    // 환경 변수와 매핑할 프로퍼티 이름
    enum PropertyName {
        DATABASE_URL("r2dbc.url"),
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


    private final ResourceLoader loader = new DefaultResourceLoader();
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        //prod 환경이면 -> 시스템 환경변수를 하나씩 스캔하고 존재하면, 기존의 프로퍼티를 덮어씌운다.
        if (environment.getActiveProfiles()[0].equals("prod")) {
            scanSystemEnvironment(environment.getPropertySources());
        }
    }

    private void scanSystemEnvironment(MutablePropertySources propertySources) {
        Map<String,Object> systemEnvMap = new HashMap<>();
        for (PropertyName propertyName : PropertyName.values()) {
            String systemEnv = System.getenv(propertyName.name());
            if (Objects.nonNull(systemEnv)) {
                systemEnvMap.put(propertyName.getPropertyName(),systemEnv);
            }
        }
        //시스템 변수를 통해 지정된 프로퍼티를 맨 앞에 추가한다.
        propertySources.addFirst(new MapPropertySource("systemEnvProperty",systemEnvMap));
    }
}

