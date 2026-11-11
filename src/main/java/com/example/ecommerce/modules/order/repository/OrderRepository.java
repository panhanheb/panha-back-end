package com.example.ecommerce.modules.order.repository;

import com.example.ecommerce.modules.order.entity.Order;
import com.example.ecommerce.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "user"})
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select o from Order o
            where o.user.id = :userId
              and (:status is null or o.status = :status)
            """)
    Page<Order> findForUser(@Param("userId") Long userId, @Param("status") OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:userId is null or o.user.id = :userId)
              and (:keyword is null
                   or lower(o.orderNumber) like lower(concat('%', :keyword, '%'))
                   or lower(o.recipientName) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.username) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.email) like lower(concat('%', :keyword, '%')))
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("userId") Long userId,
                       @Param("keyword") String keyword, Pageable pageable);
}
