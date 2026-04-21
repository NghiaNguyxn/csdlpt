package com.example.csdlpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CsdlptApplication {

	public static void main(String[] args) {
		SpringApplication.run(CsdlptApplication.class, args);
	}

}
