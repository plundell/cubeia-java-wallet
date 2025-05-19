package com.example.walletapi.exception;

import com.example.walletapi.exception.ControllerExceptionHandler.ErrorResponseMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles exceptions thrown by Spring Boot filters before the controllers are
 * hit. For errors thrown after the controllers see
 * {@link ControllerExceptionHandler}.
 * 
 * NOTE: This needs to be added in the {@link SecurityConfig#filterChain} method
 * as the first filter (that implies it's able to catch everything that comes
 * after it)
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FilterChainExceptionHandler extends OncePerRequestFilter {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
		} catch (ResponseStatusException ex) {
			// Convert ResponseStatusException directly
			setErrorResponse(response, ex.getStatusCode().value(), ex.getReason(), request.getRequestURI());
		} catch (AuthenticationException ex) {
			// Convert Spring Security AuthenticationExceptions to our UnauthorizedException
			UnauthorizedException unauthorizedException = new UnauthorizedException(ex.getMessage());
			setErrorResponse(response, unauthorizedException.getStatusCode().value(),
					unauthorizedException.getReason(), request.getRequestURI());
		} catch (RuntimeException ex) {
			// Convert any other runtime exceptions
			setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					ex.getMessage(), request.getRequestURI());
		}
	}

	private void setErrorResponse(HttpServletResponse response, int status, String message, String path) {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		ErrorResponseMap errorDetails = new ErrorResponseMap(status, message, path);

		try {
			objectMapper.writeValue(response.getOutputStream(), errorDetails);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}