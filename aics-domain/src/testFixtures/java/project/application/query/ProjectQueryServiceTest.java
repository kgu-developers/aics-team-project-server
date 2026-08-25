package project.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    @Mock private ProjectRepository projectRepository;
    @InjectMocks private ProjectQueryService projectQueryService;

    @Test
    @DisplayName("getProjectByTeamId는 팀의 프로젝트 제안서를 반환한다")
    void getProjectByTeamId() {
        given(projectRepository.findAllByTeamId(1L)).willReturn(List.of(project()));

        assertThat(projectQueryService.getProjectByTeamId(1L).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getProjectByTeamId는 제안서가 없으면 예외를 던진다")
    void getProjectByTeamId_notFound() {
        given(projectRepository.findAllByTeamId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> projectQueryService.getProjectByTeamId(1L)).isInstanceOf(CustomException.class);
    }

    private Project project() {
        return Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
    }
}
