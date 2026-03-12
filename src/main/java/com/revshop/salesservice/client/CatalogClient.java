package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/products/{productId}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId);
}
