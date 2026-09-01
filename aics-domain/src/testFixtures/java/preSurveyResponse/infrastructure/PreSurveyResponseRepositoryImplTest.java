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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse.PreSurveyResponseBuilder;
import kgu.developers.domain.preSurveyResponse.infrastructure.JpaPreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseJpaEntity;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class PreSurveyResponseRepositoryImplTest {

	private static final String USER_ID = "202012345";
	private static final Long SECTION_ID = 1L;
	private static final String TOPIC_OPINION = "웹 서비스";

	@Mock
	private JpaPreSurveyResponseRepository jpaPreSurveyResponseRepository;

	@InjectMocks
	private PreSurveyResponseRepositoryImpl repository;

	private static JsonNode roles() {
		return JsonConverter.parse("{\"role\":\"BACKEND\"}");
	}

	private static PreSurveyResponseBuilder storedBuilder(Long id) {
		return PreSurveyResponse.builder()
				.id(id)
				.userId(USER_ID)
				.sectionId(SECTION_ID)
				.preferredRoles(roles())
				.topicOpinion(TOPIC_OPINION)
				.etcOpinion(null);
	}

	private static PreSurveyResponseJpaEntity storedEntity(Long id) {
		return PreSurveyResponseJpaEntity.toEntity(storedBuilder(id).build());
	}

	@Test
	@DisplayName("저장소 어댑터는 저장 결과를 도메인으로 반환한다")
	void save() {
		PreSurveyResponse response = PreSurveyResponse.create(USER_ID, SECTION_ID, roles(), TOPIC_OPINION, null);

		given(jpaPreSurveyResponseRepository.save(any(PreSurveyResponseJpaEntity.class)))
				.willReturn(PreSurveyResponseJpaEntity.toEntity(
						storedBuilder(1L)
								.submittedAt(response.getSubmittedAt())
								.createdAt(response.getCreatedAt())
								.updatedAt(response.getUpdatedAt())
								.build()
				));

		PreSurveyResponse saved = repository.save(response);

		assertThat(saved.getId()).isEqualTo(1L);
		ArgumentCaptor<PreSurveyResponseJpaEntity> captor = ArgumentCaptor.forClass(PreSurveyResponseJpaEntity.class);
		verify(jpaPreSurveyResponseRepository).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
		assertThat(captor.getValue().getSectionId()).isEqualTo(SECTION_ID);
	}

	@Test
	@DisplayName("저장소 어댑터는 id로 삭제되지 않은 응답을 조회한다")
	void findById() {
		given(jpaPreSurveyResponseRepository.findByIdAndDeletedAtIsNull(1L))
				.willReturn(Optional.of(storedEntity(1L)));

		Optional<PreSurveyResponse> found = repository.findById(1L);

		assertThat(found).isPresent();
		assertThat(found.get().getUserId()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("저장소 어댑터는 사용자와 섹션으로 삭제되지 않은 응답을 조회한다")
	void findByUserIdAndSectionId() {
		given(jpaPreSurveyResponseRepository.findByUserIdAndSectionIdAndDeletedAtIsNull(USER_ID, SECTION_ID))
				.willReturn(Optional.of(storedEntity(1L)));

		Optional<PreSurveyResponse> found = repository.findByUserIdAndSectionId(USER_ID, SECTION_ID);

		assertThat(found).isPresent();
		assertThat(found.get().getUserId()).isEqualTo(USER_ID);
		assertThat(found.get().getSectionId()).isEqualTo(SECTION_ID);
	}

	@Test
	@DisplayName("저장소 어댑터는 섹션별로 삭제되지 않은 응답 목록을 조회한다")
	void findAllBySectionId() {
		given(jpaPreSurveyResponseRepository.findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(SECTION_ID))
				.willReturn(List.of(storedEntity(1L)));

		List<PreSurveyResponse> responses = repository.findAllBySectionId(SECTION_ID);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getUserId()).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("사용자·섹션 조회는 응답이 없으면 다른 조회 없이 빈 값만 반환한다")
	void findByUserIdAndSectionId_ReturnsEmptyWithoutExtraQueries() {
		given(jpaPreSurveyResponseRepository.findByUserIdAndSectionIdAndDeletedAtIsNull(USER_ID, SECTION_ID))
				.willReturn(Optional.empty());

		Optional<PreSurveyResponse> existing = repository.findByUserIdAndSectionId(USER_ID, SECTION_ID);

		assertThat(existing).isEmpty();
		verify(jpaPreSurveyResponseRepository).findByUserIdAndSectionIdAndDeletedAtIsNull(USER_ID, SECTION_ID);
		verifyNoMoreInteractions(jpaPreSurveyResponseRepository);
	}
}
