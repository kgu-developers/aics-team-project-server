package kgu.developers.domain.teammessage.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.teammessage.domain.TeamMessageReadReceipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "\"team_message_read_receipt\"",
    uniqueConstraints = @UniqueConstraint(name = "uq_team_message_read_receipt", columnNames = {"message_id", "user_id"})
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamMessageReadReceiptJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    public TeamMessageReadReceipt toDomain() {
        return TeamMessageReadReceipt.builder()
            .id(this.id)
            .messageId(this.messageId)
            .userId(this.userId)
            .build();
    }

    public static TeamMessageReadReceiptJpaEntity toEntity(TeamMessageReadReceipt domain) {
        return TeamMessageReadReceiptJpaEntity.builder()
            .id(domain.getId())
            .messageId(domain.getMessageId())
            .userId(domain.getUserId())
            .build();
    }
}
