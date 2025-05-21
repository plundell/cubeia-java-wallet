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

		// Ugly hack. Checking the uri here since it doesn't seem to matter what i do in
		// SecurityConfig
		String uri = request.getRequestURI();
		if (!uri.startsWith("/api/wallet/v1/protected")) {
			logger.warn("TODO: Fix chain matching in SecurityConfig.Authorization filter run for public endpoint "
					+ uri + " , skipping...");
			filterChain.doFilter(request, response);
			return;
		}

		logger.info("PROCESSING REQUEST FOR AUTHORIZATION...");

		try {
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
			jwtUtil.setAuthenticatedUser(request, walletId);

			logger.info("AUTHORIZATION SUCCESSFUL. CONTINUING TO THE NEXT FILTER...");
			// Continue to the next filter (and eventually hit the controller...)
			filterChain.doFilter(request, response);

		} catch (Exception e) {
			logger.error(e);
			throw e;
		}
	}
}