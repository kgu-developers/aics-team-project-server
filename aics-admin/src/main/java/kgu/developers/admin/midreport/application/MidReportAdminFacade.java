package kgu.developers.admin.midreport.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kgu.developers.admin.midreport.presentation.request.MidReportFeedbackAdminRequest;
import kgu.developers.admin.midreport.presentation.response.MidReportAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportBlockAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminPageResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportFeedbackAdminResponse;
import kgu.developers.admin.midreport.presentation.response.MidReportRevisionAdminResponse;
import kgu.developers.common.json.JsonConverter;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.midreport.application.command.MidReportCommandService;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import kgu.developers.domain.midreport.exception.InvalidMidReportFieldsException;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.midreport.exception.MidReportNotSubmittedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidReportAdminFacade {

    private final SectionQueryService sectionQueryService;
    private final TeamRepository teamRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final MidReportRepository midReportRepository;
    private final MidReportCommandService midReportCommandService;
    private final TeamThreadCommandService teamThreadCommandService;
    private final TeamThreadQueryService teamThreadQueryService;
    private final TeamMessageCommandService teamMessageCommandService;
    private final TeamMessageQueryService teamMessageQueryService;
    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;

    @Transactional
    public MidReportAdminResponse getMidReport(Long sectionId, Long teamId, String professorId) {
        Team team = validateProfessorOwnsSectionAndTeam(sectionId, teamId, professorId);
        Milestone milestone = getMidReportMilestone(sectionId);

        Project project = projectRepository.findAllByTeamId(team.getId()).stream().findFirst().orElse(null);
        String baseTitle = project == null ? team.getName() : project.getTitle();
        MidReport report = midReportCommandService.getOrCreate(
            team.getId(),
            milestone.getId(),
            baseTitle + " 중간보고서",
            milestone.getSchedule().dueAt(),
            project == null ? "" : project.getTitle(),
            project == null ? "" : project.getDescription()
        );

        return toAdminResponse(team, milestone, report);
    }

    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("id"));

    @Transactional
    public MidReportFeedbackAdminResponse postFeedback(
        Long sectionId,
        Long teamId,
        MidReportFeedbackAdminRequest request,
        String professorId
    ) {
        Team team = validateProfessorOwnsSectionAndTeam(sectionId, teamId, professorId);
        Milestone milestone = getMidReportMilestone(sectionId);

        MidReport report = midReportRepository.findByTeamIdAndMilestoneId(teamId, milestone.getId())
            .orElseThrow(MidReportNotFoundException::new);

        if (report.getStatus() != MidReportStatus.SUBMITTED) {
            throw new MidReportNotSubmittedException();
        }

        List<String> affectedBlockKeys = request.affectedBlockKeys() != null ? request.affectedBlockKeys() : List.of();
        for (String blockKey : affectedBlockKeys) {
            MidReportBlockDefinition.fromKey(blockKey);
        }

        MidReport updatedReport = midReportCommandService.requestRevision(report.getId(), affectedBlockKeys, LocalDateTime.now());
        if (updatedReport.getStatus() != MidReportStatus.REVISION_REQUESTED) {
            throw new MidReportNotSubmittedException();
        }

        TeamThread thread = teamThreadCommandService.getOrCreateThread(teamId);
        TeamMessage message = teamMessageCommandService.postMessage(
            thread.getId(),
            professorId,
            TeamMessageRelatedType.MID_REPORT,
            report.getId(),
            request.message()
        );

        String professorName = resolveUserName(professorId);
        return MidReportFeedbackAdminResponse.of(message, teamId, report.getId(), professorName);
    }

    public MidReportFeedbackAdminPageResponse getFeedbacks(
        Long sectionId,
        Long teamId,
        Pageable pageable,
        String professorId
    ) {
        validateProfessorOwnsSectionAndTeam(sectionId, teamId, professorId);
        Milestone milestone = getMidReportMilestone(sectionId);
        MidReport report = midReportRepository.findByTeamIdAndMilestoneId(teamId, milestone.getId())
            .orElse(null);
        if (report == null) {
            return emptyFeedbackPage(pageable);
        }

        TeamThread thread;
        try {
            thread = teamThreadQueryService.getThread(teamId);
        } catch (TeamThreadNotFoundException exception) {
            return emptyFeedbackPage(pageable);
        }

        Pageable latestFirstPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            LATEST_FIRST
        );

        Page<TeamMessage> messages = teamMessageQueryService.getMessages(
            thread.getId(),
            TeamMessageRelatedType.MID_REPORT,
            report.getId(),
            latestFirstPageable
        );

        List<String> senderIds = messages.getContent().stream()
            .map(TeamMessage::getSenderId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .toList();
        Map<String, String> senderNames = userQueryService.getUsersByStudentNumbersIncludingDeleted(senderIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName, (first, second) -> first));

        Page<MidReportFeedbackAdminResponse> mappedPage = messages.map(msg ->
            MidReportFeedbackAdminResponse.of(msg, teamId, report.getId(), senderNames.get(msg.getSenderId()))
        );

        return MidReportFeedbackAdminPageResponse.from(mappedPage);
    }

    private MidReportFeedbackAdminPageResponse emptyFeedbackPage(Pageable pageable) {
        return MidReportFeedbackAdminPageResponse.builder()
            .contents(List.of())
            .pageable(PageableResponse.<MidReportFeedbackAdminResponse>builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalPages(0)
                .totalElements(0L)
                .isEnd(true)
                .build())
            .build();
    }

    private MidReportAdminResponse toAdminResponse(Team team, Milestone milestone, MidReport report) {
        String leaderId = teamMemberRepository.findLeaderByTeamId(report.getTeamId())
            .map(TeamMember::getUserId)
            .orElse(null);
        Set<String> userIds = report.getBlocks().stream()
            .map(MidReportBlock::getLastEditedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (leaderId != null) {
            userIds.add(leaderId);
        }
        if (report.getSubmittedBy() != null) {
            userIds.add(report.getSubmittedBy());
        }
        Map<String, String> names = userQueryService.getUsersByStudentNumbersIncludingDeleted(List.copyOf(userIds)).stream()
            .collect(Collectors.toMap(User::getStudentNumber, User::getName, (first, second) -> first));

        LocalDateTime currentDueDate = milestone.getSchedule() != null && milestone.getSchedule().dueAt() != null
            ? milestone.getSchedule().dueAt()
            : report.getDueDate();
        Map<String, JsonNode> resolvedFields = resolveFields(report);

        List<MidReportBlockAdminResponse> blockResponses = report.getBlocks().stream()
            .map(block -> MidReportBlockAdminResponse.of(
                block,
                names.get(block.getLastEditedBy()),
                resolvedFields.get(block.getKey())
            ))
            .toList();

        return MidReportAdminResponse.builder()
            .id(report.getId())
            .teamId(report.getTeamId())
            .teamName(team.getName())
            .milestoneId(report.getMilestoneId())
            .title(report.getTitle())
            .version(report.getVersion())
            .dueDate(currentDueDate)
            .status(report.getStatus())
            .submittedAt(report.getSubmittedAt())
            .submittedBy(report.getSubmittedBy())
            .submittedByName(names.get(report.getSubmittedBy()))
            .leaderName(names.get(leaderId))
            .revision(MidReportRevisionAdminResponse.from(report.getRevision()))
            .blocks(blockResponses)
            .build();
    }

    private Map<String, JsonNode> resolveFields(MidReport report) {
        Map<String, JsonNode> resolved = new LinkedHashMap<>();
        Set<String> memberIds = teamMemberRepository.findAllByTeamId(report.getTeamId()).stream()
            .map(TeamMember::getUserId)
            .collect(Collectors.toSet());
        report.getBlocks().forEach(block -> {
            if (!MidReportBlockDefinition.GUI_DESIGN.key().equals(block.getKey())) {
                resolved.put(block.getKey(), block.getFields());
                return;
            }
            JsonNode fields = block.getFields().deepCopy();
            JsonNode guiScreensField = findField(fields, "guiScreens");
            if (guiScreensField == null || !guiScreensField.path("value").isTextual()) {
                resolved.put(block.getKey(), fields);
                return;
            }
            JsonNode parsedScreens;
            try {
                parsedScreens = parseScreens(guiScreensField.path("value").asText());
            } catch (InvalidMidReportFieldsException exception) {
                resolved.put(block.getKey(), fields);
                return;
            }
            if (!parsedScreens.isArray()) {
                resolved.put(block.getKey(), fields);
                return;
            }
            ArrayNode screens = ((ArrayNode) parsedScreens).deepCopy();
            for (JsonNode screen : screens) {
                if (!screen.isObject()) {
                    continue;
                }
                ObjectNode resolvedScreen = (ObjectNode) screen;
                resolvedScreen.remove("imageUrl");
                JsonNode fileIdNode = resolvedScreen.get("imageFileId");
                if (fileIdNode == null || !fileIdNode.isIntegralNumber()) {
                    continue;
                }
                fileObjectRepository.findById(fileIdNode.asLong())
                    .filter(file -> memberIds.contains(file.getUploadedBy()))
                    .filter(this::isImage)
                    .ifPresent(file -> {
                        resolvedScreen.put("imageName", file.getFileName());
                        resolvedScreen.put("imageUrl", fileStorage.presignedUrl(file.getStorageKey()));
                    });
            }
            ((ObjectNode) guiScreensField).put("value", screens.toString());
            resolved.put(block.getKey(), fields);
        });
        return resolved;
    }

    private JsonNode findField(JsonNode fields, String key) {
        if (fields == null || !fields.isArray()) {
            return null;
        }
        for (JsonNode field : fields) {
            if (key.equals(field.path("key").asText())) {
                return field;
            }
        }
        return null;
    }

    private JsonNode parseScreens(String value) {
        return JsonConverter.parse(value, ignored -> new InvalidMidReportFieldsException());
    }

    private boolean isImage(FileObject file) {
        return file.getContentType() != null && file.getContentType().startsWith("image/");
    }

    private Milestone getMidReportMilestone(Long sectionId) {
        return milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
            .filter(item -> item.getType() == MilestoneType.MID_REPORT)
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AccessDeniedException("중간보고서 마일스톤이 존재하지 않습니다."));
    }

    private Team validateProfessorOwnsSectionAndTeam(Long sectionId, Long teamId, String professorId) {
        if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
            throw new AccessDeniedException("담당 분반의 팀만 접근할 수 있습니다.");
        }
        Team team = teamRepository.findById(teamId)
            .orElseThrow(TeamNotFoundException::new);
        if (!team.getSectionId().equals(sectionId)) {
            throw new AccessDeniedException("해당 분반에 속한 팀이 아닙니다.");
        }
        return team;
    }

    private String resolveUserName(String studentNumber) {
        if (studentNumber == null || studentNumber.isBlank()) {
            return null;
        }
        try {
            return userQueryService.getUserByStudentNumber(studentNumber).getName();
        } catch (UserNotFoundException e) {
            return null;
        }
    }
}
