package com.selfhealing.auto_healer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.selfhealing.auto_healer.config.PrometheusProperties;

@SpringBootApplication
@EnableConfigurationProperties(PrometheusProperties.class)
public class AutoHealerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoHealerApplication.class, args);
	}

}
