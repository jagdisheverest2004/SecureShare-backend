package org.example.secureshare;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecureShareApplication {

    public static void main(String[] args) {
        Dotenv.load();
        SpringApplication.run(SecureShareApplication.class, args);
    }

}
