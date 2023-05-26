package com.oj.videostreamingserver.domain.vod.config;

import com.oj.videostreamingserver.domain.vod.component.EncodingChannel;
import com.oj.videostreamingserver.domain.vod.util.PathManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
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
