package kgu.developers.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import kgu.developers.infra.AicsConfigGroup;
import kgu.developers.infra.EnableAicsConfig;

@SpringBootApplication(scanBasePackages = "kgu.developers")
@EnableAicsConfig({ AicsConfigGroup.JPA, AicsConfigGroup.JPA_AUDITING, AicsConfigGroup.PROPERTIES })
public class AicsAdminApplication {

  public static void main(String[] args) {
    SpringApplication.run(AicsAdminApplication.class, args);
  }

}
