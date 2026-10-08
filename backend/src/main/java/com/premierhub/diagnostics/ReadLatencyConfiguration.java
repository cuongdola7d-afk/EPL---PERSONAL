package com.premierhub.diagnostics;

import com.premierhub.fantasy.FantasyEntryService;
import com.premierhub.service.FootballQueries;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "premierhub.diagnostics.read-latency.enabled", havingValue = "true")
public class ReadLatencyConfiguration {
    @Bean static BeanPostProcessor readLatencyDataSourceInstrumentation() {
        return new BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean, String name) {
                return bean instanceof DataSource source && !(source instanceof MeasuredDataSource)
                        ? new MeasuredDataSource(source) : bean;
            }
        };
    }

    @Bean FilterRegistrationBean<ReadLatencyFilter> readLatencyFilter(Environment environment, JdbcTemplate jdbc,
            FootballQueries football, FantasyEntryService fantasy, PlatformTransactionManager transactions) {
        var probe = new ReadLatencyProbe(jdbc, football, fantasy, transactions);
        var filter = new ReadLatencyFilter(environment, probe, JsonMapper.builder().build());
        var registration = new FilterRegistrationBean<>(filter);
        // Before Spring Session: session lookup/save and security are included in request totals.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
