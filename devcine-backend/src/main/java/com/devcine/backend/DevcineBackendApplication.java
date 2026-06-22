package com.devcine.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // bật @Scheduled cho job dọn ghế giữ quá hạn
public class DevcineBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevcineBackendApplication.class, args);
    }

}
