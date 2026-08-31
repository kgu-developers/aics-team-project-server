package preSurveyResponse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.infrastructure.JpaPreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseJpaEntity;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class PreSurveyResponseRepositoryImplTest {

	@Mock
	private JpaPreSurveyResponseRepository jpaPreSurveyResponseRepository;

	@Test
	@DisplayName("저장소 어댑터는 저장 결과를 도메인으로 반환한다")
	void save() {
		PreSurveyResponseRepositoryImpl repository = new PreSurveyResponseRepositoryImpl(jpaPreSurveyResponseRepository);
		JsonNode roles = JsonConverter.parse("{\"role\":\"BACKEND\"}");
		PreSurveyResponse response = PreSurveyResponse.create("202012345", 1L, roles, "웹 서비스", null);

		given(jpaPreSurveyResponseRepository.save(any(PreSurveyResponseJpaEntity.class)))
				.willReturn(PreSurveyResponseJpaEntity.toEntity(
						PreSurveyResponse.builder()
								.id(1L)
								.userId("202012345")
								.sectionId(1L)
								.preferredRoles(roles)
								.topicOpinion("웹 서비스")
								.etcOpinion(null)
								.submittedAt(response.getSubmittedAt())
								.createdAt(response.getCreatedAt())
								.updatedAt(response.getUpdatedAt())
								.deletedAt(null)
								.build()
				));

		PreSurveyResponse saved = repository.save(response);

		assertThat(saved.getId()).isEqualTo(1L);
		ArgumentCaptor<PreSurveyResponseJpaEntity> captor = ArgumentCaptor.forClass(PreSurveyResponseJpaEntity.class);
		verify(jpaPreSurveyResponseRepository).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo("202012345");
		assertThat(captor.getValue().getSectionId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("저장소 어댑터는 id로 삭제되지 않은 응답을 조회한다")
	void findById() {
		PreSurveyResponseRepositoryImpl repository = new PreSurveyResponseRepositoryImpl(jpaPreSurveyResponseRepository);
		JsonNode roles = JsonConverter.parse("{\"role\":\"BACKEND\"}");

		given(jpaPreSurveyResponseRepository.findByIdAndDeletedAtIsNull(1L))
				.willReturn(Optional.of(PreSurveyResponseJpaEntity.toEntity(
						PreSurveyResponse.builder()
								.id(1L)
								.userId("202012345")
								.sectionId(1L)
								.preferredRoles(roles)
								.topicOpinion("웹 서비스")
								.etcOpinion(null)
								.submittedAt(null)
								.createdAt(null)
								.updatedAt(null)
								.deletedAt(null)
								.build()
				)));

		Optional<PreSurveyResponse> found = repository.findById(1L);

		assertThat(found).isPresent();
		assertThat(found.get().getUserId()).isEqualTo("202012345");
	}

	@Test
	@DisplayName("저장소 어댑터는 사용자와 섹션으로 삭제되지 않은 응답을 조회한다")
	void findByUserIdAndSectionId() {
		PreSurveyResponseRepositoryImpl repository = new PreSurveyResponseRepositoryImpl(jpaPreSurveyResponseRepository);
		JsonNode roles = JsonConverter.parse("{\"role\":\"BACKEND\"}");

		given(jpaPreSurveyResponseRepository.findByUserIdAndSectionIdAndDeletedAtIsNull("202012345", 1L))
				.willReturn(Optional.of(PreSurveyResponseJpaEntity.toEntity(
						PreSurveyResponse.builder()
								.id(1L)
								.userId("202012345")
								.sectionId(1L)
								.preferredRoles(roles)
								.topicOpinion("웹 서비스")
								.etcOpinion(null)
								.submittedAt(null)
								.createdAt(null)
								.updatedAt(null)
								.deletedAt(null)
								.build()
				)));

		Optional<PreSurveyResponse> found = repository.findByUserIdAndSectionId("202012345", 1L);

		assertThat(found).isPresent();
		assertThat(found.get().getUserId()).isEqualTo("202012345");
		assertThat(found.get().getSectionId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("저장소 어댑터는 섹션별로 삭제되지 않은 응답 목록을 조회한다")
	void findAllBySectionId() {
		PreSurveyResponseRepositoryImpl repository = new PreSurveyResponseRepositoryImpl(jpaPreSurveyResponseRepository);
		JsonNode roles = JsonConverter.parse("{\"role\":\"BACKEND\"}");

		given(jpaPreSurveyResponseRepository.findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(1L))
				.willReturn(List.of(PreSurveyResponseJpaEntity.toEntity(
						PreSurveyResponse.builder()
								.id(1L)
								.userId("202012345")
								.sectionId(1L)
								.preferredRoles(roles)
								.topicOpinion("웹 서비스")
								.etcOpinion(null)
								.submittedAt(null)
								.createdAt(null)
								.updatedAt(null)
								.deletedAt(null)
								.build()
				)));

		List<PreSurveyResponse> responses = repository.findAllBySectionId(1L);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getUserId()).isEqualTo("202012345");
	}

	@Test
	@DisplayName("중복 제출 체크는 소프트 삭제된 응답을 제외한 조회에만 의존한다")
	void checkDuplicateSubmissionExcludesSoftDeleted() {
		PreSurveyResponseRepositoryImpl repository = new PreSurveyResponseRepositoryImpl(jpaPreSurveyResponseRepository);
		String userId = "202012345";
		Long sectionId = 1L;

		given(jpaPreSurveyResponseRepository.findByUserIdAndSectionIdAndDeletedAtIsNull(userId, sectionId))
				.willReturn(Optional.empty());

		Optional<PreSurveyResponse> existing = repository.findByUserIdAndSectionId(userId, sectionId);

		assertThat(existing).isEmpty();
		verify(jpaPreSurveyResponseRepository).findByUserIdAndSectionIdAndDeletedAtIsNull(userId, sectionId);
		verifyNoMoreInteractions(jpaPreSurveyResponseRepository);
	}
}
