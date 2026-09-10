package submission.infrastructure;

import java.time.LocalDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.infrastructure.JpaSubmissionArtifactRepository;
import kgu.developers.domain.submission.infrastructure.JpaSubmissionMemberConfirmationRepository;
import kgu.developers.domain.submission.infrastructure.JpaSubmissionRepository;
import kgu.developers.domain.submission.infrastructure.SubmissionArtifactJpaEntity;
import kgu.developers.domain.submission.infrastructure.SubmissionArtifactRepositoryImpl;
import kgu.developers.domain.submission.infrastructure.SubmissionMemberConfirmationJpaEntity;
import kgu.developers.domain.submission.infrastructure.SubmissionMemberConfirmationRepositoryImpl;
import kgu.developers.domain.submission.infrastructure.SubmissionRepositoryImpl;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:submission-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
    SubmissionArtifactRepositoryImpl.class,
    SubmissionMemberConfirmationRepositoryImpl.class,
    SubmissionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubmissionPersistenceIntegrationTest {

    @SpringBootConfiguration
    @EntityScan("kgu.developers.domain.submission.infrastructure")
    @EnableJpaRepositories("kgu.developers.domain.submission.infrastructure")
    static class TestConfig {
    }

    @Autowired
    private SubmissionArtifactRepositoryImpl artifactRepository;

    @Autowired
    private JpaSubmissionArtifactRepository jpaArtifactRepository;

    @Autowired
    private SubmissionMemberConfirmationRepositoryImpl confirmationRepository;

    @Autowired
    private JpaSubmissionMemberConfirmationRepository jpaConfirmationRepository;

    @Autowired
    private SubmissionRepositoryImpl submissionRepository;

    @Test
    @DisplayName("산출물(SubmissionArtifact) 저장 시 BaseTimeEntity의 createdAt과 updatedAt이 정상 생성된다")
    void saveSubmissionArtifact_PopulatesCreatedAtAndUpdatedAt() {
        SubmissionArtifact artifact = SubmissionArtifact.file(1L, 10L, 100L);

        List<SubmissionArtifact> saved = artifactRepository.saveAll(List.of(artifact));

        Assertions.assertThat(saved).hasSize(1);
        Long artifactId = saved.get(0).getId();
        Assertions.assertThat(artifactId).isNotNull();

        SubmissionArtifactJpaEntity entity = jpaArtifactRepository.findById(artifactId).orElseThrow();
        Assertions.assertThat(entity.getCreatedAt()).isNotNull();
        Assertions.assertThat(entity.getUpdatedAt()).isNotNull();
        Assertions.assertThat(entity.getType()).isEqualTo(ArtifactType.FILE);
        Assertions.assertThat(entity.getFileId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("팀원 확인(SubmissionMemberConfirmation) 최초 저장 및 새 버전 재확인(UPDATE) 시 409 충돌 없이 동작한다")
    void saveAndConfirmNewVersion_DoesNotCauseUniqueConstraintConflict() {
        Long submissionId = 1L;
        String userId = "202611111";

        // 최초 확인 저장 (v1)
        SubmissionMemberConfirmation confirmationV1 = SubmissionMemberConfirmation.builder()
                .submissionId(submissionId)
                .userId(userId)
                .version(1)
                .confirmedAt(LocalDateTime.now())
                .build();
        SubmissionMemberConfirmation savedV1 = confirmationRepository.save(confirmationV1);
        Assertions.assertThat(savedV1.getId()).isNotNull();

        SubmissionMemberConfirmationJpaEntity entityV1 = jpaConfirmationRepository.findById(savedV1.getId()).orElseThrow();
        Assertions.assertThat(entityV1.getCreatedAt()).isNotNull();
        Assertions.assertThat(entityV1.getUpdatedAt()).isNotNull();
        Assertions.assertThat(entityV1.getVersion()).isEqualTo(1);

        // 재제출 후 버전 갱신 확인 (v2) - 기존 ID 및 createdAt을 유지하여 UPDATE
        SubmissionMemberConfirmation confirmationV2 = savedV1.updateConfirm(2);

        Assertions.assertThatCode(() -> {
            SubmissionMemberConfirmation savedV2 = confirmationRepository.save(confirmationV2);
            Assertions.assertThat(savedV2.getId()).isEqualTo(savedV1.getId());
            Assertions.assertThat(savedV2.getVersion()).isEqualTo(2);
        }).doesNotThrowAnyException();

        SubmissionMemberConfirmationJpaEntity entityV2 = jpaConfirmationRepository.findById(savedV1.getId()).orElseThrow();
        Assertions.assertThat(entityV2.getVersion()).isEqualTo(2);
        Assertions.assertThat(entityV2.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("제출물(Submission) 수정 후 재저장 시 createdAt이 보존된다")
    void saveSubmission_PreservesCreatedAtOnUpdate() {
        Submission submission = submissionRepository.save(Submission.create(1L, 10L));
        LocalDateTime originalCreatedAt = submission.getCreatedAt();

        submission.recordNewVersion(1);
        Submission updated = submissionRepository.save(submission);

        Assertions.assertThat(updated.getCurrentVersion()).isEqualTo(1);
        Assertions.assertThat(updated.getCreatedAt()).isNotNull();
        if (originalCreatedAt != null) {
            Assertions.assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        }
    }
}
