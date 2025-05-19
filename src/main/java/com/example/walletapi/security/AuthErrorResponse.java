package com.example.walletapi.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.walletapi.exception.UnauthorizedException;

/**
 * Default Spring Boot authentication error handler. This *should* only be
 * called when no Authorization header
 * is provided for endpoints defined in {@link SecurityConfig#PRE_AUTH}.
 */
@Component
public class AuthErrorResponse implements AuthenticationEntryPoint {

	@Override
	public void commence(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		throw new UnauthorizedException(authException.getMessage());
	}

}