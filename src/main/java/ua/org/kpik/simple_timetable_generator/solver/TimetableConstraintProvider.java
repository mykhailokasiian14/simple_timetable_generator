package ua.org.kpik.simple_timetable_generator.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;

public class TimetableConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                teacherConflict(constraintFactory),
                groupConflict(constraintFactory),
                auditoryConflict(constraintFactory)
        };
    }

    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTeacher),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict!");
    }

    private Constraint groupConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroup),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group conflict!");
    }

    private Constraint auditoryConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getAuditory),
                        Joiners.equal(Lesson::getDayOfWeek),
                        Joiners.equal(Lesson::getLessonNumber))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Auditory conflict!");
    }
}
