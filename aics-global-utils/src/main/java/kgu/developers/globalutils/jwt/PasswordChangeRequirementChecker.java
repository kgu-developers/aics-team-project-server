package kgu.developers.globalutils.jwt;

public interface PasswordChangeRequirementChecker {
    boolean isRequired(String studentNumber);
}
