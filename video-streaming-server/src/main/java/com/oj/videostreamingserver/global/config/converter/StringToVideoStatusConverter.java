package com.oj.videostreamingserver.global.config.converter;

import com.oj.videostreamingserver.domain.vod.domain.VideoEntry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToVideoStatusConverter implements Converter<String, VideoEntry.Status> {
    @Override
    public VideoEntry.Status convert(String source) {
        return VideoEntry.Status.valueOf(source.toUpperCase());
    }

}
