package kgu.developers.admin.preSurveyResponse.application;

// 엑셀 다운로드 응답을 만드는 데 필요한 파일명과 통합문서 바이트를 함께 묶어 컨트롤러에 전달한다.
public record PreSurveyResponseExcelDownload(String fileName, byte[] content) {
}
