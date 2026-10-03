package kgu.developers.domain.submission.application.query;

public record SectionArtifactMember(String studentNumber, String name) {
    private static final String WITHDRAWN_USER_NAME = "(탈퇴한 사용자)";

    public String displayName() {
        if (name == null || name.isBlank()) {
            return WITHDRAWN_USER_NAME;
        }
        return name;
    }
}
