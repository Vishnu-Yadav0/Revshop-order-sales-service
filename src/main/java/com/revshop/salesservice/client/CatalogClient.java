package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-catalog-service", url = "${product-catalog-service.url:http://localhost:8082}")
public interface CatalogClient {

    @GetMapping("/api/products/{productId}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("productId") Long productId);
}
