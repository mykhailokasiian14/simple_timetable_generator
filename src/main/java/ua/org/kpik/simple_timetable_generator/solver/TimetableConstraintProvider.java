package ua.org.kpik.simple_timetable_generator.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ua.org.kpik.simple_timetable_generator.enaum.LessonWeekType;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;

public class TimetableConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                auditoryConflict(constraintFactory),
                diversifyLessonTimes(constraintFactory),
                oneSubjectPerDay(constraintFactory),
                noConsecutiveSameSubject(constraintFactory),
                teacherRoomSharingPenalty(constraintFactory),
                noGapsBetweenLessons(constraintFactory),
                shortFridaysAndWednesdays(constraintFactory),
        };
    }

    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .filter((lesson1, lesson2) -> {
                    // Якщо хоча б одна з пар йде ЩОТИЖНЯ - це конфлікт накладки
                    if (lesson1.getLessonWeekType() == LessonWeekType.EVERY_WEEK || lesson2.getLessonWeekType() == LessonWeekType.EVERY_WEEK) {
                        return true;
                    }
                    // Якщо обидві пари на одному тижні (обидві чисельник або обидві знаменник) - це конфлікт
                    if (lesson1.getLessonWeekType() == lesson2.getLessonWeekType()) {
                        return true;
                    }
                    // Якщо одна чисельник, а інша знаменник - все чотко, пропускаємо!
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict!");
    }

    private Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .filter((lesson1, lesson2) -> {
                    // Якщо хоча б одна з пар йде ЩОТИЖНЯ - це конфлікт накладки
                    if (lesson1.getLessonWeekType() == LessonWeekType.EVERY_WEEK || lesson2.getLessonWeekType() == LessonWeekType.EVERY_WEEK) {
                        return true;
                    }
                    // Якщо обидві пари на одному тижні (обидві чисельник або обидві знаменник) - це конфлікт
                    if (lesson1.getLessonWeekType() == lesson2.getLessonWeekType()) {
                        return true;
                    }
                    // Якщо одна чисельник, а інша знаменник - все чотко, пропускаємо!
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group conflict!");
    }

    private Constraint auditoryConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getAuditory),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .filter((lesson1, lesson2) -> {
                    // Якщо хоча б одна з пар йде ЩОТИЖНЯ - це конфлікт накладки
                    if (lesson1.getLessonWeekType() == LessonWeekType.EVERY_WEEK || lesson2.getLessonWeekType() == LessonWeekType.EVERY_WEEK) {
                        return true;
                    }
                    // Якщо обидві пари на одному тижні (обидві чисельник або обидві знаменник) - це конфлікт
                    if (lesson1.getLessonWeekType() == lesson2.getLessonWeekType()) {
                        return true;
                    }
                    // Якщо одна чисельник, а інша знаменник - все чотко, пропускаємо!
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Auditory conflict!");
    }

    private Constraint diversifyLessonTimes(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(Lesson::getLessonNumber))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Undiversified lesson times!");
    }

    private Constraint oneSubjectPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getSubject))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("One subject per day!");
    }

    private Constraint noConsecutiveSameSubject(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getSubject))
                .filter((lesson1, lesson2) -> Math.abs(lesson1.getLessonNumber() - lesson2.getLessonNumber()) == 1)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("There is no same subject!");
    }

    private Constraint teacherRoomSharingPenalty(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getAuditory))
                .filter((lesson1, lesson2) -> !lesson1.getTeacher().getId().equals(lesson2.getTeacher().getId()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("room sharing penalty!");
    }

    private Constraint noGapsBetweenLessons(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.lessThan(Lesson::getLessonNumber))

                .ifNotExists(Lesson.class,
                        Joiners.equal((l1, l2) -> l1.getGroup(), Lesson::getGroup),
                        Joiners.equal((l1, l2) -> l1.getDayOfWeek(), Lesson::getDayOfWeek),
                        Joiners.greaterThan((l1, l2) -> l1.getLessonNumber(), Lesson::getLessonNumber),
                        Joiners.lessThan((l1, l2) -> l2.getLessonNumber(), Lesson::getLessonNumber))

                .filter((l1, l2) -> l2.getLessonNumber() - l1.getLessonNumber() > 1)

                .penalize(HardSoftScore.ofSoft(2),
                        (l1, l2) -> l2.getLessonNumber() - l1.getLessonNumber() - 1)
                .asConstraint("No gaps between lessons!");
    }

    private Constraint shortFridaysAndWednesdays(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getDayOfWeek() == 5 || lesson.getDayOfWeek() == 3)
                .filter(lesson -> lesson.getLessonNumber() > 3)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("max 3 lessons on friday and wednesday!");
    }
}
