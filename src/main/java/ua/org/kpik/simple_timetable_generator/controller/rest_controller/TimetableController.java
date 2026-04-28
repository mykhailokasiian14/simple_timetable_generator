package ua.org.kpik.simple_timetable_generator.controller.rest_controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;
import ua.org.kpik.simple_timetable_generator.repository.LessonRepository;
import ua.org.kpik.simple_timetable_generator.service.TimetableService;

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

        allLessons.sort(Comparator.comparing(Lesson::getDayOfWeek)
                .thenComparing(Lesson::getLessonNumber));

        return allLessons.stream()
                .collect(Collectors.groupingBy(
                        lesson -> lesson.getGroup().getGroupName(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                Lesson::getDayOfWeek,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));
    }

}
