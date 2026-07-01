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
import org.example.eshopbackend.entity.Item;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.entity.OrderEntity;

import org.example.eshopbackend.mapper.*;

import org.example.eshopbackend.service.*;

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

    private final ItemMapper itemMapper;
    private final ItemService itemService;

    // --- CATEGORY ---

    @PostMapping("/categories")
    public CategoryResponseDTO createCategory(@Valid @RequestBody CreateCategoryRequestDTO dto) {
        Category saved = categoryService.addCategory(dto);
        return categoryMapper.toDto(saved);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponseDTO updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequestDTO dto){
        Category saved = categoryService.update(id, dto);
        return  categoryMapper.toDto(saved);
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
    @Deprecated
    public ProductResponseDTO createProduct(@Valid @RequestBody CreateProductRequestDTO dto) {
        Product saved = productService.addProduct(dto);
        return productMapper.toDto(saved);
    }

    @GetMapping("/products")
    @Deprecated
    public Page<ProductResponseDTO> listProducts(Pageable pageable) {
        Page<Product> page = productService.list(pageable);
        return page.map(productMapper::toDto);
    }

    @GetMapping("/products/{id}")
    @Deprecated
    public ProductResponseDTO getProduct(@PathVariable Long id) {
        return productMapper.toDto(productService.getById(id));
    }

    @PutMapping("/products/{id}")
    @Deprecated
    public ProductResponseDTO updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody UpdateProductRequestDTO dto) {
        Product updated = productService.update(id, dto);
        return productMapper.toDto(updated);
    }

    @DeleteMapping("/products/{id}")
    @Deprecated
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
    public ImageResponseDTO createProductImage(@PathVariable Long Id,
                                               @Valid @RequestBody CreateImageRequestDTO dto) {
        return imageService.addToItem(Id, dto);
    }

    @GetMapping("/products/{Id}/images")
    public List<ImageResponseDTO> listProductImages(@PathVariable Long Id) {
        return imageService.listByItem(Id);
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
// --- ITEMS

    @PostMapping("/items")
    public ItemResponseDTO createItem(@Valid @RequestBody CreateItemRequestDTO dto) {
        Item saved = itemService.create(dto);
        return itemMapper.toDto(saved);
    }

    @GetMapping("/items")
    public Page<ItemResponseDTO> listItems(Pageable pageable) {
        Page<Item> page = itemService.list(pageable);
        return page.map(itemMapper::toDto);
    }

    @GetMapping("/items/{id}")
    public ItemResponseDTO getItem(@PathVariable Long id) {
        return itemMapper.toDto(itemService.getById(id));
    }

    @PutMapping("/items/{id}")
    public ItemResponseDTO updateItem(@PathVariable Long id,
                                      @Valid @RequestBody UpdateItemRequestDTO dto) {
        Item updated = itemService.update(id, dto);
        return itemMapper.toDto(updated);
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemService.delete(id);
    }
}
