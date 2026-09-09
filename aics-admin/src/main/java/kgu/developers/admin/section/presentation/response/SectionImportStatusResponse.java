package kgu.developers.admin.section.presentation.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionImportStatusResponse {
	private LastImportStatusResponse enrollment;
	private LastImportStatusResponse team;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class LastImportStatusResponse {
		private String fileName;
		private LocalDateTime appliedAt;
	}
}