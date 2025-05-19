package com.example.walletapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import com.example.walletapi.exception.UnauthorizedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Skip any endpoints which don't require authentication
		if (!Arrays.asList(SecurityConfig.PRE_AUTH).contains(request.getRequestURI())) {

			// Now all endpoints require a JWT, so make sure one is set...
			String authorizationHeader = request.getHeader("Authorization");
			String token;
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				token = authorizationHeader.substring(7);
			} else {
				throw new UnauthorizedException("No JWT found in the Authorization header");
			}

			// ... and that is's valid (can be parsed with our secret key)...
			JwtUtil.Jwt jwt;
			String walletId;
			try {
				jwt = jwtUtil.parseToken(token);
				// ...and that it has a walletId which is a valid UUID.
				walletId = jwt.getStringClaim("walletId");
			} catch (Exception e) {
				throw new UnauthorizedException("The JWT wasn't valid");
			}

			// ...and not expired
			if (jwt.isExpired()) {
				throw new UnauthorizedException("The JWT is expired");
			}

			// OK, it's a valid JWT, let's store the walletId as the security context user
			// so we can use it in the controller
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				jwtUtil.setAuthenticatedUser(request, walletId);
			} else {
				// There shouldn't be an authentication already set up, so throw an error
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected authentication state");
			}

			// REMEMBER: We still haven't checked if the JWT is valid for the wallet in the
			// URL, that happens in the controller
		}

		// Continue to the next filter (and eventually hit the controller...)
		filterChain.doFilter(request, response);
	}
}