package com.baoying.vehicleapplication;

import com.baoying.vehicleapplication.utils.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VehicleApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void passwordTest() {
        String rawPassword = "123456";
        String encodedPassword = PasswordUtils.encode(rawPassword);
        System.out.println(encodedPassword);
        String encodePassword = "a7cb037f650325b6bb6d1d8137a68c46";
        String decodedPassword = PasswordUtils.decode(encodePassword);
        System.out.println(decodedPassword);
    }
}
