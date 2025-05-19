package com.example.walletapi.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void testResponseStatusExceptionHandling() throws Exception {
		mockMvc.perform(get("/test/unauthorized"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Test unauthorized exception"));
	}

	@Test
	public void testGenericExceptionHandling() throws Exception {
		mockMvc.perform(get("/test/runtime-exception"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Test runtime exception"));
	}

	@Profile("test")
	@Configuration
	static class TestConfig {
		@Bean
		public TestController testController() {
			return new TestController();
		}
	}

	@RestController
	static class TestController {
		@GetMapping("/test/unauthorized")
		public String throwUnauthorized() {
			throw new UnauthorizedException("Test unauthorized exception");
		}

		@GetMapping("/test/runtime-exception")
		public String throwRuntimeException() {
			throw new RuntimeException("Test runtime exception");
		}
	}
}