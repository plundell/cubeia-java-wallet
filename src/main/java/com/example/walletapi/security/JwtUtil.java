package com.example.walletapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A utility class for handling JWT tokens.
 * 
 * We're leaving this class agnostic to the specific of our JWT implementation.
 * 
 * The only specification we make is that the "id" claim is a UUID. So if you
 */
@Component
public class JwtUtil {

	@Value("${jwt.secret:myDefaultSecretKeyWhichNeedsToBeAtLeast32CharactersLong}")
	private String secret;

	@Value("${jwt.expiration:86400000}") // Default: 24 hours
	private long jwtExpirationMs;

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}

	protected static <T> T cast(Object value, Class<T> type) throws ClassCastException {
		try {
			if (type == UUID.class) {
				return type.cast(UUID.fromString(value.toString()));
			} else if (type == Integer.class) {
				return type.cast(Integer.parseInt(value.toString()));
			} else if (type == Long.class) {
				return type.cast(Long.parseLong(value.toString()));
			} else if (type == Double.class) {
				return type.cast(Double.parseDouble(value.toString()));
			} else if (type == Boolean.class) {
				return type.cast(Boolean.parseBoolean(value.toString()));
			} else {
				return type.cast(value);
			}
		} catch (Exception e) {
			throw new ClassCastException(
					"Value " + value + " could not be cast to " + type.getName());
		}
	}

	/**
	 * Generates a JWT token with a single claim "id".
	 * 
	 * @param userId The ID to include in the token
	 * @return The generated JWT token
	 */
	public String generateToken(Object userId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("id", userId);
		return this.generateToken(claims);
	}

	/**
	 * Generates a JWT token with an arbitrary map of claims.
	 * 
	 * @param claims The claims to include in the token
	 * @return The generated JWT token
	 */
	public String generateToken(Map<String, Object> claims) {
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public Jwt parseToken(String token) {
		Claims claims = Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
		return new Jwt(claims);
	}

	/**
	 * A simple wrapper around the JWT claims.
	 */
	public static class Jwt {
		private Claims claims;

		public Jwt(Claims claims) {
			this.claims = claims;
		}

		public Date getExpiration() {
			return claims.getExpiration();
		}

		public boolean isExpired() {
			final Date expiration = this.getExpiration();
			return expiration.before(new Date());
		}

		public <T> T get(String key, Class<T> type) throws IllegalArgumentException, ClassCastException {
			if (!this.claims.containsKey(key)) {
				throw new IllegalArgumentException("Key not found: " + key);
			} else {
				try {
					return cast(this.claims.get(key), type);
				} catch (ClassCastException e) {
					throw new ClassCastException(
							"Key " + key + " existed on JWT, but could not be cast to " + type.getName());
				}
			}
		}

		public <T> T getUserId(Class<T> type) {
			return this.get("id", type); // same key as used in generateToken
		}

	}

	/**
	 * Gets the authenticated user as a string from the security context. If you
	 * need to get the user as a different type, use
	 * {@link #getAuthenticatedUser(Class)}
	 * 
	 * @return A string representing the user or null if not authenticated
	 */
	public String getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			if (!"anonymousUser".equals(authentication.getPrincipal())) {
				return authentication.getName();
			}
		}
		return null;
	}

	/**
	 * Gets the authenticated user from the security context.
	 * 
	 * @param type The type to cast the user to
	 * 
	 * @return The authenticated user or null if not authenticated
	 */
	public <T> T getAuthenticatedUser(Class<T> type) {
		String user = getAuthenticatedUser();
		if (user == null) {
			return null;
		} else {
			return cast(user, type);
		}
	}

	/**
	 * Sets a user ID on the security context
	 * 
	 * @param request The HTTP request
	 * @param user    The user object to set. Can be a String or a UUID
	 * @throws ResponseStatusException A 500 server error if the if the security
	 *                                 context has already been set
	 */
	public void setAuthenticatedUser(HttpServletRequest request, Object user) throws ResponseStatusException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					user, null,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authToken);
		} else {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected authentication state");
		}
	}
}