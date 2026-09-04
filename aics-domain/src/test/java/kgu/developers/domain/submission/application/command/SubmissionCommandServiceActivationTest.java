package kgu.developers.domain.submission.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SubmissionCommandServiceActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SubmissionCommandService.class);

    @Test
    @DisplayName("S3를 사용하지 않는 애플리케이션에서는 제출 커맨드 서비스를 생성하지 않는다")
    void doesNotCreateSubmissionCommandServiceWhenS3IsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SubmissionCommandService.class);
        });
    }
}
