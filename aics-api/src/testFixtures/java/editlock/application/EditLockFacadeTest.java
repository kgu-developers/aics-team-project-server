package editlock.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kgu.developers.api.editlock.application.EditLockFacade;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import mock.repository.FakeEditLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EditLockFacadeTest {

    private static final String MEMBER = "202412345";
    private static final Long TARGET_ID = 1L;
    private static final String SECTION_KEY = "DEFAULT";

    private EditLockFacade facade;

    @BeforeEach
    public void init() {
        FakeEditLockRepository fakeEditLockRepository = new FakeEditLockRepository();
        facade = new EditLockFacade(
            new EditLockCommandService(fakeEditLockRepository),
            new EditLockQueryService(fakeEditLockRepository)
        );
    }

    private EditLockAcquireRequest buildRequest() {
        return EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(TARGET_ID)
            .sectionKey(SECTION_KEY)
            .build();
    }

    // PRESENTATION_CONTENT는 KD3-214에서 도메인째 삭제됐고, 남은 유일한 대상인 PROJECT(B2)는
    // 아직 도메인 검증 로직이 붙지 않아 전부 거부된다 — 즉 지금 EditLock은 검증 대상이 하나도
    // 없는 상태다(석민의 Project 잠금 연동이 들어오면 여기 케이스가 다시 채워져야 한다).
    @Test
    @DisplayName("PROJECT 대상은 아직 지원하지 않아 거부된다")
    public void acquire_RejectsUnsupportedProjectTarget() {
        assertThatThrownBy(() -> facade.acquire(MEMBER, buildRequest()))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("PROJECT 대상 조회도 아직 지원하지 않아 거부된다")
    public void getStatus_RejectsUnsupportedProjectTarget() {
        assertThatThrownBy(() -> facade.getStatus(EditLockTargetType.PROJECT, TARGET_ID, SECTION_KEY, MEMBER))
            .isInstanceOf(CustomException.class);
    }
}
