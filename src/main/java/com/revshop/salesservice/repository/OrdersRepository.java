package com.revshop.salesservice.repository;

import com.revshop.salesservice.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserId(Long userId);

    Optional<Orders> findByOrderNumber(String orderNumber);

    List<Orders> findDistinctByOrderItems_SellerId(Long sellerId);

    boolean existsByUserIdAndOrderItems_ProductIdAndStatusIn(Long userId, Long productId, List<Orders.OrderStatus> statuses);
}
