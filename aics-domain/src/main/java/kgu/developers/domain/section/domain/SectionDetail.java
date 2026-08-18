package kgu.developers.domain.section.domain;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.user.domain.User;

/**
 * 분반 조회 결과. 강좌/교수를 함께 fetch join 해서 담는다.
 */
public record SectionDetail(Section section, Course course, User professor) {
}
