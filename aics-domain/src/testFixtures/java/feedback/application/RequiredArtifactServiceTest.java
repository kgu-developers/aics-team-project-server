package feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kgu.developers.domain.feedback.application.command.RequiredArtifactCommandService;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.feedback.exception.RequiredArtifactNotFoundException;
import mock.repository.FakeRequiredArtifactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequiredArtifactServiceTest {
    private FakeRequiredArtifactRepository repository;
    private RequiredArtifactCommandService commandService;
    private RequiredArtifactQueryService queryService;

    @BeforeEach
    void setUp() {
        repository = new FakeRequiredArtifactRepository();
        queryService = new RequiredArtifactQueryService(repository);
        commandService = new RequiredArtifactCommandService(repository, queryService);
    }

    @Test
    @DisplayName("필수 산출물을 생성하고 마일스톤별로 조회한다")
    void createAndFindAll() {
        Long id = commandService.create(
                1L,
                RequiredArtifactType.FILE,
                " 보고서 PDF ",
                true,
                "pdf",
                20
        );

        assertThat(queryService.getRequiredArtifacts(1L)).singleElement().satisfies(artifact -> {
            assertThat(artifact.getId()).isEqualTo(id);
            assertThat(artifact.getLabel()).isEqualTo("보고서 PDF");
            assertThat(artifact.getAllowedExtensions()).isEqualTo("pdf");
        });
    }

    @Test
    @DisplayName("다른 마일스톤의 산출물은 수정할 수 없다")
    void rejectUpdateFromAnotherMilestone() {
        Long id = commandService.create(1L, RequiredArtifactType.LINK, "시연 링크", true, null, null);

        assertThatThrownBy(() -> commandService.update(
                2L,
                id,
                RequiredArtifactType.TEXT,
                "설명",
                false,
                null,
                null
        )).isInstanceOf(RequiredArtifactNotFoundException.class);
    }

    @Test
    @DisplayName("삭제한 산출물은 활성 목록과 조회에서 제외한다")
    void softDelete() {
        Long id = commandService.create(1L, RequiredArtifactType.LINK, "시연 링크", true, null, null);

        commandService.delete(1L, id);

        assertThat(queryService.getRequiredArtifacts(1L)).isEmpty();
        assertThatThrownBy(() -> queryService.getRequiredArtifact(1L, id))
                .isInstanceOf(RequiredArtifactNotFoundException.class);
    }

    @Test
    @DisplayName("파일이 아닌 산출물에는 파일 옵션을 설정할 수 없다")
    void rejectFileOptionsForNonFileType() {
        assertThatThrownBy(() -> commandService.create(
                1L,
                RequiredArtifactType.LINK,
                "시연 링크",
                true,
                "pdf",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("산출물 설정을 수정한다")
    void update() {
        Long id = commandService.create(1L, RequiredArtifactType.FILE, "보고서", true, "pdf", 10);

        commandService.update(1L, id, RequiredArtifactType.FILE, "최종 보고서", false, "pdf,zip", 30);

        RequiredArtifact updated = queryService.getRequiredArtifact(1L, id);
        assertThat(updated.getLabel()).isEqualTo("최종 보고서");
        assertThat(updated.isRequired()).isFalse();
        assertThat(updated.getAllowedExtensions()).isEqualTo("pdf,zip");
        assertThat(updated.getMaxFileSizeMb()).isEqualTo(30);
    }
}
