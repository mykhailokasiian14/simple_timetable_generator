package ua.org.kpik.simple_timetable_generator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ua.org.kpik.simple_timetable_generator.entity.Lesson;
import ua.org.kpik.simple_timetable_generator.repository.LessonRepository;
import ua.org.kpik.simple_timetable_generator.service.TimetableService;
import ua.org.kpik.simple_timetable_generator.solver.Timetable;

import java.util.List;

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

}
