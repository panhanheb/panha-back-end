package com.example.ecommerce.security;

import com.example.ecommerce.exception.ErrorCode;
import com.example.ecommerce.exception.GlobalExceptionHandler;
import com.example.ecommerce.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Writes the standard JSON error envelope from filters, which run before the
 * {@link GlobalExceptionHandler} can see an exception.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code, String message)
            throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.failure(GlobalExceptionHandler.apiError(code, message, List.of(), request.getRequestURI())));
    }
}
