package meetingrecord.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.infrastructure.JpaMeetingRecordRepository;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingActionRepositoryImpl;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordJpaEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// 분반 단위 액션플랜 조회는 meeting_action ⨝ meeting_record 조인 + 선택 필터(null 허용) +
// 런타임 정렬로 돌아가는데, Fake 리포지토리로는 실제 JPQL과 정렬 별칭이 맞는지 검증할 수 없다.
// 그래서 H2에 실제 쿼리를 태워 필터·정렬·페이징을 확인한다.
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:meeting-action-section;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(MeetingActionRepositoryImpl.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeetingActionSectionQueryIntegrationTest {

    private static final Pageable LATEST_FIRST =
        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.meetingrecord.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.meetingrecord.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private MeetingActionRepositoryImpl repository;

    @Autowired
    private JpaMeetingRecordRepository jpaMeetingRecordRepository;

    @Test
    @DisplayName("담당 분반 팀 목록에 속한 액션플랜만 조회하고 다른 팀 것은 제외한다")
    void findAllByTeamIdIn_FiltersByTeamIds() {
        Long ownedRecordId = saveMeetingRecord(10L);
        Long otherRecordId = saveMeetingRecord(99L);
        MeetingAction owned = saveAction(ownedRecordId, MeetingActionStatus.TODO);
        saveAction(otherRecordId, MeetingActionStatus.TODO);

        Page<MeetingAction> result = repository.findAllByTeamIdIn(List.of(10L, 20L), null, null, LATEST_FIRST);

        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent())
            .extracting(MeetingAction::getId)
            .containsExactly(owned.getId());
    }

    @Test
    @DisplayName("회의록 필터와 상태 필터를 함께 적용한다")
    void findAllByTeamIdIn_FiltersByMeetingRecordAndStatus() {
        Long targetRecordId = saveMeetingRecord(10L);
        Long otherRecordId = saveMeetingRecord(10L);
        MeetingAction done = saveAction(targetRecordId, MeetingActionStatus.DONE);
        saveAction(targetRecordId, MeetingActionStatus.TODO);
        saveAction(otherRecordId, MeetingActionStatus.DONE);

        Page<MeetingAction> result = repository.findAllByTeamIdIn(
            List.of(10L), targetRecordId, MeetingActionStatus.DONE, LATEST_FIRST);

        Assertions.assertThat(result.getContent())
            .extracting(MeetingAction::getId)
            .containsExactly(done.getId());
    }

    @Test
    @DisplayName("정렬은 생성 최신순으로 적용되어 나중에 등록된 액션플랜이 먼저 나온다")
    void findAllByTeamIdIn_SortsLatestFirst() {
        Long recordId = saveMeetingRecord(10L);
        MeetingAction first = saveAction(recordId, MeetingActionStatus.TODO);
        MeetingAction second = saveAction(recordId, MeetingActionStatus.TODO);
        MeetingAction third = saveAction(recordId, MeetingActionStatus.TODO);

        Page<MeetingAction> result = repository.findAllByTeamIdIn(List.of(10L), null, null, LATEST_FIRST);

        Assertions.assertThat(result.getContent())
            .extracting(MeetingAction::getId)
            .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    @DisplayName("페이지 크기를 넘으면 총 건수를 유지한 채 나눠서 반환한다")
    void findAllByTeamIdIn_Paginates() {
        Long recordId = saveMeetingRecord(10L);
        saveAction(recordId, MeetingActionStatus.TODO);
        MeetingAction newer = saveAction(recordId, MeetingActionStatus.TODO);

        Page<MeetingAction> result = repository.findAllByTeamIdIn(
            List.of(10L), null, null,
            PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        Assertions.assertThat(result.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(result.isLast()).isFalse();
        Assertions.assertThat(result.getContent())
            .extracting(MeetingAction::getId)
            .containsExactly(newer.getId());
    }

    @Test
    @DisplayName("팀 목록이 비어 있으면 쿼리 없이 빈 페이지를 반환한다")
    void findAllByTeamIdIn_EmptyTeamIds() {
        Long recordId = saveMeetingRecord(10L);
        saveAction(recordId, MeetingActionStatus.TODO);

        Page<MeetingAction> result = repository.findAllByTeamIdIn(List.of(), null, null, LATEST_FIRST);

        Assertions.assertThat(result.getTotalElements()).isZero();
    }

    private Long saveMeetingRecord(Long teamId) {
        return jpaMeetingRecordRepository.saveAndFlush(MeetingRecordJpaEntity.builder()
            .teamId(teamId)
            .title("3주차 정기 회의")
            .phase(MeetingPhase.MID_CHECK)
            .authorId("202612345")
            .meetingAt(LocalDateTime.of(2026, 9, 30, 14, 0))
            .location("온라인")
            .content("회의 내용")
            .build()).getId();
    }

    private MeetingAction saveAction(Long meetingRecordId, MeetingActionStatus status) {
        return repository.save(MeetingAction.create(
            meetingRecordId, "202412345", "로그인 API 연동", status, LocalDateTime.of(2026, 10, 7, 23, 59)));
    }
}
