package com.application.layered.service;

import com.application.layered.domain.Product;
import com.application.layered.dto.ProductRequest;
import com.application.layered.dto.ProductResponse;
import com.application.layered.exception.ProductNotFoundException;
import com.application.layered.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name(), request.price(), request.stock());
        return ProductResponse.from(repository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return repository.findById(id)
            .map(ProductResponse::from)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        return ProductResponse.from(product);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
