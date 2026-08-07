package globaltutils.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
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
    logger().addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger().detachAppender(appender);
    appender.stop();
  }

  private Logger logger() {
    return (Logger)LoggerFactory.getLogger(LoggingUtils.class);
  }

  @Test
  @DisplayName("startTime 속성이 없으면 예외 없이 -1ms로 기록한다")
  void logDurationWithoutStartTime() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), null);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage()).contains("DURATION : -1ms");
  }

  @Test
  @DisplayName("startTime 속성이 없어도 예외 로깅 경로가 동작한다")
  void logDurationWithoutStartTimeOnExceptionPath() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");

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
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
    request.setAttribute("startTime", System.currentTimeMillis() - 50);

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), null);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("DURATION : -1ms");
  }

  @Test
  @DisplayName("값에 섞인 개행은 이스케이프해 로그 한 줄을 위조하지 못하게 한다")
  void getObjectFieldsEscapesLineBreaks() {
    String result = LoggingUtils.getObjectFields(
        new LoginRequest("hong\r\n[REQUEST] CLIENT IP : 9.9.9.9", "s3cret", "eyJhbGci"));

    assertThat(result).contains("loginId = hong\\r\\n[REQUEST] CLIENT IP : 9.9.9.9");
    assertThat(result).doesNotContain("\n");
    assertThat(result).doesNotContain("\r");
  }

  @Test
  @DisplayName("필드 값의 toString이 터져도 로깅은 요청을 깨뜨리지 않는다")
  void getObjectFieldsSurvivesFailingToString() {
    String result = LoggingUtils.getObjectFields(new Holder());

    assertThat(result).contains("value = ACCESS_DENIED");
  }

  @Test
  @DisplayName("접근이 막힌 JDK 내부 필드도 예외를 전파하지 않는다")
  void getObjectFieldsSurvivesInaccessibleField() {
    assertThatCode(() -> LoggingUtils.getObjectFields("hello")).doesNotThrowAnyException();
  }

  private record LoginRequest(String loginId, String password, String accessToken) {
  }

  private static class Exploding {
    @Override
    public String toString() {
      throw new IllegalStateException("boom");
    }
  }

  private static class Holder {
    private final Exploding value = new Exploding();
  }

  private record MemberRequest(String name, @NoLogging String phoneNumber) {
  }
}
