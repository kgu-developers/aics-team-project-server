package globaltutils.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import kgu.developers.globalutils.annotation.NoLogging;
import kgu.developers.globalutils.logging.LoggingUtils;

class LoggingUtilsTest {

  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    appender = new ListAppender<>();
    appender.start();
    ((Logger)LoggerFactory.getLogger(LoggingUtils.class)).addAppender(appender);
  }

  @Test
  @DisplayName("startTime 속성이 없으면 예외 없이 -1ms로 기록한다")
  void logDurationWithoutStartTime() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/courses");

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), null);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage()).contains("DURATION : -1ms");
  }

  @Test
  @DisplayName("startTime 속성이 없어도 예외 로깅 경로가 동작한다")
  void logDurationWithoutStartTimeOnExceptionPath() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/courses");

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), new IllegalStateException("boom"));

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage())
      .contains("DURATION : -1ms")
      .contains("EXCEPTION : boom");
  }

  @Test
  @DisplayName("이름이 민감한 필드는 값 대신 마스킹한다")
  void getObjectFieldsMasksSensitiveFieldsByName() {
    String result = LoggingUtils.getObjectFields(new LoginRequest("hong", "s3cret", "eyJhbGci"));

    assertThat(result).contains("loginId = hong");
    assertThat(result).contains("password = ****");
    assertThat(result).contains("accessToken = ****");
    assertThat(result).doesNotContain("s3cret");
    assertThat(result).doesNotContain("eyJhbGci");
  }

  @Test
  @DisplayName("@NoLogging이 붙은 필드는 이름과 무관하게 마스킹한다")
  void getObjectFieldsMasksAnnotatedFields() {
    String result = LoggingUtils.getObjectFields(new MemberRequest("hong", "010-1234-5678"));

    assertThat(result).contains("name = hong");
    assertThat(result).contains("phoneNumber = ****");
    assertThat(result).doesNotContain("010-1234-5678");
  }

  @Test
  @DisplayName("startTime 속성이 있으면 경과 시간을 계산한다")
  void logDurationWithStartTime() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/courses");
    request.setAttribute("startTime", System.currentTimeMillis() - 50);

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), null);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("DURATION : -1ms");
  }

  private record LoginRequest(String loginId, String password, String accessToken) {
  }

  private record MemberRequest(String name, @NoLogging String phoneNumber) {
  }
}
