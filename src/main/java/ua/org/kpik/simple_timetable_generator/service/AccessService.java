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
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AccessService {
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingLoadRepository teachingLoadRepository;

    private static final int DEFAULT_WEEKS_FALLBACK = 15;

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

            try (Connection conn = DriverManager.getConnection(dbURL)) {

                Map<String, Integer> groupWeeksMap = loadGroupWeeksMap(conn, semester);

                /*  Here are the editable fields from the Access database:*/
                String sql = "SELECT " +
                        "g.KodGr AS GroupName, " +
                        "s.NazvaDysc AS SubjectName, " +
                        "t.Prizv AS TeacherName, " +
                        "l." + rightSemHoursColumn + " AS ActiveHours " +
                        "FROM РозподілГодин l " +
                        "JOIN Групи g ON l.Grupa = g.NomGr " +
                        "JOIN Дисципліни s ON l.Dysc = s.NomDysc " +
                        "JOIN Викладачі t ON l.Vykladach = t.NomVykl";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    while (rs.next()) {
                        int semesterHours = rs.getInt("ActiveHours");

                        if (rs.wasNull() || semesterHours <= 0) {
                            continue;
                        }

                        String groupName = rs.getString("GroupName").trim();
                        String subjectName = rs.getString("SubjectName").trim();
                        String teacherName = rs.getString("TeacherName").trim();

                        // Look up the specific number of weeks for this group
                        int groupWeeks = groupWeeksMap.getOrDefault(groupName, DEFAULT_WEEKS_FALLBACK);
                        double pairsPerWeek = (double) semesterHours / (groupWeeks * 2);

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
            }
            return "Successful rows added: " + rowsAdded + " for subjects in semester " + semester;
        } catch (Exception e) {
            throw new RuntimeException("Exception while reading Access: "+ e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


     // Also changeable fields from accdb

    private Map<String, Integer> loadGroupWeeksMap(Connection conn, int semester) {
        Map<String, Integer> groupWeeksMap = new HashMap<>();
        String weeksColumn = (semester == 1) ? "[1 семестр]" : "[2 семестр]";
        String sql = "SELECT [Група], " + weeksColumn + " AS Weeks FROM [Лист1]";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String groupName = rs.getString("Група");
                int weeks = rs.getInt("Weeks");

                if (groupName != null && !rs.wasNull() && weeks > 0) {
                    groupWeeksMap.put(groupName.trim(), weeks);
                }
            }
        } catch (Exception e) {
            System.err.println("WARNING: Could not read [Group_Weeks] table: " + e.getMessage());
            System.err.println("Falling back to default " + DEFAULT_WEEKS_FALLBACK + " weeks for all groups.");
        }

        return groupWeeksMap;
    }
}

