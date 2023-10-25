package com.example.springbootvaultdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@SpringBootApplication
@Component
public class SpringBootVaultDemoApplication {

@Autowired
private VaultConfiguration config1 = null;

	private static final Logger logger = LoggerFactory.getLogger(SpringBootVaultDemoApplication.class.getName());

	public static void main(String[] args) {
		SpringApplication.run(SpringBootVaultDemoApplication.class, args);
	}

	@PostConstruct
	public void postConstruct() {
		logger.info("Reading properties from autowired config object inside SpringBootVaultDemo");
		logger.info("Min Renewal = "+config1.minRenewal);
		logger.info("Exp Threshold = "+config1.expiryThreshold);
		logger.info("isEnabled = "+config1.isEnabled);
	}
}
