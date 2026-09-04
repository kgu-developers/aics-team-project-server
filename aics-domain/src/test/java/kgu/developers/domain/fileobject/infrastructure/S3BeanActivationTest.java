package kgu.developers.domain.fileobject.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import kgu.developers.infra.config.S3Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3BeanActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(S3Config.class, S3FileStorage.class);

    @Test
    @DisplayName("S3를 사용하지 않는 애플리케이션에서는 S3 빈을 생성하지 않는다")
    void doesNotCreateS3BeansWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(S3Client.class);
            assertThat(context).doesNotHaveBean(S3Presigner.class);
            assertThat(context).doesNotHaveBean(S3FileStorage.class);
        });
    }

    @Test
    @DisplayName("S3가 활성화된 애플리케이션에서는 파일 저장 빈을 생성한다")
    void createsS3BeansWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "aws.s3.enabled=true",
                "aws.s3.region=ap-northeast-2",
                "aws.s3.bucket=test-bucket",
                "aws.s3.access-key=test-access-key",
                "aws.s3.secret-key=test-secret-key"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(S3Client.class);
                assertThat(context).hasSingleBean(S3Presigner.class);
                assertThat(context).hasSingleBean(S3FileStorage.class);
            });
    }
}
