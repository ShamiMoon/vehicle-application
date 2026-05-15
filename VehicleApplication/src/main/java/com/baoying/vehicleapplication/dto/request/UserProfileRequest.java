package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String realname;
    private String phone;
    private String email;
    private Integer emailNotify;
}
