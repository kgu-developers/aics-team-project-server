package common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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

    @PostMapping("/body")
    void body(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/param/{id}")
    void param(@Positive @PathVariable Long id) {
    }

    @GetMapping("/conflict")
    void conflict() {
      throw new DataIntegrityViolationException(
          "ERROR: duplicate key value violates unique constraint \"user_pkey\"");
    }
  }

  record TestRequest(@NotBlank String name) {
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

  @Test
  @DisplayName("본문 검증 실패는 400과 어긋난 필드를 알려준다")
  void handlesInvalidBody() throws Exception {
    mockMvc.perform(post("/body")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
  }

  @Test
  @DisplayName("경로 변수 검증 실패도 400과 같은 형식으로 응답한다")
  void handlesInvalidPathVariable() throws Exception {
    mockMvc.perform(get("/param/-5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("DB 제약 위반은 500이 아니라 409로 응답하고 제약 이름을 노출하지 않는다")
  void handlesDataIntegrityViolation() throws Exception {
    mockMvc.perform(get("/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
        .andExpect(jsonPath("$.message").value("요청이 기존 데이터와 충돌합니다."))
        .andExpect(jsonPath("$.message").value(not(containsString("user_pkey"))));
  }
}
