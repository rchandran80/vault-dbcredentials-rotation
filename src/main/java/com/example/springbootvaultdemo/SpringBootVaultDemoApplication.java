package com.example.springbootvaultdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
public class SpringBootVaultDemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringBootVaultDemoApplication.class, args);
	}
}
