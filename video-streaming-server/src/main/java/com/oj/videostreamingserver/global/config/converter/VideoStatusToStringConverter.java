package com.oj.videostreamingserver.global.config.converter;

import com.oj.videostreamingserver.domain.vod.domain.VodVideo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class VideoStatusToStringConverter implements Converter<VodVideo.Status, String> {

    @Override
    public String convert(VodVideo.Status source) {
        return source.name();
    }
}
