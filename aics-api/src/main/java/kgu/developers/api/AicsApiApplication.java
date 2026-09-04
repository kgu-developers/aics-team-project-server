package kgu.developers.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import kgu.developers.infra.AicsConfigGroup;
import kgu.developers.infra.EnableAicsConfig;

@SpringBootApplication(scanBasePackages = "kgu.developers")
@EnableAicsConfig({ AicsConfigGroup.JPA, AicsConfigGroup.JPA_AUDITING, AicsConfigGroup.PROPERTIES, AicsConfigGroup.S3 })
@EnableScheduling
public class AicsApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(AicsApiApplication.class, args);
	}
}