package ua.org.kpik.simple_timetable_generator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;
import ua.org.kpik.simple_timetable_generator.repository.LessonRepository;
import ua.org.kpik.simple_timetable_generator.service.TimetableService;
import ua.org.kpik.simple_timetable_generator.solver.Timetable;

import java.util.Comparator;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {
    private final TimetableService timetableService;
    private final LessonRepository lessonRepository;

    @PostMapping("/generate")
    public String generateTimetable() {
        timetableService.solve();
        return "Generation started wait few seconds...";
    }

    @GetMapping
    public List<Lesson> getTimetable() {
        return lessonRepository.findAll();
    }

    @GetMapping("/formatted")
    public Map<String, Map<Integer, List<Lesson>>> getFormattedTimetable() {
        List<Lesson> allLessons = lessonRepository.findAll();

        // 1. Спочатку сортуємо всі пари по днях (1-5), а потім по номеру пари (1-4)
        allLessons.sort(Comparator.comparing(Lesson::getDayOfWeek)
                .thenComparing(Lesson::getLessonNumber));

        // 2. Групуємо: спочатку по Назві Групи, потім по Дню Тижня
        return allLessons.stream()
                .collect(Collectors.groupingBy(
                        lesson -> lesson.getGroup().getGroupName(), // Перший рівень: Група
                        LinkedHashMap::new, // Зберігаємо порядок сортування груп
                        Collectors.groupingBy(
                                Lesson::getDayOfWeek, // Другий рівень: День тижня
                                LinkedHashMap::new, // Зберігаємо порядок днів
                                Collectors.toList() // Третій рівень: Список пар на цей день
                        )
                ));
    }

}
