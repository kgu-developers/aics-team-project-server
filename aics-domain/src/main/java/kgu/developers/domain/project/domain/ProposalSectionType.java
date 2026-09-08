package kgu.developers.domain.project.domain;

/**
 * 교수님이 제공한 양식의 고정 섹션 구성. 팀이 섹션을 추가·삭제할 수 없고, 양식이 바뀌면 여기만 고친다.
 * 각 섹션의 본문은 별도 저장소가 아니라 이미 있는 필드에 들어간다.
 * TOPIC=확정된 topicCandidateId(+복사된 title·description·goal),
 * DATA=dataConfiguration, SCREEN=screenConfiguration,
 * TEAM_OPERATION=collaborationStyle·projectSchedule + 팀원별 team_member.project_role.
 */
public enum ProposalSectionType {
    TOPIC,           // 주제
    DATA,            // 데이터 구성
    SCREEN,          // 화면 구성
    TEAM_OPERATION   // 팀 운영방식
}
