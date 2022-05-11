package com.addy360.jasbrob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JasbrobApplication {

	public static void main(String[] args) {
		SpringApplication.run(JasbrobApplication.class, args);
	}

}
