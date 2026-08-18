package section.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.exception.ContactNotVisibleException;

class SectionContactVisibilityTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 3, 2, 0, 0);
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 6, 20, 18, 0);

    private Section section(LocalDateTime from, LocalDateTime until) {
        return Section.builder().id(1L).contactVisibleFrom(from).contactVisibleUntil(until).build();
    }

    @Test
    @DisplayName("공개기간 안이면 통과한다")
    void visibleWithinPeriod() {
        assertThatCode(() -> section(FROM, UNTIL).validateContactVisible(FROM.plusDays(1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공개 시작/종료 경계는 포함한다")
    void boundariesAreInclusive() {
        assertThatCode(() -> section(FROM, UNTIL).validateContactVisible(FROM)).doesNotThrowAnyException();
        assertThatCode(() -> section(FROM, UNTIL).validateContactVisible(UNTIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공개 시작 전이면 예외를 던진다")
    void beforePeriod() {
        assertThatThrownBy(() -> section(FROM, UNTIL).validateContactVisible(FROM.minusSeconds(1)))
                .isInstanceOf(ContactNotVisibleException.class);
    }

    @Test
    @DisplayName("공개 종료 후면 예외를 던진다")
    void afterPeriod() {
        assertThatThrownBy(() -> section(FROM, UNTIL).validateContactVisible(UNTIL.plusSeconds(1)))
                .isInstanceOf(ContactNotVisibleException.class);
    }

    @Test
    @DisplayName("공개 종료가 없으면 기한 없이 공개한다")
    void openEnded() {
        assertThatCode(() -> section(FROM, null).validateContactVisible(FROM.plusYears(5)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공개 시작이 없으면 아직 공개하지 않는다")
    void notOpenedYet() {
        assertThatThrownBy(() -> section(null, UNTIL).validateContactVisible(FROM))
                .isInstanceOf(ContactNotVisibleException.class);
    }
}
