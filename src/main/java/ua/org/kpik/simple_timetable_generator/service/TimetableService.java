package ua.org.kpik.simple_timetable_generator.service;

import ai.timefold.solver.core.api.solver.SolverManager;
import org.springframework.transaction.annotation.Transactional;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;
import ua.org.kpik.simple_timetable_generator.entity.TeachingLoad;
import ua.org.kpik.simple_timetable_generator.repository.AuditoryRepository;
import ua.org.kpik.simple_timetable_generator.repository.LessonRepository;
import ua.org.kpik.simple_timetable_generator.repository.TeachingLoadRepository;
import ua.org.kpik.simple_timetable_generator.solver.Timetable;

import java.util.List;

public class TimetableService {
    private  TeachingLoadRepository loadRepository;
    private  LessonRepository lessonRepository;
    private  AuditoryRepository auditoryRepository;

    private SolverManager<Timetable, Long> solverManager;

    @Transactional
    public void generateEmptyLessons() {
        lessonRepository.deleteAll();

        List<TeachingLoad> loads = loadRepository.findAll();

        for (TeachingLoad load : loads) {
            for (int i = 0; i < load.getHoursPerWeek(); i++) {
                Lesson lesson = new Lesson();
                lesson.setTeacher(load.getTeacher());
                lesson.setGroup(load.getGroup());
                lesson.setSubject(load.getSubject());

                lessonRepository.save(lesson);
            }
        }
    }

    public Timetable buildProblem() {
        Timetable problem = new Timetable();

        problem.setAuditories(auditoryRepository.findAll());

        problem.setDayOfWeeks(List.of(1, 2, 3, 4, 5));

        problem.setLessonNumbers(List.of(1, 2, 3, 4));

        problem.setLessons(lessonRepository.findAll());

        return problem;
    }

    public void solve() {
        System.out.println("Creating lessons templates...");
        generateEmptyLessons();

        System.out.println("Containerizing data...");
        Timetable problem = buildProblem();

        System.out.println("Starting timatable generation...");
        solverManager.solveBuilder()
                .withProblemId(1L)
                .withProblemFinder(id -> problem)
                .withBestSolutionConsumer(this::saveSolution)
                .run();
    }

    @Transactional
    protected void saveSolution(Timetable solution) {
        System.out.println("Best timetable found. Saving solution...");
        lessonRepository.saveAll(solution.getLessons());
    }
}
