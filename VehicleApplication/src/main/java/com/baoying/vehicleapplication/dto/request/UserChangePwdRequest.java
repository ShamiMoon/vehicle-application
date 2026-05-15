package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class UserChangePwdRequest {
    private Long id;
    private String oldPassword;
    private String newPassword;
}