package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.product.ProductPostPutDTO;
import com.two_m.yourbarber.dto.product.ProductResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.ProductMapper;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.Product;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BarberShopRepository barberShopRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public ProductResponseDTO createProduct(
            Long shopId, ProductPostPutDTO dto, Long requesterId) {
        subscriptionService.assertActive(requesterId);
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);

        Product product =
                Product.builder()
                        .name(dto.getName())
                        .description(dto.getDescription())
                        .price(dto.getPrice())
                        .image(dto.getImage())
                        .barberShop(shop)
                        .build();

        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public List<ProductResponseDTO> listProducts(Long shopId) {
        findShop(shopId);
        return productRepository.findByBarberShopId(shopId).stream()
                .map(ProductMapper::toDto)
                .toList();
    }

    @Override
    public ProductResponseDTO updateProduct(
            Long shopId, Long productId, ProductPostPutDTO dto, Long requesterId) {
        subscriptionService.assertActive(requesterId);
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        Product product = findProductInShop(shop, productId);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImage(dto.getImage());

        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long shopId, Long productId, Long requesterId) {
        subscriptionService.assertActive(requesterId);
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        Product product = findProductInShop(shop, productId);
        productRepository.delete(product);
    }

    @Override
    public ProductResponseDTO toggleAvailability(
            Long shopId, Long productId, Long requesterId) {
        subscriptionService.assertActive(requesterId);
        BarberShop shop = findShop(shopId);
        assertOwner(shop, requesterId);
        Product product = findProductInShop(shop, productId);

        product.setAvailable(!product.isAvailable());
        return ProductMapper.toDto(productRepository.save(product));
    }

    private Product findProductInShop(BarberShop shop, Long productId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Product not found: " + productId));
        if (product.getBarberShop() == null
                || !product.getBarberShop().getId().equals(shop.getId())) {
            throw new ResourceNotFoundException(
                    "Product " + productId + " does not belong to shop " + shop.getId());
        }
        return product;
    }

    private void assertOwner(BarberShop shop, Long requesterId) {
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(requesterId)) {
            throw new ForbiddenOperationException("Only the shop owner can perform this action");
        }
    }

    private BarberShop findShop(Long id) {
        return barberShopRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Barbershop not found: " + id));
    }
}
