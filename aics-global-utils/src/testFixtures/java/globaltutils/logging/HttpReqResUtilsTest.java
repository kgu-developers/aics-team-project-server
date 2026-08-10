package globaltutils.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ForwardedHeaderFilter;

import jakarta.servlet.http.HttpServletRequest;
import kgu.developers.globalutils.logging.HttpReqResUtils;

class HttpReqResUtilsTest {

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  private MockHttpServletRequest bindRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    return request;
  }

  @Test
  @DisplayName("요청 컨텍스트가 없으면 0.0.0.0을 반환한다")
  void returnsPlaceholderWithoutRequest() {
    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("0.0.0.0");
  }

  @Test
  @DisplayName("연결한 상대의 주소를 반환한다")
  void returnsRemoteAddress() {
    bindRequest();

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("클라이언트가 보낸 프록시 헤더는 신뢰하지 않는다")
  void ignoresClientSuppliedForwardedHeader() {
    bindRequest().addHeader("X-Forwarded-For", "203.0.113.7");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("로그 위조를 노린 헤더 값도 그대로 무시된다")
  void ignoresLogForgingHeader() {
    bindRequest().addHeader("X-Forwarded-For", "1.1.1.1\n[REQUEST] CLIENT IP : 9.9.9.9");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("forward-headers-strategy로 프록시를 신뢰하도록 설정하면 실제 클라이언트 IP가 잡힌다")
  void trustsForwardedHeaderWhenProxyIsConfigured() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.7");

    new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
      RequestContextHolder.setRequestAttributes(new ServletRequestAttributes((HttpServletRequest)req));

      assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("203.0.113.7");
    });
  }
}
