package kgu.developers.domain.course.domain;

import lombok.*;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Course {
    private Long id;
    private String name;
    private int year;
    private SemesterType semester;
    private StatusType status;

    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected LocalDateTime deletedAt;

    public static Course create(String name, int year, SemesterType semester, StatusType status) {
        return Course.builder()
                .name(name)
                .year(year)
                .semester(semester)
                .status(status)
                .build();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateYear(int year) {
        this.year = year;
    }

    public void updateSemester(SemesterType semester) {
        this.semester = semester;
    }

    public void updateStatus(StatusType status) {
        this.status = status;
    }

    public void delete() {
        deletedAt = LocalDateTime.now();
    }
}
