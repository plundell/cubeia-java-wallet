package com.example.demoapi.service;

import com.example.demoapi.model.Product;
import com.example.demoapi.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductServiceImpl();
	}

	@Test
	void shouldSaveProduct() {
		// Given
		Product product = Product.builder()
				.name("Test Product")
				.description("Test Description")
				.price(10.0)
				.build();

		// When
		Product savedProduct = productService.save(product);

		// Then
		assertNotNull(savedProduct.getId());
		assertEquals("Test Product", savedProduct.getName());
		assertEquals("Test Description", savedProduct.getDescription());
		assertEquals(10.0, savedProduct.getPrice());
	}

	@Test
	void shouldFindProductById() {
		// Given
		Product product = productService.save(Product.builder()
				.name("Test Product")
				.description("Test Description")
				.price(10.0)
				.build());

		// When
		Optional<Product> foundProduct = productService.findById(product.getId());

		// Then
		assertTrue(foundProduct.isPresent());
		assertEquals(product.getId(), foundProduct.get().getId());
		assertEquals(product.getName(), foundProduct.get().getName());
	}

	@Test
	void shouldReturnEmptyWhenProductNotFound() {
		// When
		Optional<Product> notFoundProduct = productService.findById(999L);

		// Then
		assertTrue(notFoundProduct.isEmpty());
	}

	@Test
	void shouldFindAllProducts() {
		// Given
		productService.save(Product.builder().name("Product 1").price(10.0).build());
		productService.save(Product.builder().name("Product 2").price(20.0).build());

		// When
		List<Product> products = productService.findAll();

		// Then
		assertEquals(2, products.size());
	}

	@Test
	void shouldDeleteProduct() {
		// Given
		Product product = productService.save(Product.builder()
				.name("Test Product")
				.price(10.0)
				.build());

		// When
		productService.deleteById(product.getId());
		Optional<Product> deletedProduct = productService.findById(product.getId());

		// Then
		assertTrue(deletedProduct.isEmpty());
	}
}