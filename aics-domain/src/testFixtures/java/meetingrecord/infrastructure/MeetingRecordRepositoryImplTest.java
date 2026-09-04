package meetingrecord.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.infrastructure.JpaMeetingActionRepository;
import kgu.developers.domain.meetingrecord.infrastructure.JpaMeetingParticipantRepository;
import kgu.developers.domain.meetingrecord.infrastructure.JpaMeetingRecordRepository;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingParticipantJpaEntity;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordJpaEntity;
import kgu.developers.domain.meetingrecord.infrastructure.MeetingRecordRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class MeetingRecordRepositoryImplTest {

  @Mock
  private JpaMeetingRecordRepository jpaMeetingRecordRepository;

  @Mock
  private JpaMeetingParticipantRepository jpaMeetingParticipantRepository;

  @Mock
  private JpaMeetingActionRepository jpaMeetingActionRepository;

  @InjectMocks
  private MeetingRecordRepositoryImpl meetingRecordRepositoryImpl;

  private MeetingRecordJpaEntity savedEntity() {
    return MeetingRecordJpaEntity.builder()
        .id(1L)
        .teamId(10L)
        .phase(MeetingPhase.PROPOSAL)
        .authorId("202412345")
        .meetingAt(LocalDateTime.now())
        .content("내용")
        .build();
  }

  private MeetingParticipantJpaEntity participantEntity(Long id, String userId) {
    return MeetingParticipantJpaEntity.builder()
        .id(id)
        .meetingRecordId(1L)
        .userId(userId)
        .build();
  }

  private MeetingRecord meetingRecordWithParticipants(List<String> userIds) {
    return MeetingRecord.builder()
        .id(1L)
        .teamId(10L)
        .phase(MeetingPhase.PROPOSAL)
        .authorId("202412345")
        .content("내용")
        .participants(userIds.stream()
            .map(userId -> MeetingParticipant.builder().meetingRecordId(1L).userId(userId).build())
            .toList())
        .build();
  }

  @Test
  @DisplayName("save는 참가자 목록이 그대로면 기존 참가자를 지우거나 다시 만들지 않는다")
  public void save_UnchangedParticipants_DoesNotTouchExistingRows() {
    // given
    given(jpaMeetingRecordRepository.save(any())).willReturn(savedEntity());
    given(jpaMeetingParticipantRepository.findAllByMeetingRecordId(1L))
        .willReturn(List.of(participantEntity(100L, "202412345"), participantEntity(101L, "202412346")));

    MeetingRecord meetingRecord = meetingRecordWithParticipants(List.of("202412345", "202412346"));

    // when
    meetingRecordRepositoryImpl.save(meetingRecord);

    // then
    verify(jpaMeetingParticipantRepository, never()).deleteAllById(anyList());
    verify(jpaMeetingParticipantRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("save는 빠진 참가자만 지우고 새로 추가된 참가자만 저장한다")
  public void save_ChangedParticipants_OnlyTouchesDifference() {
    // given
    given(jpaMeetingRecordRepository.save(any())).willReturn(savedEntity());
    given(jpaMeetingParticipantRepository.findAllByMeetingRecordId(1L))
        .willReturn(List.of(participantEntity(100L, "202412345"), participantEntity(101L, "202412346")));

    MeetingRecord meetingRecord = meetingRecordWithParticipants(List.of("202412345", "202499999"));

    // when
    meetingRecordRepositoryImpl.save(meetingRecord);

    // then
    verify(jpaMeetingParticipantRepository).deleteAllById(List.of(101L));
    verify(jpaMeetingParticipantRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("deleteById는 참가자와 액션플랜을 함께 삭제한다")
  public void deleteById_DeletesParticipantsAndActionsTogether() {
    meetingRecordRepositoryImpl.deleteById(1L);

    verify(jpaMeetingParticipantRepository).deleteAllByMeetingRecordId(1L);
    verify(jpaMeetingActionRepository).deleteAllByMeetingRecordId(1L);
    verify(jpaMeetingRecordRepository).deleteById(1L);
  }

  @Test
  @DisplayName("save는 기존 회의록을 수정할 때 참가자 동기화 전에 행 잠금을 먼저 건다")
  public void save_ExistingRecord_LocksRowBeforeSyncingParticipants() {
    // given
    given(jpaMeetingRecordRepository.save(any())).willReturn(savedEntity());
    given(jpaMeetingParticipantRepository.findAllByMeetingRecordId(1L)).willReturn(List.of());

    MeetingRecord meetingRecord = meetingRecordWithParticipants(List.of());

    // when
    meetingRecordRepositoryImpl.save(meetingRecord);

    // then
    verify(jpaMeetingRecordRepository).findByIdForUpdate(1L);
  }

  @Test
  @DisplayName("save는 새 회의록을 생성할 때는 행 잠금을 걸지 않는다")
  public void save_NewRecord_DoesNotLockRow() {
    // given
    given(jpaMeetingRecordRepository.save(any())).willReturn(savedEntity());
    given(jpaMeetingParticipantRepository.findAllByMeetingRecordId(1L)).willReturn(List.of());

    MeetingRecord meetingRecord = MeetingRecord.builder()
        .teamId(10L)
        .phase(MeetingPhase.PROPOSAL)
        .authorId("202412345")
        .content("내용")
        .participants(List.of())
        .build();

    // when
    meetingRecordRepositoryImpl.save(meetingRecord);

    // then
    verify(jpaMeetingRecordRepository, never()).findByIdForUpdate(any());
  }

  @Test
  @DisplayName("findAllByTeamIdIn은 페이지의 회의록에 참가자 목록을 함께 조립한다")
  public void findAllByTeamIdIn_MapsParticipants() {
    // given
    var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "meetingAt"));
    MeetingRecordJpaEntity entity = savedEntity();
    given(jpaMeetingRecordRepository.findAllByTeamIdIn(List.of(10L), pageable))
        .willReturn(new PageImpl<>(List.of(entity), pageable, 1));
    given(jpaMeetingParticipantRepository.findAllByMeetingRecordIdIn(List.of(1L)))
        .willReturn(List.of(participantEntity(100L, "202412345")));

    // when
    var result = meetingRecordRepositoryImpl.findAllByTeamIdIn(List.of(10L), pageable);

    // then
    assertThat(result.getContent()).singleElement().satisfies(meetingRecord -> {
      assertThat(meetingRecord.getTeamId()).isEqualTo(10L);
      assertThat(meetingRecord.getParticipantCount()).isEqualTo(1);
    });
  }
}
