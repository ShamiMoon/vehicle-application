package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class RoleRequest {
    private Integer id;
    private String name;
    private String description;
    private Integer status;
}