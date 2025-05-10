package com.example.demoapi.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Objects;

public class Product {

	private Long id;

	@NotBlank(message = "Name is required")
	private String name;

	private String description;

	@NotNull(message = "Price is required")
	@Positive(message = "Price must be positive")
	private Double price;

	// Default constructor
	public Product() {
	}

	// All-args constructor
	public Product(Long id, String name, String description, Double price) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
	}

	// Builder pattern method
	public static ProductBuilder builder() {
		return new ProductBuilder();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	// equals and hashCode
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Product product = (Product) o;
		return Objects.equals(id, product.id) &&
				Objects.equals(name, product.name) &&
				Objects.equals(description, product.description) &&
				Objects.equals(price, product.price);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, price);
	}

	// toString
	@Override
	public String toString() {
		return "Product{" +
				"id=" + id +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", price=" + price +
				'}';
	}

	// Builder class
	public static class ProductBuilder {
		private Long id;
		private String name;
		private String description;
		private Double price;

		ProductBuilder() {
		}

		public ProductBuilder id(Long id) {
			this.id = id;
			return this;
		}

		public ProductBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ProductBuilder description(String description) {
			this.description = description;
			return this;
		}

		public ProductBuilder price(Double price) {
			this.price = price;
			return this;
		}

		public Product build() {
			return new Product(id, name, description, price);
		}
	}
}