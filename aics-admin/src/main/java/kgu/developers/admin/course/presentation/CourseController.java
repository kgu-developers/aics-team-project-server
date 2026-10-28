package kgu.developers.admin.course.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.course.presentation.request.CourseRequest;
import kgu.developers.admin.course.presentation.response.CourseListResponse;
import kgu.developers.admin.course.presentation.response.CoursePersistResponse;
import kgu.developers.admin.course.presentation.response.CourseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Course", description = "OOP 강좌 API")
public interface CourseController {

	@Operation(summary = "강좌 생성 API", description = """
			- Description : 이 API는 신규 OOP 강좌를 생성합니다.
		""")
	@ApiResponse(
		responseCode = "201",
		content = @Content(schema = @Schema(implementation = CoursePersistResponse.class)))
	ResponseEntity<CoursePersistResponse> createCourse(
		@Parameter(
			description = "강좌 생성 request 객체 입니다.",
			required = true
		) @Valid @RequestBody CourseRequest request
	);

	@GetMapping("/{id}")
	ResponseEntity<CourseResponse> getCourseById(
			@Positive @PathVariable Long id
	);

	@Operation(summary = "강좌 목록 조회 API", description = """
			- Description : 이 API는 호출한 교수 본인이 담당 분반을 가진 강좌, 그리고 분반이 아직 하나도 없는 강좌를 조회합니다.
			- 강좌는 여러 교수의 분반이 같이 달릴 수 있는 공용 카탈로그입니다. 다른 교수만 담당하는 분반이 있는 강좌는 응답에서 제외됩니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = CourseListResponse.class)))
	ResponseEntity<CourseListResponse> getCourses(Authentication authentication);

	@Operation(summary = "강좌 수정 API", description = """
			- Description : 이 API는 기존 강좌 정보를 수정합니다.
		""")
	@ApiResponse(responseCode = "204")
	ResponseEntity<Void> updateCourse(
		@Parameter(
			description = "강좌 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long id,
		@Parameter(
			description = "강좌 수정 request 객체 입니다.",
			required = true
		) @Valid @RequestBody CourseRequest request
	);

	@Operation(summary = "강좌 삭제 API", description = """
			- Description : 이 API는 지정된 강좌를 삭제합니다.
		""")
	@ApiResponse(responseCode = "204")
	ResponseEntity<Void> deleteCourse(
		@Parameter(
			description = "강좌 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long id
	);
}
