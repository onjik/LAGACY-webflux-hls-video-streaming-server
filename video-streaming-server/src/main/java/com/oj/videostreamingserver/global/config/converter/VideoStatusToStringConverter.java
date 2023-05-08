package com.oj.videostreamingserver.global.config.converter;

import com.oj.videostreamingserver.domain.vod.domain.VideoEntry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class VideoStatusToStringConverter implements Converter<VideoEntry.Status, String> {

    @Override
    public String convert(VideoEntry.Status source) {
        return source.name();
    }
}
