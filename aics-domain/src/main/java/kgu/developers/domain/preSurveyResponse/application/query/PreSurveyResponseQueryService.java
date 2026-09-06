package kgu.developers.domain.preSurveyResponse.application.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static kgu.developers.domain.enrollment.domain.Role.STUDENT;
import static kgu.developers.domain.enrollment.domain.Status.ACTIVE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

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
	private static final String[] EXCEL_HEADERS = {"학번", "이름", "희망 역할", "주제 의견", "기타 의견", "제출일"};
	private static final DateTimeFormatter SUBMITTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final String NOT_SUBMITTED = "미제출";

	private final PreSurveyResponseRepository preSurveyResponseRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;

	public PreSurveyResponse getResponse(String userId, Long sectionId) {
		return preSurveyResponseRepository.findByUserIdAndSectionId(userId, sectionId)
				.orElseThrow(PreSurveyResponseNotFoundException::new);
	}

	/**
	 * 분반 수강생 전원을 학번 오름차순 시트 한 장으로 만든다. 미응답 학생도 제출일 칸에 "미제출"을
	 * 적은 빈 행으로 넣는다. 호출 전 분반 접근 권한은 상위에서 확인한다.
	 */
	public byte[] writeSectionResponsesExcel(Long sectionId) {
		// 응답 행에는 유니크 제약이 보장되지 않아 같은 학번이 여러 건일 수 있다. 다른 조회 경로와
		// 마찬가지로 id가 가장 큰(가장 최근) 응답을 그 학생의 응답으로 본다.
		Map<String, PreSurveyResponse> responseByUserId = preSurveyResponseRepository.findAllBySectionId(sectionId)
				.stream()
				.collect(toMap(PreSurveyResponse::getUserId, identity(),
						(a, b) -> a.getId() >= b.getId() ? a : b));

		// 수강생 명단이 기준이지만, 수강 철회 등으로 명단에서 빠진 학생의 응답까지 잃지 않도록 합집합을 쓴다.
		TreeSet<String> userIds = new TreeSet<>(responseByUserId.keySet());
		enrollmentRepository.findAllBySectionId(sectionId).stream()
				.filter(enrollment -> enrollment.getStatus() == ACTIVE && enrollment.getRole() == STUDENT)
				.map(Enrollment::getUserId)
				.forEach(userIds::add);

		Map<String, String> nameByUserId = userRepository.findAllByStudentNumberIn(List.copyOf(userIds)).stream()
				.collect(toMap(User::getStudentNumber, User::getName));

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("사전조사 응답");

			Row header = sheet.createRow(0);
			for (int i = 0; i < EXCEL_HEADERS.length; i++) {
				header.createCell(i).setCellValue(EXCEL_HEADERS[i]);
			}

			int rowNumber = 1;
			for (String userId : userIds) {
				Row row = sheet.createRow(rowNumber++);
				row.createCell(0).setCellValue(userId);
				row.createCell(1).setCellValue(nameByUserId.get(userId));  // 탈퇴로 사라진 계정이면 빈 칸

				PreSurveyResponse response = responseByUserId.get(userId);
				if (response == null) {
					row.createCell(5).setCellValue(NOT_SUBMITTED);
					continue;
				}
				row.createCell(2).setCellValue(preferredRoles(response.getPreferredRoles()));
				row.createCell(3).setCellValue(response.getTopicOpinion());
				row.createCell(4).setCellValue(response.getEtcOpinion());
				row.createCell(5).setCellValue(response.getSubmittedAt().format(SUBMITTED_AT_FORMATTER));
			}

			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// 희망 역할은 형식 제약이 없는 jsonb라 배열이면 사람이 읽을 수 있게 풀고, 그 외 형태는 원본 그대로 적는다.
	private String preferredRoles(JsonNode preferredRoles) {
		if (!preferredRoles.isArray()) {
			return preferredRoles.toString();
		}
		return StreamSupport.stream(preferredRoles.spliterator(), false)
				.map(JsonNode::asText)
				.collect(joining(", "));
	}
}
