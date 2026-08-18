package com.two_m.yourbarber.mapper;

import com.two_m.yourbarber.dto.product.ProductResponseDTO;
import com.two_m.yourbarber.model.Product;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductResponseDTO toDto(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .available(product.isAvailable())
                .barberShopId(
                        product.getBarberShop() != null
                                ? product.getBarberShop().getId()
                                : null)
                .build();
    }
}
