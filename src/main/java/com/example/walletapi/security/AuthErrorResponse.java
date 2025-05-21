package com.example.walletapi.security;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.access.AccessDeniedException; // Spring Security's AccessDeniedException
import org.springframework.stereotype.Component;

/**
 * Handles failed security related responses. This is registered by
 * {@link SecurityConfig#protectedApiFilterChain}.
 * 
 * <ul>
 * <li>Implements AuthenticationEntryPoint to handle authentication failures
 * (401).</li>
 * <li>Implements AccessDeniedHandler to handle authorization failures
 * (403).</li>
 * </ul>
 */
@Component
public class AuthErrorResponse implements AuthenticationEntryPoint, AccessDeniedHandler {

	private Logger logger;

	private Logger getLogger() {
		if (this.logger == null) {
			this.logger = LoggerFactory.getLogger(this.getClass());
		}
		return this.logger;
	}

	@Override
	public void commence(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException, ServletException {
		// server side logging for debug
		getLogger().warn("Handling an AuthenticationException and responding with: "
				+ HttpServletResponse.SC_UNAUTHORIZED + " " + authException.getMessage(), authException);
		// respond to client
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
	}

	@Override
	public void handle(
			HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException, ServletException {
		// server side logging for debug
		getLogger().warn("Handling an AccessDeniedException and responding with: "
				+ HttpServletResponse.SC_FORBIDDEN + " " + accessDeniedException.getMessage(), accessDeniedException);
		// respond to client
		response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
	}
}