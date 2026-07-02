package com.joprelys.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JoprelysBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JoprelysBackendApplication.class, args);
	}

}
