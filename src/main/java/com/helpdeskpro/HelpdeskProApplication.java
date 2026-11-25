package com.helpdeskpro;

import com.helpdeskpro.config.DemoDatabaseInstaller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelpdeskProApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(HelpdeskProApplication.class);
		application.addListeners(new DemoDatabaseInstaller());
		application.run(args);
	}

}
