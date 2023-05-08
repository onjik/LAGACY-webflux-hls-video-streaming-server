package com.oj.videostreamingserver.domain.vod.component;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.component.PathManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.nio.file.Path;

@Configuration
public class VodBeanConfig {
    @Value("${volume.media}")
    private String MEDIA_VOLUME_ROOT;

    @PostConstruct
    protected void init() throws NoSuchFieldException {
        PathManager.setMediaRootPath(Path.of(MEDIA_VOLUME_ROOT));
    }

    @Bean
    public EncodingChannel encodingChannel(){
        return new EncodingChannel();
    }


}
