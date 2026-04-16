package ua.org.kpik.simple_timetable_generator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.repository.*;



@Service
@RequiredArgsConstructor
public class AccessService {
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingLoadRepository teachingLoadRepository;
    private final LessonRepository lessonRepository;

    public String parseAndSaveAccess(MultipartFile file) {
        return  null;
    }
}
