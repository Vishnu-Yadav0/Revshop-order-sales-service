package com.revshop.salesservice.dto;

import lombok.Data;

@Data
public class ShipperDTO {
    private Long shipperId;
    private String name;
    private String phone;
    private String email;
    private String vehicleNumber;
}
