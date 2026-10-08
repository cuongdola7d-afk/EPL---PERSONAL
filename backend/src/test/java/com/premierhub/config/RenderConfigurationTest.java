package com.premierhub.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class RenderConfigurationTest {
    static ApplicationContextRunner configuration(String profiles, String host, String truststore) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=" + profiles, "PORT=18080",
                        "PREMIERHUB_JDBC_URL=jdbc:mysql://" + host + ":33030/render_tls_check",
                        "PREMIERHUB_DB_USER=root", "PREMIERHUB_DB_PASSWORD=Render-tls-local-only",
                        "PREMIERHUB_DB_TRUSTSTORE_URL=" + truststore);
    }

    @Test void renderBindsPortAndVerifiedTlsWithoutInitializingSchema() {
        configuration("prod,render", "localhost", "file:/unused-test-ca.p12").run(context -> {
            assertNull(context.getStartupFailure());
            var environment = context.getEnvironment();
            assertEquals(18080, environment.getProperty("server.port", Integer.class));
            assertEquals("0.0.0.0", environment.getProperty("server.address"));
            assertEquals("never", environment.getProperty("spring.sql.init.mode"));
            assertEquals("never", environment.getProperty("spring.session.jdbc.initialize-schema"));
            assertEquals("false", environment.getProperty("premierhub.minigame.local-data.enabled"));
            assertEquals("none", environment.getProperty("server.forward-headers-strategy"));
            assertEquals("true", environment.getProperty("server.servlet.session.cookie.secure"));
            var source = context.getBean(HikariDataSource.class);
            var properties = source.getDataSourceProperties();
            assertEquals("VERIFY_IDENTITY", properties.getProperty("sslMode"));
            assertEquals("file:/unused-test-ca.p12", properties.getProperty("trustCertificateKeyStoreUrl"));
            assertEquals("PKCS12", properties.getProperty("trustCertificateKeyStoreType"));
            assertEquals("false", properties.getProperty("fallbackToSystemTrustStore"));
            assertEquals("UTC", properties.getProperty("connectionTimeZone"));
            assertEquals("true", properties.getProperty("forceConnectionTimeZoneToSession"));
            assertEquals(4, source.getMaximumPoolSize());
            assertEquals(0, source.getMinimumIdle());
            assertEquals(0, source.getKeepaliveTime());
            assertFalse(source.isRunning(), "The config test must not connect to a database.");
        });
    }

    @Test void existingProdProfileDoesNotActivateRenderSettings() {
        configuration("prod", "localhost", "file:/unused-test-ca.p12").run(context -> {
            assertNull(context.getStartupFailure());
            var source = context.getBean(HikariDataSource.class);
            assertNull(source.getDataSourceProperties().getProperty("sslMode"));
            assertNull(context.getEnvironment().getProperty("server.address"));
            assertEquals("never", context.getEnvironment().getProperty("spring.sql.init.mode"));
            assertFalse(source.isRunning());
        });
    }
}
