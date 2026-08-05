package common.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import kgu.developers.common.exception.CustomException;
import kgu.developers.common.exception.ExceptionCode;
import kgu.developers.common.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

  enum TestExceptionCode implements ExceptionCode {
    CLIENT_FAULT(BAD_REQUEST, "잘못된 요청입니다."),
    SERVER_FAULT(INTERNAL_SERVER_ERROR, "서버 오류입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    TestExceptionCode(HttpStatus status, String message) {
      this.status = status;
      this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
      return status;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getCode() {
      return this.name();
    }
  }

  @RestController
  static class TestController {
    @GetMapping("/client-fault")
    void clientFault() {
      throw new CustomException(TestExceptionCode.CLIENT_FAULT);
    }

    @GetMapping("/server-fault")
    void serverFault() {
      throw new CustomException(TestExceptionCode.SERVER_FAULT, new IllegalStateException("원인"));
    }
  }

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("CustomException은 ExceptionCode의 상태와 code/message로 응답한다")
  void handlesClientFault() throws Exception {
    mockMvc.perform(get("/client-fault"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CLIENT_FAULT"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
  }

  @Test
  @DisplayName("서버 오류 코드는 500으로 응답한다")
  void handlesServerFault() throws Exception {
    mockMvc.perform(get("/server-fault"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("SERVER_FAULT"))
        .andExpect(jsonPath("$.message").value("서버 오류입니다."));
  }
}
