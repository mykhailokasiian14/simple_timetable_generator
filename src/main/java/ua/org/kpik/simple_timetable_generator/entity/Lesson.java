package ua.org.kpik.simple_timetable_generator.entity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import jakarta.persistence.*;
import lombok.*;
import ua.org.kpik.simple_timetable_generator.enaum.LessonWeekType;

@Entity
@Table(name = "lessons")
@PlanningEntity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PlanningId
    @EqualsAndHashCode.Include
    private Long lessonId;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "auditory_id")
    @PlanningVariable(valueRangeProviderRefs = "auditoryRange")
    private Auditory auditory;

    @PlanningVariable(valueRangeProviderRefs = "dayOfWeekRange")
    private Integer dayOfWeek;

    @PlanningVariable(valueRangeProviderRefs = "lessonNumberRange")
    private Integer lessonNumber;

    @Enumerated(EnumType.STRING)
    private LessonWeekType lessonWeekType = LessonWeekType.EVERY_WEEK;
}
