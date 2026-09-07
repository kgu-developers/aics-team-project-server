package kgu.developers.domain.preSurveyResponse.application.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static kgu.developers.domain.enrollment.domain.Role.STUDENT;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponseRepository;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseNotFoundException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreSurveyResponseQueryService {
	private final PreSurveyResponseRepository preSurveyResponseRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;

	public Optional<PreSurveyResponse> findResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId);
	}

	public PreSurveyResponse getResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
				.orElseThrow(PreSurveyResponseNotFoundException::new);
	}

	public List<PreSurveyResponse> getReceivedPreferredPeerRequests(String peerUserId, Long sectionId) {
		return preSurveyResponseRepository.findAllBySectionIdAndPreferredPeerUserId(sectionId, peerUserId);
	}

	/**
	 * 분반 수강생 전원을 학번 오름차순으로 돌려준다. 아직 응답하지 않은 학생도 response가 null인
	 * 행으로 포함된다. 호출 전 분반 접근 권한은 상위에서 확인한다.
	 */
	public List<PreSurveyResponseRow> getSectionResponseRows(Long sectionId) {
		// 응답 행에는 유니크 제약이 보장되지 않아 같은 학번이 여러 건일 수 있다. 다른 조회 경로와
		// 마찬가지로 id가 가장 큰(가장 최근) 응답을 그 학생의 응답으로 본다.
		Map<String, PreSurveyResponse> responseByUserId = preSurveyResponseRepository.findAllBySectionId(sectionId)
				.stream()
				.collect(toMap(PreSurveyResponse::getUserId, identity(),
						(a, b) -> a.getId() >= b.getId() ? a : b));

		// 현재 수강 중인 학생만 기준으로 삼는다. 조교나 수강 철회 학생이 남긴 응답은 결과에서 제외된다.
		TreeSet<String> userIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
				.filter(enrollment -> enrollment.getStatus() == ACTIVE && enrollment.getRole() == STUDENT)
				.map(Enrollment::getUserId)
				.collect(toCollection(TreeSet::new));

		Map<String, String> nameByUserId = userRepository.findAllByStudentNumberIn(List.copyOf(userIds)).stream()
				.collect(toMap(User::getStudentNumber, User::getName));

		return userIds.stream()
				.map(userId -> new PreSurveyResponseRow(userId, nameByUserId.get(userId), responseByUserId.get(userId)))
				.toList();
	}
}
