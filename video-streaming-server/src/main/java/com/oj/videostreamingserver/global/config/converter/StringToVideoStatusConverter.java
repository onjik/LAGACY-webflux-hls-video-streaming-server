package com.oj.videostreamingserver.global.config.converter;

import com.oj.videostreamingserver.domain.vod.domain.VodVideo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToVideoStatusConverter implements Converter<String, VodVideo.Status> {
    @Override
    public VodVideo.Status convert(String source) {
        return VodVideo.Status.valueOf(source.toUpperCase());
    }

}
