package kgu.developers.domain.meetingrecord.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.meetingrecord.domain.MeetingParticipant;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"meeting_record\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class MeetingRecordJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    // 개발 DB에 이미 title 없는 옛 회의록 행이 있을 수 있어(ddl-auto=update가 NOT NULL 컬럼
    // 추가 자체를 실패시킴, sunzx0428 PR #118 리뷰) DB 컬럼은 nullable로 두고, "필수"는
    // 애플리케이션 레벨(생성 시 @NotBlank, 수정 시 MeetingRecordInvalidTitleException)에서만 강제한다.
    @Column(length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    @Enumerated(STRING)
    private MeetingPhase phase;

    @Column(nullable = false, length = 20)
    private String authorId;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(length = 200)
    private String location;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Version
    private long version;

    public MeetingRecord toDomain(List<MeetingParticipant> participants) {
        return MeetingRecord.builder()
            .id(this.id)
            .teamId(this.teamId)
            .title(this.title)
            .phase(this.phase)
            .authorId(this.authorId)
            .meetingAt(this.meetingAt)
            .location(this.location)
            .content(this.content)
            .participants(participants)
            .version(this.version)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static MeetingRecordJpaEntity toEntity(MeetingRecord domain) {
        return MeetingRecordJpaEntity.builder()
            .id(domain.getId())
            .teamId(domain.getTeamId())
            .title(domain.getTitle())
            .phase(domain.getPhase())
            .authorId(domain.getAuthorId())
            .meetingAt(domain.getMeetingAt())
            .location(domain.getLocation())
            .content(domain.getContent())
            .version(domain.getVersion())
            .build();
    }
}
