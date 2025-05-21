package com.example.walletapi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import com.example.walletapi.exception.FilterChainExceptionHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application.
 * 
 * Security works by running requests through "chains" of security filters.
 * Each request only matches a single security chain, and they order they're
 * evaluated is determined by the @Order annotation.
 * 
 * 
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${wallet.api.cors-allowed-origins:localhost")
	private List<String> corsAllowedOrigins;

	@Autowired
	private AuthErrorResponse authErrorResponse;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	// Helper method to apply common configurations
	private void configureCommonHttpSecurity(HttpSecurity http) throws Exception {
		// First thing we do is register an excpetion handler.
		http.addFilterBefore(exceptionHandlerFilter(), UsernamePasswordAuthenticationFilter.class);
		// NOTE: While we don't employ username/password auth, it's possible to use
		// UsernamePasswordAuthenticationFilter.class anyway since it's a reliable
		// marker for "very early in the chain"...

		// Enable CORS. "with defaults" means use a bean of type
		// CorsConfigurationSource, which, incidentally, is something we've created at
		// the bottom of this file
		http.cors(Customizer.withDefaults());

		// CSRF is disabled because it's too much hassel. In prod you should employ it,
		// but for now we're relying on CORS not allowing any other origins...
		http.csrf(csrf -> csrf.disable());

		// The API is stateless, so no sessions are needed
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

	}

	@Bean
	public JwtAuthFilter jwtAuthFilter() {
		return new JwtAuthFilter();
	}

	@Bean
	public FilterChainExceptionHandler exceptionHandlerFilter() {
		return new FilterChainExceptionHandler();
	}

	/**
	 * Security chain for the
	 * {@link com.example.walletapi.controller.WalletControllerProtected
	 * WalletControllerProtected}
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain protectedApiFilterChain(HttpSecurity http) throws Exception {
		logger.info("SecurityConfig: Configuring the PROTECTED chain...");
		// This chain only applies to paths matching this:
		http.securityMatcher("/api/wallet/v1/protected/**"); // Specific matcher

		// Apply common settings
		configureCommonHttpSecurity(http);

		// This chain will throw AuthenticationException and AccessDeniedException
		// and those need to be handled by our AuthErrorResponse class
		http.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authErrorResponse)
				.accessDeniedHandler(authErrorResponse));

		// Require that all requests in this chain are authenticated. This will cause
		// "AuthorizationFilter" to run later in the chain and check for a valid
		// "Authentication" object (see JwtUtil#setAuthenticatedUser), else it will
		// throw errors which are caught by ^
		http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

		// Now register our JWT filter to run before AuthorizationFilter so it can check
		// for a valid JWT and set it as the security context.
		http.addFilterBefore(jwtAuthFilter(), AuthorizationFilter.class);

		return http.build();
	}

	/**
	 * Default security chain for any requests which havn't matched another chain
	 * yet.
	 * 
	 * REMEMBER: Each request only matches a single security chain.
	 */
	@Bean
	@Order(10)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		// No securityMatcher here, so it applies to requests not caught by earlier
		// chains
		logger.info("SecurityConfig: Configuring the DEFAULT chain...");

		configureCommonHttpSecurity(http); // Apply common settings

		http.authorizeHttpRequests(auth -> auth
				.anyRequest().permitAll() // All other requests are permitted (as per user's last change)
		);

		// No JWT filter here

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsAllowedOrigins);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}