package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface IdentityClient {

    @GetMapping("/api/users/{userId}")
    ApiResponse<UserDTO> getUserById(@PathVariable("userId") Long userId);
}
