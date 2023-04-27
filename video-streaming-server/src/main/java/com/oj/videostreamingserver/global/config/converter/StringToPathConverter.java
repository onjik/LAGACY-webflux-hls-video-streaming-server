package com.oj.videostreamingserver.global.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.nio.file.Path;

@ReadingConverter
public class StringToPathConverter implements Converter<String, Path>{
    @Override
    public Path convert(String source) {
        return Path.of(source);
    }
}
