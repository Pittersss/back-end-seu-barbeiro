package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.product.ProductPostPutDTO;
import com.two_m.yourbarber.dto.product.ProductResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.BarberShop;
import com.two_m.yourbarber.model.Product;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberShopRepository;
import com.two_m.yourbarber.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private BarberShopRepository barberShopRepository;

    @InjectMocks private ProductServiceImpl productService;

    private Barber owner(long id) {
        Barber barber =
                Barber.builder()
                        .name("Owner")
                        .email("owner@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    private BarberShop shop(long id, Barber owner) {
        BarberShop shop = BarberShop.builder().name("Shop").owner(owner).build();
        shop.setId(id);
        return shop;
    }

    private Product product(long id, BarberShop shop) {
        Product product =
                Product.builder()
                        .name("Pomade")
                        .price(BigDecimal.valueOf(20))
                        .available(true)
                        .barberShop(shop)
                        .build();
        product.setId(id);
        return product;
    }

    @Test
    void createProduct_owner_savesProduct() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductPostPutDTO dto =
                new ProductPostPutDTO("Pomade", "desc", BigDecimal.valueOf(20), null);
        ProductResponseDTO result = productService.createProduct(5L, dto, 1L);

        assertThat(result.getName()).isEqualTo("Pomade");
        assertThat(result.getBarberShopId()).isEqualTo(5L);
    }

    @Test
    void createProduct_notOwner_throwsForbidden() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));

        ProductPostPutDTO dto =
                new ProductPostPutDTO("Pomade", "desc", BigDecimal.valueOf(20), null);

        assertThrows(
                ForbiddenOperationException.class,
                () -> productService.createProduct(5L, dto, 2L));
    }

    @Test
    void listProducts_returnsShopProducts() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(productRepository.findByBarberShopId(5L)).thenReturn(List.of(product(30L, shop)));

        List<ProductResponseDTO> result = productService.listProducts(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(30L);
    }

    @Test
    void toggleAvailability_ownerAndProductInShop_flipsFlag() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        Product product = product(30L, shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO result = productService.toggleAvailability(5L, 30L, 1L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void toggleAvailability_productFromOtherShop_throwsNotFound() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        BarberShop otherShop = shop(6L, owner);
        Product product = product(30L, otherShop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.toggleAvailability(5L, 30L, 1L));
    }

    @Test
    void deleteProduct_owner_deletes() {
        Barber owner = owner(1L);
        BarberShop shop = shop(5L, owner);
        Product product = product(30L, shop);
        when(barberShopRepository.findById(5L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));

        productService.deleteProduct(5L, 30L, 1L);

        verify(productRepository).delete(product);
    }
}
