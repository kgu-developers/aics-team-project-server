package kgu.developers.domain.preSurveyResponse.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredRolesInvalidException;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
		name = "\"pre_survey_response\"",
		indexes = @Index(name = "idx_pre_survey_response_section", columnList = "section_id, deleted_at"),
		uniqueConstraints = @UniqueConstraint(name = "uk_pre_survey_response_user_section", columnNames = {"user_id", "section_id"})
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PreSurveyResponseJpaEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String userId;

	@Column(nullable = false)
	private Long sectionId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String preferredRoles;

	@Column(columnDefinition = "text")
	private String topicOpinion;

	@Column(columnDefinition = "text")
	private String etcOpinion;

	@Column(nullable = false)
	private LocalDateTime submittedAt;

	public PreSurveyResponse toDomain() {
		return PreSurveyResponse.builder()
				.id(id)
				.userId(userId)
				.sectionId(sectionId)
				.preferredRoles(JsonConverter.parse(preferredRoles, PreSurveyResponsePreferredRolesInvalidException::new))
				.topicOpinion(topicOpinion)
				.etcOpinion(etcOpinion)
				.submittedAt(submittedAt)
				.createdAt(getCreatedAt())
				.updatedAt(getUpdatedAt())
				.deletedAt(getDeletedAt())
				.build();
	}

	public static PreSurveyResponseJpaEntity toEntity(PreSurveyResponse response) {
		PreSurveyResponseJpaEntity entity = PreSurveyResponseJpaEntity.builder()
				.id(response.getId())
				.userId(response.getUserId())
				.sectionId(response.getSectionId())
				.preferredRoles(response.getPreferredRoles().toString())
				.topicOpinion(response.getTopicOpinion())
				.etcOpinion(response.getEtcOpinion())
				.submittedAt(response.getSubmittedAt())
				.build();
		entity.createdAt = response.getCreatedAt();
		entity.setDeletedAt(response.getDeletedAt());
		return entity;
	}
}
