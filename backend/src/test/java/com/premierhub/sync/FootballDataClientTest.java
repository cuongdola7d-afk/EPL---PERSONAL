package com.premierhub.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FootballDataClientTest {
    @TempDir Path directory;

    @Test
    void environmentTakesPriorityAndLocalFileCanLoadQuotedKeyWithoutShellExport() throws Exception {
        Path file = directory.resolve(".env.local");
        Files.writeString(file, "\uFEFFFOOTBALL_DATA_API_KEY = 'local-test-token'\n");
        assertEquals("env-test-token", FootballDataClient.readKey(" env-test-token ", file));
        assertEquals("local-test-token", FootballDataClient.readKey(null, file));
        assertThrows(IOException.class, () -> FootballDataClient.readKey(null, directory.resolve("missing")));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429})
    @SuppressWarnings("unchecked")
    void httpErrorsStopWithoutRetryOrLeakingBodyOrToken(int status) throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        FootballDataClient client = new FootballDataClient(new ObjectMapper(), http);
        var error = assertThrows(IOException.class, () -> client.download("private-test-token"));
        assertTrue(error.getMessage().contains("HTTP " + status));
        assertFalse(error.getMessage().contains("private-test-token"));
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(response, never()).body();
    }
}
