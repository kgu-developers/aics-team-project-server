package kgu.developers.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import kgu.developers.infra.AicsConfigGroup;
import kgu.developers.infra.EnableAicsConfig;

@SpringBootApplication(scanBasePackages = "kgu.developers")
@EnableAicsConfig({ AicsConfigGroup.JPA, AicsConfigGroup.JPA_AUDITING, AicsConfigGroup.PROPERTIES })
public class AicsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AicsAuthApplication.class, args);
	}

}
