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
  @DisplayName("startTime 속성이 있으면 경과 시간을 계산한다")
  void logDurationWithStartTime() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/courses");
    request.setAttribute("startTime", System.currentTimeMillis() - 50);

    LoggingUtils.logDuration(request, new MockHttpServletResponse(), null);

    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.get(0).getFormattedMessage()).doesNotContain("DURATION : -1ms");
  }
}
