package com.premierhub.web;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class ErrorResponseAssertions {
    private ErrorResponseAssertions() {
    }

    static void expectError(ResultActions result, HttpStatus httpStatus,
                            String code, String path) throws Exception {
        result.andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(httpStatus.value()))
                .andExpect(jsonPath("$.error").value(httpStatus.getReasonPhrase()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
