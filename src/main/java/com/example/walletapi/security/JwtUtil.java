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

@Component
public class JwtUtil {

	@Value("${jwt.secret:myDefaultSecretKeyWhichNeedsToBeAtLeast32CharactersLong}")
	private String secret;

	@Value("${jwt.expiration:86400000}") // Default: 24 hours
	private long jwtExpirationMs;

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}

	public String generateToken(String walletId) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("walletId", walletId);
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

	public static class Jwt {
		private Claims claims;

		public Jwt(Claims claims) {
			this.claims = claims;
		}

		public String getStringClaim(String key) {
			return claims.get(key, String.class);
		}

		public Date getExpiration() {
			return claims.getExpiration();
		}

		public boolean isExpired() {
			final Date expiration = this.getExpiration();
			return expiration.before(new Date());
		}
	}

	/**
	 * Gets the authenticated user ID from the security context
	 * 
	 * @return The user ID or null if not authenticated
	 */
	public String getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return authentication.getName();
		}
		return null;
	}

	/**
	 * Sets a user ID on the security context
	 * 
	 * @param request The HTTP request
	 * @param userId  The user ID to set
	 * @throws ResponseStatusException A 500 server error if the if the security
	 *                                 context has already been set
	 */
	public void setAuthenticatedUser(HttpServletRequest request, String userId) throws ResponseStatusException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					userId, null,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authToken);
		} else {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected authentication state");
		}
	}
}