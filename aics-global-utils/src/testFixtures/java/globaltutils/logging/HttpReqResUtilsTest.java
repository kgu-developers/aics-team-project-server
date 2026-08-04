package globaltutils.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
  @DisplayName("프록시 헤더의 첫 번째 주소가 유효하면 그 값을 반환한다")
  void returnsFirstValidForwardedAddress() {
    bindRequest().addHeader("X-Forwarded-For", "203.0.113.7, 70.41.3.18");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("203.0.113.7");
  }

  @Test
  @DisplayName("IP 형식이 아닌 프록시 헤더는 무시하고 remoteAddr로 폴백한다")
  void ignoresNonIpForwardedValue() {
    bindRequest().addHeader("X-Forwarded-For", "1.1.1.1 || USER ID : admin");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("개행이 섞인 헤더 값은 로그 위조를 막기 위해 무시한다")
  void ignoresNewlineInjectedValue() {
    bindRequest().addHeader("X-Forwarded-For", "1.1.1.1\n[REQUEST] CLIENT IP : 9.9.9.9");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("10.0.0.1");
  }

  @Test
  @DisplayName("앞선 헤더가 유효하지 않으면 다음 후보 헤더를 계속 확인한다")
  void continuesToNextCandidateHeader() {
    MockHttpServletRequest request = bindRequest();
    request.addHeader("X-Forwarded-For", "not-an-ip");
    request.addHeader("HTTP_CLIENT_IP", "198.51.100.23");

    assertThat(HttpReqResUtils.getClientIpAddressIfServletRequestExist()).isEqualTo("198.51.100.23");
  }
}
