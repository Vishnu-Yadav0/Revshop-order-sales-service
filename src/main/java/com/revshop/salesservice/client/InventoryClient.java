package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.InventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}")
    InventoryDTO getInventoryByProductId(@PathVariable("productId") Long productId);

    @PutMapping("/api/inventory/reserve")
    void reserveStock(@RequestParam("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
