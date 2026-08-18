package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.product.ProductPostPutDTO;
import com.two_m.yourbarber.dto.product.ProductResponseDTO;
import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(Long shopId, ProductPostPutDTO dto, Long requesterId);

    List<ProductResponseDTO> listProducts(Long shopId);

    ProductResponseDTO updateProduct(
            Long shopId, Long productId, ProductPostPutDTO dto, Long requesterId);

    void deleteProduct(Long shopId, Long productId, Long requesterId);

    ProductResponseDTO toggleAvailability(Long shopId, Long productId, Long requesterId);
}
