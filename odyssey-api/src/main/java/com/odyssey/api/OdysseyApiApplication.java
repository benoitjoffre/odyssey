package com.odyssey.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OdysseyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OdysseyApiApplication.class, args);
	}

}
