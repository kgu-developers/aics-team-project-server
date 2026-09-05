package kgu.developers.domain.projectApproval.domain;

/**
 * 팀원 전체 인원과 그중 현재 제안서 리비전에 동의한 인원.
 */
public record ApprovalCount(long totalMembers, long approvedMembers) {
}
