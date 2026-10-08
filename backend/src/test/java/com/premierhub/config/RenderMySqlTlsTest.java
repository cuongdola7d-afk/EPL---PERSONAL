package com.premierhub.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, fixed loopback TLS server and synthetic data only; no production URL/credentials. */
@EnabledIfEnvironmentVariable(named = "PRISMAXI_RENDER_MYSQL_TEST", matches = "true")
class RenderMySqlTlsTest {
    private String store(String name) {
        Path root = Path.of("target/render-aiven-tls-check").toAbsolutePath().normalize();
        Path store = root.resolve(name).normalize();
        assertTrue(store.startsWith(root) && Files.isRegularFile(store), "Prepare the isolated TLS fixture first.");
        return store.toUri().toString();
    }

    @Test void verifiesCaAndHostnameAndUsesUtc() {
        RenderConfigurationTest.configuration("prod,render", "localhost", store("trusted.p12")).run(context -> {
            try (var connection = context.getBean(HikariDataSource.class).getConnection();
                 var statement = connection.createStatement()) {
                try (var result = statement.executeQuery("SELECT DATABASE(),@@session.time_zone")) {
                    assertTrue(result.next());
                    assertEquals("render_tls_check", result.getString(1));
                    assertTrue(result.getString(2).equals("+00:00") || result.getString(2).equals("UTC"));
                }
                try (var result = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
                    assertTrue(result.next());
                    assertFalse(result.getString(2).isBlank());
                }
            }
        });
    }

    @Test void refusesAnUntrustedCertificate() {
        RenderConfigurationTest.configuration("prod,render", "localhost", store("untrusted.p12")).run(context -> {
            assertThrows(SQLException.class, () -> context.getBean(HikariDataSource.class).getConnection());
        });
    }

    @Test void refusesHostnameMismatchDespiteTrustedCa() {
        // The isolated certificate has DNS:localhost only, without an IP SAN.
        RenderConfigurationTest.configuration("prod,render", "127.0.0.1", store("trusted.p12")).run(context -> {
            assertThrows(SQLException.class, () -> context.getBean(HikariDataSource.class).getConnection());
        });
    }
}
