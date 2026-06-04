package ua.org.kpik.simple_timetable_generator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.entity.Group;
import ua.org.kpik.simple_timetable_generator.entity.Subject;
import ua.org.kpik.simple_timetable_generator.entity.Teacher;
import ua.org.kpik.simple_timetable_generator.entity.TeachingLoad;
import ua.org.kpik.simple_timetable_generator.repository.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


@Service
@RequiredArgsConstructor
public class AccessService {
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingLoadRepository teachingLoadRepository;

    @Transactional
    public String parseAccess(MultipartFile file, int semester) {
        File tempFile = null;
        try{
            tempFile = File.createTempFile("uploaded_access_file", ".accdb");
            file.transferTo(tempFile);
            String safePath = tempFile.getAbsolutePath().replace("\\", "/");
            String dbURL = "jdbc:ucanaccess://" + safePath;
            int rowsAdded = 0;
            String rightSemHoursColumn = (semester == 1) ? "[1sem]" : "[2sem]";
            int semesterWeeks = (semester == 1) ? 15 : 22;

            /* УВАГА! ТУТ ЗМІННІ НАЗВИ З БАЗИ ACCESS:
             * 1. "MainTable" -> на назву таблиці зі скріна (де лежать NomDysc, 1sem, 2sem).
             * 2. "GroupsTable", "SubjectsTable", "TeachersTable" -> на їхні реальні назви.
             * 3. "g.Name", "s.Name", "t.Name" -> на реальні назви колонок з текстом.
             */
            String sql = "SELECT " +
                    "g.KodGr AS GroupName, " +
                    "s.NazvaDysc AS SubjectName, " +
                    "t.Prizv AS TeacherName, " +
                    "l." + rightSemHoursColumn + " AS ActiveHours " +
                    "FROM РозподілГодин l " +
                    "JOIN Групи g ON l.Grupa = g.NomGr " +
                    "JOIN Дисципліни s ON l.Dysc = s.NomDysc " +
                    "JOIN Викладачі t ON l.Vykladach = t.NomVykl";

            try (Connection conn = DriverManager.getConnection(dbURL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int semesterHours = rs.getInt("ActiveHours");

                    if (rs.wasNull() || semesterHours <= 0) {
                        continue;
                    }
                    double pairsPerWeek = (double) semesterHours / (semesterWeeks * 2);

                    String groupName = rs.getString("GroupName").trim();
                    String subjectName = rs.getString("SubjectName").trim();
                    String teacherName = rs.getString("TeacherName").trim();

                    //saving
                    Group group = groupRepository.findByGroupName(groupName)
                            .orElseGet(() -> {
                                Group g = new Group();
                                g.setGroupName(groupName);
                                return groupRepository.save(g);
                            });

                    Subject subject = subjectRepository.findBySubjectName(subjectName)
                            .orElseGet(() -> {
                                Subject s = new Subject();
                                s.setSubjectName(subjectName);
                                return subjectRepository.save(s);
                            });

                    Teacher teacher = teacherRepository.findByLastName(teacherName)
                            .orElseGet(() -> {
                                Teacher t = new Teacher();
                                t.setLastName(teacherName);
                                return teacherRepository.save(t);
                            });

                    if (!teacher.getSubjects().contains(subject)) {
                        teacher.getSubjects().add(subject);
                        teacherRepository.save(teacher);
                    }

                    TeachingLoad load = new TeachingLoad();
                    load.setGroup(group);
                    load.setSubject(subject);
                    load.setTeacher(teacher);
                    load.setLessonsPerWeek(pairsPerWeek);
                    teachingLoadRepository.save(load);

                    rowsAdded++;
                }
            }
            return "Successful rows added: " + rowsAdded + "for subjects in semester" + semester;
        } catch (Exception e) {
            throw new RuntimeException("Exception while reading Access: "+ e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
