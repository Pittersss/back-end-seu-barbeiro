package com.two_m.yourbarber.repository;

import com.two_m.yourbarber.model.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByBarberShopId(Long barberShopId);
}
