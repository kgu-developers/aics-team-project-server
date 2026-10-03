package meetingrecord.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.meetingrecord.application.MeetingRecordEditLogFacade;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordEditLogQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MeetingRecordEditLogFacadeTest {

    private static final String MEMBER = "202412345";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @Mock
    private MeetingRecordEditLogQueryService meetingRecordEditLogQueryService;

    @Mock
    private TeamAccessValidator teamAccessValidator;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private MeetingRecordEditLogFacade meetingRecordEditLogFacade;

    @Test
    @DisplayName("팀원 또는 담당 교수면 수정 이력을 수정자 이름과 함께 조회한다")
    void getMeetingRecordLogs_Success() {
        given(meetingRecordQueryService.getMeetingRecord(7L)).willReturn(meetingRecord());
        given(meetingRecordEditLogQueryService.getMeetingRecordLogs(eq(7L), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(editLog(3L, MEMBER)), PAGEABLE, 1));
        given(userQueryService.getUsersByStudentNumbers(List.of(MEMBER)))
            .willReturn(List.of(user(MEMBER, "홍길동")));

        var response = meetingRecordEditLogFacade.getMeetingRecordLogs(7L, PAGEABLE, MEMBER);

        verify(teamAccessValidator).validateMembershipOrProfessor(10L, MEMBER);
        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.id()).isEqualTo(3L);
            assertThat(content.meetingRecordId()).isEqualTo(7L);
            assertThat(content.editorId()).isEqualTo(MEMBER);
            assertThat(content.editorName()).isEqualTo("홍길동");
            assertThat(content.reason()).isNotBlank();
            assertThat(content.createdAt()).isEqualTo("2026-10-04 15:30");
        });
        assertThat(response.pageable().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("조회는 최신순으로 고정되며 클라이언트 정렬은 적용하지 않는다")
    void getMeetingRecordLogs_FixedSort() {
        given(meetingRecordQueryService.getMeetingRecord(7L)).willReturn(meetingRecord());
        given(meetingRecordEditLogQueryService.getMeetingRecordLogs(eq(7L), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        meetingRecordEditLogFacade.getMeetingRecordLogs(
            7L, PageRequest.of(0, 20, Sort.by("reason")), MEMBER);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(meetingRecordEditLogQueryService).getMeetingRecordLogs(eq(7L), captor.capture());
        assertThat(captor.getValue().getSort())
            .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    @DisplayName("팀원도 담당 교수도 아니면 조회할 수 없다")
    void getMeetingRecordLogs_Forbidden() {
        given(meetingRecordQueryService.getMeetingRecord(7L)).willReturn(meetingRecord());
        willThrow(new AccessDeniedException("해당 팀에 소속된 사용자 또는 담당 교수만 접근할 수 있습니다."))
            .given(teamAccessValidator).validateMembershipOrProfessor(10L, "202400000");

        assertThatThrownBy(() -> meetingRecordEditLogFacade.getMeetingRecordLogs(7L, PAGEABLE, "202400000"))
            .isInstanceOf(AccessDeniedException.class);

        verify(meetingRecordEditLogQueryService, never()).getMeetingRecordLogs(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("이력이 없으면 수정자 조회 없이 빈 목록을 돌려준다")
    void getMeetingRecordLogs_Empty() {
        given(meetingRecordQueryService.getMeetingRecord(7L)).willReturn(meetingRecord());
        given(meetingRecordEditLogQueryService.getMeetingRecordLogs(eq(7L), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        var response = meetingRecordEditLogFacade.getMeetingRecordLogs(7L, PAGEABLE, MEMBER);

        assertThat(response.contents()).isEmpty();
        verify(userQueryService, never()).getUsersByStudentNumbers(anyList());
    }

    private MeetingRecord meetingRecord() {
        return MeetingRecord.builder()
            .id(7L)
            .teamId(10L)
            .title("3주차 정기 회의")
            .phase(MeetingPhase.MID_CHECK)
            .authorId(MEMBER)
            .meetingAt(LocalDateTime.of(2026, 9, 30, 14, 0))
            .content("내용")
            .participants(List.of())
            .build();
    }

    private MeetingRecordEditLog editLog(Long id, String editorId) {
        return MeetingRecordEditLog.builder()
            .id(id)
            .meetingRecordId(7L)
            .teamId(10L)
            .editorId(editorId)
            .reason("회의 내용 중 담당자 표기가 실제 논의와 달라 바로잡고 참석자 목록도 함께 고쳤습니다.")
            .createdAt(LocalDateTime.of(2026, 10, 4, 15, 30))
            .build();
    }

    private User user(String studentNumber, String name) {
        return User.builder()
            .studentNumber(studentNumber)
            .name(name)
            .build();
    }
}
