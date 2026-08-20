package kgu.developers.domain.teamMember.domain;

import kgu.developers.domain.user.domain.User;

/**
 * 팀원과 그 학번에 해당하는 유저를 짝지은 것. 응답 DTO 마다 조합 로직을 복사하지 않으려고 둔다.
 *
 * @param user 유저를 찾지 못하면 null. 학번만 있고 유저가 지워진 경우가 있어 조회 실패를 예외로 보지 않는다.
 */
public record TeamMemberWithUser(TeamMember member, User user) {
}
