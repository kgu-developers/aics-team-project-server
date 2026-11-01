package editlock.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EditLockAcquireRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean isValid(EditLockTargetType targetType, String sectionKey) {
        return validator.validate(new EditLockAcquireRequest(targetType, 1L, sectionKey)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOPIC", "DATA", "SCREEN", "TEAM_OPERATION"})
    @DisplayName("제안서 잠금은 제안서 섹션 이름을 키로 받는다")
    void acceptsProposalSectionNames(String sectionKey) {
        assertThat(isValid(EditLockTargetType.PROJECT, sectionKey)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEAM_INFO", "screen", "DEFAULT"})
    @DisplayName("제안서 잠금은 섹션 이름이 아닌 키를 거부한다")
    void rejectsOtherKeysForProject(String sectionKey) {
        assertThat(isValid(EditLockTargetType.PROJECT, sectionKey)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEAM_INFO", "DEFAULT"})
    @DisplayName("제안서가 아닌 대상은 자유로운 키를 그대로 받는다")
    void keepsFreeKeysForOtherTargets(String sectionKey) {
        assertThat(isValid(EditLockTargetType.MEETING_RECORD, sectionKey)).isTrue();
    }
}
