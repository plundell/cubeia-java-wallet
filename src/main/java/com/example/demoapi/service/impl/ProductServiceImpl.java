package com.example.demoapi.service.impl;

import com.example.demoapi.model.Product;
import com.example.demoapi.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductServiceImpl implements ProductService {

	// Using ConcurrentHashMap for thread safety in a multi-instance environment
	private final Map<Long, Product> products = new ConcurrentHashMap<>();
	private final AtomicLong idGenerator = new AtomicLong(1);

	@Override
	public List<Product> findAll() {
		return new ArrayList<>(products.values());
	}

	@Override
	public Optional<Product> findById(Long id) {
		return Optional.ofNullable(products.get(id));
	}

	@Override
	public Product save(Product product) {
		if (product.getId() == null) {
			product.setId(idGenerator.getAndIncrement());
		}
		products.put(product.getId(), product);
		return product;
	}

	@Override
	public void deleteById(Long id) {
		products.remove(id);
	}
}