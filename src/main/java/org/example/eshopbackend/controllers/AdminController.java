// src/main/java/org/example/zeniqbackend/controllers/AdminController.java
package org.example.eshopbackend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.eshopbackend.dto.*;
import org.example.eshopbackend.dto.image.CreateImageRequestDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.dto.image.UpdateImageRequestDTO;
import org.example.eshopbackend.dto.shipment.OrderResponseDTO;
import org.example.eshopbackend.dto.shipment.UpdateOrderDTO;

import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.entity.OrderEntity;

import org.example.eshopbackend.mapper.CategoryMapper;
import org.example.eshopbackend.mapper.ImageMapper;
import org.example.eshopbackend.mapper.ProductMapper;
import org.example.eshopbackend.mapper.OrderMapper;

import org.example.eshopbackend.service.CategoryService;
import org.example.eshopbackend.service.ImageService;
import org.example.eshopbackend.service.ProductService;
import org.example.eshopbackend.service.OrderService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    private final ImageService imageService;
    private final ImageMapper imageMapper;

    // --- CATEGORY ---

    @PostMapping("/categories")
    public CategoryResponseDTO createCategory(@Valid @RequestBody CreateCategoryRequestDTO dto) {
        Category saved = categoryService.addCategory(dto);
        return categoryMapper.toDto(saved);
    }

    @GetMapping("/categories")
    public List<CategoryResponseDTO> listCategories() {
        return categoryService.listAll().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @GetMapping("/categories/{id}")
    public CategoryResponseDTO getCategory(@PathVariable Long id) {
        return categoryMapper.toDto(categoryService.getById(id));
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    // --- PRODUCTS ---

    @PostMapping("/products")
    public ProductResponseDTO createProduct(@Valid @RequestBody CreateProductRequestDTO dto) {
        Product saved = productService.addProduct(dto);
        return productMapper.toDto(saved);
    }

    @GetMapping("/products")
    public Page<ProductResponseDTO> listProducts(Pageable pageable) {
        Page<Product> page = productService.list(pageable);
        return page.map(productMapper::toDto);
    }

    @GetMapping("/products/{id}")
    public ProductResponseDTO getProduct(@PathVariable Long id) {
        return productMapper.toDto(productService.getById(id));
    }

    @PutMapping("/products/{id}")
    public ProductResponseDTO updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody UpdateProductRequestDTO dto) {
        Product updated = productService.update(id, dto);
        return productMapper.toDto(updated);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }

    // --- ORDERS

    @GetMapping("/orders")
    public Page<OrderResponseDTO> listOrders(Pageable pageable) {
        Page<OrderEntity> page = orderService.getAllOrders(pageable);
        return page.map(orderMapper::toDto);
    }

    @GetMapping("/orders/{id}")
    public OrderResponseDTO getOrder(@PathVariable Long id) {
        return orderMapper.toDto(orderService.getOrderById(id));
    }

    @PutMapping("/orders/{id}")
    public OrderResponseDTO updateOrder(@PathVariable Long id,
                                        @Valid @RequestBody UpdateOrderDTO dto) {
        OrderEntity updated = orderService.updateOrder(id, dto);
        return orderMapper.toDto(updated);
    }

    @DeleteMapping("/orders/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    @PostMapping("/products/{productId}/images")
    public ImageResponseDTO createProductImage(@PathVariable Long productId,
                                               @Valid @RequestBody CreateImageRequestDTO dto) {
        return imageService.addToProduct(productId, dto);
    }

    @GetMapping("/products/{productId}/images")
    public List<ImageResponseDTO> listProductImages(@PathVariable Long productId) {
        return imageService.listByProduct(productId);
    }

    @PutMapping("/images/{imageId}")
    public ImageResponseDTO updateImage(@PathVariable Long imageId,
                                        @Valid @RequestBody UpdateImageRequestDTO dto) {
        return imageService.update(imageId, dto);
    }

    @DeleteMapping("/images/{imageId}")
    public void deleteImage(@PathVariable Long imageId) {
        imageService.delete(imageId);
    }

    @PostMapping("/images/{imageId}/set-primary")
    public ImageResponseDTO setPrimaryImage(@PathVariable Long imageId) {
        return imageService.setPrimary(imageId);
    }
}
