package com.example.demoapi.controller;

import com.example.demoapi.model.Product;
import com.example.demoapi.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ProductService productService;

	@Test
	void shouldGetAllProducts() throws Exception {
		// Given
		Product product1 = new Product(1L, "Product 1", "Description 1", 10.0);
		Product product2 = new Product(2L, "Product 2", "Description 2", 20.0);

		when(productService.findAll()).thenReturn(Arrays.asList(product1, product2));

		// When & Then
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].name", is("Product 1")))
				.andExpect(jsonPath("$[1].id", is(2)))
				.andExpect(jsonPath("$[1].name", is("Product 2")));
	}

	@Test
	void shouldGetProductById() throws Exception {
		// Given
		Product product = new Product(1L, "Test Product", "Test Description", 10.0);
		when(productService.findById(1L)).thenReturn(Optional.of(product));

		// When & Then
		mockMvc.perform(get("/api/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Test Product")))
				.andExpect(jsonPath("$.description", is("Test Description")))
				.andExpect(jsonPath("$.price", is(10.0)));
	}

	@Test
	void shouldReturn404WhenProductNotFound() throws Exception {
		// Given
		when(productService.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		mockMvc.perform(get("/api/products/1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldCreateProduct() throws Exception {
		// Given
		Product productToCreate = Product.builder()
				.name("New Product")
				.description("New Description")
				.price(15.0)
				.build();

		Product createdProduct = Product.builder()
				.id(1L)
				.name("New Product")
				.description("New Description")
				.price(15.0)
				.build();

		when(productService.save(any(Product.class))).thenReturn(createdProduct);

		// When & Then
		mockMvc.perform(post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productToCreate)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("New Product")))
				.andExpect(jsonPath("$.description", is("New Description")))
				.andExpect(jsonPath("$.price", is(15.0)));
	}

	@Test
	void shouldUpdateProduct() throws Exception {
		// Given
		Product existingProduct = new Product(1L, "Existing Product", "Existing Description", 10.0);
		Product updatedProduct = new Product(1L, "Updated Product", "Updated Description", 20.0);

		when(productService.findById(1L)).thenReturn(Optional.of(existingProduct));
		when(productService.save(any(Product.class))).thenReturn(updatedProduct);

		// When & Then
		mockMvc.perform(put("/api/products/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updatedProduct)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("Updated Product")))
				.andExpect(jsonPath("$.description", is("Updated Description")))
				.andExpect(jsonPath("$.price", is(20.0)));
	}

	@Test
	void shouldDeleteProduct() throws Exception {
		// Given
		Product product = new Product(1L, "Test Product", "Test Description", 10.0);
		when(productService.findById(1L)).thenReturn(Optional.of(product));
		doNothing().when(productService).deleteById(1L);

		// When & Then
		mockMvc.perform(delete("/api/products/1"))
				.andExpect(status().isNoContent());

		verify(productService, times(1)).deleteById(1L);
	}
}