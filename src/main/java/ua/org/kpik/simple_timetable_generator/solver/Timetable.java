package ua.org.kpik.simple_timetable_generator.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ua.org.kpik.simple_timetable_generator.entity.*;

import java.util.ArrayList;
import java.util.List;

@PlanningSolution
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Timetable {
    @ValueRangeProvider(id = "auditoryRange")
    @ProblemFactCollectionProperty
    private List<Auditory> auditories;

    @ValueRangeProvider(id = "dayOfWeekRange")
    @ProblemFactCollectionProperty
    private List<Integer> dayOfWeeks;

    @ValueRangeProvider(id= "lessonNumberRange")
    @ProblemFactCollectionProperty
    private List<Integer> lessonNumbers;

    @ProblemFactCollectionProperty
    private List<Group> groups= new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<Teacher> teachers= new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<Subject> subjects= new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<TeachingLoad> teachingLoads = new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<Lesson> lessons;

    @PlanningScore
    private HardSoftScore score;
}
