package ua.org.kpik.simple_timetable_generator.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import org.junit.jupiter.api.Test;
import ua.org.kpik.simple_timetable_generator.enaum.LessonWeekType;
import ua.org.kpik.simple_timetable_generator.entity.*;

import java.util.HashSet;

class TimetableConstraintProviderTest {

    ConstraintVerifier<TimetableConstraintProvider, Timetable> constraintVerifier =
            ConstraintVerifier.build(new TimetableConstraintProvider(), Timetable.class, Lesson.class);

    @Test
    void sameSubjectTeacherDifferentAuditory_penalized() {
        Group group = new Group(1L, "КН-21");
        Subject subject = new Subject(1L, "Комп'ютерна логіка", 30);
        Teacher teacher = new Teacher(1L, "Пилюйко О.А.", new HashSet<>());
        Auditory room11 = new Auditory(11L, "11");
        Auditory room18 = new Auditory(18L, "18");

        Lesson lesson1 = new Lesson(1L, subject, group, teacher, room11, 1, 1, LessonWeekType.NUMERATOR);
        Lesson lesson2 = new Lesson(2L, subject, group, teacher, room18, 1, 1, LessonWeekType.DENOMINATOR);

        constraintVerifier.verifyThat(TimetableConstraintProvider::sameSubjectTeacherAuditoryConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }

    @Test
    void sameSubjectTeacherSameAuditory_notPenalized() {
        Group group = new Group(1L, "КН-21");
        Subject subject = new Subject(1L, "Телекомунікац.системи", 30);
        Teacher teacher = new Teacher(1L, "Дюмін Д.М.", new HashSet<>());
        Auditory room16 = new Auditory(16L, "16");

        Lesson lesson1 = new Lesson(1L, subject, group, teacher, room16, 1, 1, LessonWeekType.NUMERATOR);
        Lesson lesson2 = new Lesson(2L, subject, group, teacher, room16, 1, 1, LessonWeekType.DENOMINATOR);

        constraintVerifier.verifyThat(TimetableConstraintProvider::sameSubjectTeacherAuditoryConflict)
                .given(lesson1, lesson2)
                .penalizesBy(0);
    }

    @Test
    void testTeacherConflict() {
        Teacher teacher = new Teacher(1L, "Пилюйко О.А.", new HashSet<>());
        Auditory room = new Auditory(1L, "1");
        Lesson lesson1 = new Lesson(1L, null, null, teacher, room, 1, 1, LessonWeekType.EVERY_WEEK);
        Lesson lesson2 = new Lesson(2L, null, null, teacher, room, 1, 1, LessonWeekType.EVERY_WEEK);

        constraintVerifier.verifyThat(TimetableConstraintProvider::teacherConflict)
                .given(lesson1, lesson2)
                .penalizesBy(1);
    }
}
