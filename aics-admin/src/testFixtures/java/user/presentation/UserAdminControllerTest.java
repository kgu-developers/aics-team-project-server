package user.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.user.application.UserAdminFacade;
import kgu.developers.admin.user.presentation.UserAdminControllerImpl;
import kgu.developers.admin.user.presentation.request.UserAdminRequest;
import kgu.developers.admin.user.presentation.request.UserAdminUpdateRequest;
import kgu.developers.admin.user.presentation.response.UserAdminListResponse;
import kgu.developers.admin.user.presentation.response.UserAdminPersistResponse;
import kgu.developers.admin.user.presentation.response.UserAdminResponse;
import static kgu.developers.domain.user.domain.UserGlobalRole.STUDENT;

import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

  private static final String BASE_URL = "/api/v1/admin/oop/users";
  private static final String STUDENT_NUMBER = "202699999";

  @Mock
  private UserAdminFacade userAdminFacade;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UserAdminRequest request =
      new UserAdminRequest(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT, "010-1234-6789");

  private final UserAdminUpdateRequest updateRequest =
      new UserAdminUpdateRequest("new@kyonggi.ac.kr", "김영희", "87654321", STUDENT, "010-9876-5432");

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UserAdminControllerImpl(userAdminFacade)).build();
  }

  private User user() {
    return User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT, "010-1234-6789");
  }

  @Test
  @DisplayName("POST /users는 201과 생성된 학번을 응답한다")
  void createUser() throws Exception {
    given(userAdminFacade.createUser(request)).willReturn(UserAdminPersistResponse.of(STUDENT_NUMBER));

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.studentNumber").value(STUDENT_NUMBER));
  }

  @Test
  @DisplayName("POST /users는 필수 값이 빠지면 400을 응답한다")
  void createUserWithMissingField() throws Exception {
    UserAdminRequest invalid =
        new UserAdminRequest(null, "kgu@kyonggi.ac.kr", "김철수", "12345678", STUDENT, "010-1234-6789");

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /users는 값이 빈 문자열이면 400을 응답한다")
  void createUserWithBlankField() throws Exception {
    UserAdminRequest blank =
        new UserAdminRequest("", "", "", "", STUDENT, "");

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(blank)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /users/{studentNumber}는 200과 유저를 응답한다")
  void getUserByStudentNumber() throws Exception {
    given(userAdminFacade.getUserByStudentNumber(STUDENT_NUMBER)).willReturn(UserAdminResponse.from(user()));

    mockMvc.perform(get(BASE_URL + "/" + STUDENT_NUMBER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentNumber").value(STUDENT_NUMBER))
        .andExpect(jsonPath("$.name").value("김철수"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  @DisplayName("GET /users는 200과 목록을 응답한다")
  void getUsers() throws Exception {
    given(userAdminFacade.getAllUsers()).willReturn(UserAdminListResponse.from(List.of(user())));

    mockMvc.perform(get(BASE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contents.length()").value(1))
        .andExpect(jsonPath("$.contents[0].password").doesNotExist());
  }

  @Test
  @DisplayName("PUT /users/{studentNumber}는 204를 응답하고 파사드에 위임한다")
  void updateUser() throws Exception {
    mockMvc.perform(put(BASE_URL + "/" + STUDENT_NUMBER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNoContent());

    verify(userAdminFacade).updateUser(STUDENT_NUMBER, updateRequest);
  }

  @Test
  @DisplayName("DELETE /users/{studentNumber}는 204를 응답하고 파사드에 위임한다")
  void deleteUser() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/" + STUDENT_NUMBER))
        .andExpect(status().isNoContent());

    verify(userAdminFacade).deleteUser(STUDENT_NUMBER);
  }
}
