package com.example.walletapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Now all endpoints require a JWT, so make sure one is set...
		String authorizationHeader = request.getHeader("Authorization");
		String token;
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			token = authorizationHeader.substring(7);
		} else {
			throw new BadCredentialsException("No JWT found in the Authorization header");
		}

		// ... and that is's valid (can be parsed with our secret key)...
		JwtUtil.Jwt jwt;
		UUID walletId;
		try {
			jwt = jwtUtil.parseToken(token);
			// ...and that it has a walletId which is a valid UUID.
			walletId = jwt.getUserId(UUID.class);
		} catch (Exception e) {
			throw new BadCredentialsException("The JWT wasn't valid", e);
		}

		// ...and not expired
		if (jwt.isExpired()) {
			throw new BadCredentialsException("The JWT is expired");
		}

		// OK, it's a valid JWT, let's store the walletId as the security context user
		// so we can use it in the controller
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			jwtUtil.setAuthenticatedUser(request, walletId);
		} else {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unexpected authentication state: Authentication already present.");
		}

		// Continue to the next filter (and eventually hit the controller...)
		filterChain.doFilter(request, response);
	}
}