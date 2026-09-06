package kgu.developers.domain.topicCandidate.exception;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TopicCandidateDomainExceptionCode implements ExceptionCode {
    TOPIC_CANDIDATE_NOT_FOUND(NOT_FOUND, "해당 주제 후보를 찾을 수 없습니다."),
    DUPLICATE_TOPIC_CANDIDATE_TITLE(CONFLICT, "같은 팀에 이미 같은 제목의 주제 후보가 있습니다."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
