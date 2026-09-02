package kgu.developers.domain.enrollment.application.query;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentDetail;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentQueryService {
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;

    public EnrollmentDetail getEnrollment(Long sectionId, String studentNumber) {
        Enrollment enrollment = enrollmentRepository.findBySectionIdAndUserId(sectionId, studentNumber)
                .orElseThrow(EnrollmentNotFoundException::new);
        User user = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(UserNotFoundException::new);
        return new EnrollmentDetail(enrollment, user);
    }

    public List<EnrollmentDetail> getEnrollmentsBySectionId(Long sectionId) {
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }

        List<Enrollment> enrollments = enrollmentRepository.findAllBySectionId(sectionId);
        List<String> studentNumbers = enrollments.stream().map(Enrollment::getUserId).toList();
        Map<String, User> users = userRepository.findAllByStudentNumberIn(studentNumbers).stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));

        // 사용자가 삭제된 수강 정보는 명단에서 제외한다
        return enrollments.stream()
                .filter(enrollment -> users.containsKey(enrollment.getUserId()))
                .map(enrollment -> new EnrollmentDetail(enrollment, users.get(enrollment.getUserId())))
                .toList();
    }

    public List<EnrollmentDetail> getEnrollmentsByStudentNumber(String studentNumber) {
        // 사용자가 삭제된 수강 정보는 명단에서 제외한다
        return userRepository.findByStudentNumber(studentNumber)
                .map(user -> enrollmentRepository.findAllByUserId(studentNumber).stream()
                        .map(enrollment -> new EnrollmentDetail(enrollment, user))
                        .toList())
                .orElseGet(List::of);
    }
}
