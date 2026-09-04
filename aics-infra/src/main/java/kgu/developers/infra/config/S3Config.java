package kgu.developers.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import kgu.developers.infra.AicsConfig;

@Configuration
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3Config implements AicsConfig {
	@Value("${aws.s3.region}")
	String region;

	@Value("${aws.s3.access-key:}")
	String accessKey;

	@Value("${aws.s3.secret-key:}")
	String secretKey;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	// 로컬/CI에서는 자격증명이 아예 없을 수 있으니 명시적으로 준 것만 쓰고,
	// 없으면 EC2 인스턴스 프로필 등 기본 체인(DefaultCredentialsProvider)에 맡긴다.
	private AwsCredentialsProvider credentialsProvider() {
		if (accessKey.isBlank() || secretKey.isBlank()) {
			return DefaultCredentialsProvider.create();
		}
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
	}
}
