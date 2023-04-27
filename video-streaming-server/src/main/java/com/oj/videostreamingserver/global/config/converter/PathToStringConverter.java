package com.oj.videostreamingserver.global.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.nio.file.Path;

@WritingConverter
public class PathToStringConverter implements Converter<java.nio.file.Path, String> {

    @Override
    public String convert(Path source) {
        return source.toAbsolutePath().toString();
    }
}
