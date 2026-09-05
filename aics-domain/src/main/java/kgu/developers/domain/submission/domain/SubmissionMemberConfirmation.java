package kgu.developers.domain.submission.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SubmissionMemberConfirmation {
    private Long id;
    private Long submissionId;
    private String userId;
    private int version;
    private LocalDateTime confirmedAt;

    // 재제출로 새 버전이 올라오면, 예전 버전에 대한 확인은 지금 버전엔 유효하지 않다.
    // "확인함" 자체는 별도 필드가 아니라 이 행이 존재하고 confirmsVersion이 참인 것으로 표현한다
    // (KD3-161 — 체크박스 2개+한줄소감 폐기, 확인 버튼 하나로 단순화).
    public boolean confirmsVersion(int version) {
        return this.version == version;
    }
}
