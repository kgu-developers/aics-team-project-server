package kgu.developers.domain.topicVote.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TopicVoteDomainExceptionCode implements ExceptionCode {
    TOPIC_VOTE_NOT_FOUND(NOT_FOUND, "해당 투표를 찾을 수 없습니다."),
    TOPIC_VOTE_CANDIDATE_CHANGED(BAD_REQUEST, "투표 후보가 변경되어 취소할 수 없습니다"),
    TOPIC_VOTE_CONFLICT(BAD_REQUEST, "동시 투표 충돌로 인해 기존 투표를 찾을 수 없습니다"),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}
