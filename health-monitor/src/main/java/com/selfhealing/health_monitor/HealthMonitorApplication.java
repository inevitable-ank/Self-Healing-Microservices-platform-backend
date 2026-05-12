package com.selfhealing.health_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HealthMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthMonitorApplication.class, args);
	}

}
