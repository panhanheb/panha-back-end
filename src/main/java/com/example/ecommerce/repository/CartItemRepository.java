package com.example.ecommerce.repository;

import com.example.ecommerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Product ids in a user's cart, read without loading the products (so they can be locked first). */
    @Query("select i.product.id from CartItem i where i.cart.user.id = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    /** Removes a product from every cart (used when the product is deleted). */
    @Modifying
    @Query("delete from CartItem i where i.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}
