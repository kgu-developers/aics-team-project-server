package kgu.developers.domain.course.exception;

import static kgu.developers.domain.course.exception.CourseDomainExceptionCode.COURSE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class CourseNotFoundException extends CustomException {
    public CourseNotFoundException() {
        super(COURSE_NOT_FOUND);
    }
}
