package common.exception;

import static kgu.developers.common.exception.ConstraintViolations.violates;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ConstraintViolationsTest {

  @Test
  @DisplayName("가장 구체적인 원인 메시지에 인덱스명이 있으면 true, 없거나 메시지가 없으면 false")
  void violates_MatchesIndexNameInMostSpecificCause() {
    DataIntegrityViolationException matching =
        new DataIntegrityViolationException("wrapper", new RuntimeException("duplicate key uk_team_section_name"));
    DataIntegrityViolationException other =
        new DataIntegrityViolationException("wrapper", new RuntimeException("duplicate key uk_other"));
    DataIntegrityViolationException noMessage =
        new DataIntegrityViolationException("wrapper", new RuntimeException());

    assertThat(violates(matching, "uk_team_section_name")).isTrue();
    assertThat(violates(other, "uk_team_section_name")).isFalse();
    assertThat(violates(noMessage, "uk_team_section_name")).isFalse();
  }
}
