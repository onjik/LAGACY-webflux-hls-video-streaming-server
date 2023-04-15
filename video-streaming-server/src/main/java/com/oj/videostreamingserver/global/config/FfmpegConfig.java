package com.oj.videostreamingserver.global.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FfmpegConfig {

    @Value("${path.ffmpeg}")
    private String ffmpegPath;
    @Value("${path.ffprobe}")
    private String ffprobePath;


    @Bean
    public FFmpeg fFmpeg(){
        try {
            return new FFmpeg(ffmpegPath);
        } catch (IOException e) {
            throw new ApplicationContextException("can not found ffmpeg : " + ffmpegPath);
        }
    }

    @Bean
    public FFprobe fFprobe(){
        try {
            return new FFprobe(ffprobePath);
        } catch (IOException e) {
            throw new ApplicationContextException("can not found ffprobe : " + ffprobePath);
        }
    }
}
