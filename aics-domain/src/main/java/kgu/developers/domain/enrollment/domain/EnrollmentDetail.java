package kgu.developers.domain.enrollment.domain;

import kgu.developers.domain.user.domain.User;

public record EnrollmentDetail(Enrollment enrollment, User user) {
}
