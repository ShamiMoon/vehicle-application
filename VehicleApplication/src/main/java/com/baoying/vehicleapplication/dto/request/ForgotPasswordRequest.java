package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String username;
    private String realname;
    private String phone;
    private String email;
}
