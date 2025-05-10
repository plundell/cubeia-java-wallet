package com.example.demoapi.service;

import com.example.demoapi.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {

	List<Product> findAll();

	Optional<Product> findById(Long id);

	Product save(Product product);

	void deleteById(Long id);
}