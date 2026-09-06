package kgu.developers.admin.submission.application;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

// zip 다운로드 응답을 만드는 데 필요한 파일명과 실제 스트리밍 바디를 함께 묶어 컨트롤러에 전달한다.
public record SubmissionArtifactsZipDownload(String fileName, StreamingResponseBody body) {
}
