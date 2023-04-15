package com.oj.videostreamingserver.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Order(Ordered.LOWEST_PRECEDENCE)
public class SecretEnvironmentLoader implements EnvironmentPostProcessor {


    private final ResourceLoader loader = new DefaultResourceLoader();
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        //volume.secret 에 등록된 시크릿 yml 패스를 로딩해서 그 파일의 내용을 환경변수로 등록
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        List<PropertySource<?>> secrets;
        try {
            secrets = yamlPropertySourceLoader.load("secrets", loader.getResource(Objects.requireNonNull(environment.getProperty("volume.secret"))));
        } catch (IOException e) {
            throw new ApplicationContextException("can't load secret setting");
        }

        secrets.forEach(environment.getPropertySources()::addLast);

    }
}
