package com.two_m.yourbarber.controller;

import com.two_m.yourbarber.dto.product.ProductPostPutDTO;
import com.two_m.yourbarber.dto.product.ProductResponseDTO;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbershops/{shopId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @PathVariable Long shopId,
            @Valid @RequestBody ProductPostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(shopId, dto, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> listProducts(@PathVariable Long shopId) {
        return ResponseEntity.ok(productService.listProducts(shopId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long shopId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductPostPutDTO dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                productService.updateProduct(shopId, productId, dto, currentUser.getId()));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long shopId,
            @PathVariable Long productId,
            @AuthenticationPrincipal User currentUser) {
        productService.deleteProduct(shopId, productId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/availability")
    public ResponseEntity<ProductResponseDTO> toggleAvailability(
            @PathVariable Long shopId,
            @PathVariable Long productId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                productService.toggleAvailability(shopId, productId, currentUser.getId()));
    }
}
