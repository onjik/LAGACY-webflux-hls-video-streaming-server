package com.oj.videostreamingserver.global.config;

import com.oj.videostreamingserver.global.config.converter.BinaryToUUIDConverter;
import com.oj.videostreamingserver.global.config.converter.StringToVideoStatusConverter;
import com.oj.videostreamingserver.global.config.converter.UUIDToBinaryConverter;
import com.oj.videostreamingserver.global.config.converter.VideoStatusToStringConverter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${r2dbc.url}")
    private String dbUrl;

    private final Environment environment;

    public R2dbcConfig(Environment environment) {
        this.environment = environment;
    }

    @NonNull
    @Bean
    @Primary
    @Override
    public ConnectionFactory connectionFactory() {
        return ConnectionFactories.get(dbUrl);
    }

    @Bean(name = "r2dbcTransactionManager")
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory){
        return new R2dbcTransactionManager(connectionFactory);
    }




    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(DatabaseClient databaseClient) {
        var dialect = DialectResolver.getDialect(databaseClient.getConnectionFactory());
        var converters = new ArrayList<>(dialect.getConverters());
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);

        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder(), converters),
                getCustomConverters());
    }

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(
                new UUIDToBinaryConverter(),new BinaryToUUIDConverter(),
                new VideoStatusToStringConverter(),new StringToVideoStatusConverter());
    }
}
