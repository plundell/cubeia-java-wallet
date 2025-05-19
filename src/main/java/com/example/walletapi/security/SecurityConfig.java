package com.example.walletapi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.walletapi.exception.FilterChainExceptionHandler;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * Endpoints that don't require authentication
	 */
	public static final String[] PRE_AUTH = {
			"/api/v1/wallet/create",
			"/api/v1/wallet/help",
	};

	@Autowired
	private AuthErrorResponse authErrorResponse;

	@Autowired
	private JwtAuthFilter jwtAuthenticationFilter;

	@Bean
	public FilterChainExceptionHandler exceptionHandlerFilter() {
		return new FilterChainExceptionHandler();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors().and()
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.exceptionHandling().authenticationEntryPoint(authErrorResponse).and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PRE_AUTH).permitAll()
						.anyRequest().authenticated());

		// Add our exception handler filter first
		http.addFilterBefore(exceptionHandlerFilter(), UsernamePasswordAuthenticationFilter.class);

		// Then add the JWT filter
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token", "x-xsrf-token"));
		configuration.setExposedHeaders(Arrays.asList("x-xsrf-token"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}