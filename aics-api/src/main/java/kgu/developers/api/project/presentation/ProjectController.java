package kgu.developers.api.project.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.request.ProposalSectionRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectImageUploadResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionListResponse;
import kgu.developers.api.project.presentation.response.ProposalSectionResponse;
import kgu.developers.domain.project.domain.ProposalSectionType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Project", description = "프로젝트 제안서 API")
public interface ProjectController {

    @Operation(summary = "프로젝트 제안서 조회", description = "팀원 또는 해당 분반 담당 교수가 조회한다.")
    ResponseEntity<ProjectResponse> getProject(@PathVariable Long teamId, Authentication authentication);

    @Operation(summary = "프로젝트 제안서 등록 또는 수정",
        description = "팀 운영방식(kickoffRule·meetingSchedule·memberRoles)은 킥오프와 저장소가 같아서 "
            + "여기서 수정한 값이 킥오프 조회에도 그대로 반영된다. 넘기지 않으면 지금 값을 유지한다."
          + "memberRoles는 역할 분담을 의미한다. meetingSchedule는 회의 일정을 의미한다. kickoffRule은 협업 방식을 의미한다. projectSchedule은 진행 일정을 의미한다.")
    ResponseEntity<ProjectResponse> saveProject(
        @PathVariable Long teamId,
        @Valid @RequestBody ProjectRequest request,
        Authentication authentication
    );

    @Operation(summary = "제안서 화면 이미지 업로드",
        description = "이미지를 저장하고 제안서 화면 구성의 imageFileId에 사용할 fileId를 반환한다. 제안서 저장·제출 상태는 변경하지 않는다.")
    ResponseEntity<ProjectImageUploadResponse> uploadProjectImage(
        @PathVariable Long teamId,
        @RequestPart("file") MultipartFile file,
        Authentication authentication
    );

    @Operation(summary = "프로젝트 제안서 삭제")
    ResponseEntity<Void> deleteProject(@PathVariable Long projectId, Authentication authentication);

    @Operation(summary = "제안서 섹션별 담당·작성 완료 현황 조회",
        description = "교수님 양식의 고정 섹션 구성을 모두 내려준다. 팀원 또는 해당 분반 담당 교수가 조회한다.")
    ResponseEntity<ProposalSectionListResponse> getProposalSections(@PathVariable Long projectId, Authentication authentication);

    @Operation(summary = "제안서 섹션 담당·작성 완료 상태 저장",
        description = "팀원이 섹션 담당자와 작성 완료 여부를 지정한다. 섹션 본문은 제안서 저장 API로 저장한다.")
    ResponseEntity<ProposalSectionResponse> updateProposalSection(
        @PathVariable Long projectId,
        @PathVariable ProposalSectionType section,
        @Valid @RequestBody ProposalSectionRequest request,
        Authentication authentication
    );

    @Operation(summary = "프로젝트 제안 단계 완료",
        description = "모든 섹션이 작성 완료된 뒤 팀장이 최종 제출한다. 팀원 동의는 선행 조건이 아니다.")
    ResponseEntity<Void> completeProposal(@PathVariable Long projectId, Authentication authentication);

    @Operation(summary = "프로젝트 제안서 동의")
    ResponseEntity<Void> approveProject(@PathVariable Long projectId, Authentication authentication);

    @Operation(summary = "프로젝트 제안서 팀원 동의 현황 조회")
    ResponseEntity<ProjectApprovalSummaryResponse> getApprovalSummary(@PathVariable Long projectId, Authentication authentication);
}
