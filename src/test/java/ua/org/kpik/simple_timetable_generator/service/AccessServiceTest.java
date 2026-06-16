package ua.org.kpik.simple_timetable_generator.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ua.org.kpik.simple_timetable_generator.entity.Group;
import ua.org.kpik.simple_timetable_generator.entity.Subject;
import ua.org.kpik.simple_timetable_generator.entity.Teacher;
import ua.org.kpik.simple_timetable_generator.entity.TeachingLoad;
import ua.org.kpik.simple_timetable_generator.repository.*;

import java.sql.*;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 Uses MockedStatic for DriverManager to mock the JDBC layer entirely.
 Tests business logic: pairsPerWeek calculation, Group_Weeks lookup,
 fallback behavior, entity reuse, and row skipping.
 */
@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private TeachingLoadRepository teachingLoadRepository;

    @InjectMocks
    private AccessService accessService;

    private MockedStatic<DriverManager> driverManagerMock;
    private Connection mockConnection;

    @BeforeEach
    void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(() -> DriverManager.getConnection(anyString()))
                .thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() {
        driverManagerMock.close();
    }

    //Helpers

    private MockMultipartFile dummyFile() {
        return new MockMultipartFile("file", "test.accdb",
                "application/msaccess", new byte[]{0x00, 0x01, 0x02});
    }

    /**
     * Creates a mock Statement+ResultSet for the Group_Weeks query.
     * Rows: each String[] is {groupName, weeks}.
     */
    private Statement mockGroupWeeksStatement(String[][] rows) throws Exception {
        Statement stmt = mock(Statement.class, "weeksStmt");
        ResultSet rs = mock(ResultSet.class, "weeksRs");

        if (rows == null || rows.length == 0) {
            when(rs.next()).thenReturn(false);
        } else if (rows.length == 1) {
            when(rs.next()).thenReturn(true, false);
            when(rs.getString("Group_Name")).thenReturn(rows[0][0]);
            when(rs.getInt("Weeks")).thenReturn(Integer.parseInt(rows[0][1]));
            when(rs.wasNull()).thenReturn(false);
        } else {
            // Build arrays for multi-row
            Boolean[] nexts = new Boolean[rows.length + 1];
            for (int i = 0; i < rows.length; i++) nexts[i] = true;
            nexts[rows.length] = false;
            when(rs.next()).thenReturn(true, java.util.Arrays.copyOfRange(nexts, 1, nexts.length));

            String[] names = new String[rows.length];
            Integer[] weeks = new Integer[rows.length];
            Boolean[] nulls = new Boolean[rows.length];
            for (int i = 0; i < rows.length; i++) {
                names[i] = rows[i][0];
                weeks[i] = Integer.parseInt(rows[i][1]);
                nulls[i] = false;
            }
            when(rs.getString("Group_Name")).thenReturn(names[0],
                    java.util.Arrays.copyOfRange(names, 1, names.length));
            when(rs.getInt("Weeks")).thenReturn(weeks[0],
                    java.util.Arrays.copyOfRange(weeks, 1, weeks.length));
            when(rs.wasNull()).thenReturn(false,
                    java.util.Arrays.copyOfRange(nulls, 1, nulls.length));
        }

        when(stmt.executeQuery(anyString())).thenReturn(rs);
        return stmt;
    }

    /**
     * Creates a mock Statement+ResultSet for the Group_Weeks query that throws.
     */
    private Statement mockGroupWeeksStatementMissing() throws Exception {
        Statement stmt = mock(Statement.class, "weeksStmtMissing");
        // Pre-create exception outside Mockito context to avoid DriverManager.getLogWriter() interference
        RuntimeException ex = new RuntimeException("Table [Group_Weeks] not found");
        when(stmt.executeQuery(anyString())).thenThrow(ex);
        return stmt;
    }

    /**
     * Creates a mock Statement+ResultSet for the main teaching load query.
     * Rows: each String[] is {groupName, subjectName, teacherName, hours}.
     */
    private Statement mockMainStatement(String[][] rows) throws Exception {
        Statement stmt = mock(Statement.class, "mainStmt");
        ResultSet rs = mock(ResultSet.class, "mainRs");

        if (rows == null || rows.length == 0) {
            when(rs.next()).thenReturn(false);
        } else if (rows.length == 1) {
            when(rs.next()).thenReturn(true, false);
            // Use lenient for getString stubs — they won't be reached if hours<=0 (row skipped)
            lenient().when(rs.getString("GroupName")).thenReturn(rows[0][0]);
            lenient().when(rs.getString("SubjectName")).thenReturn(rows[0][1]);
            lenient().when(rs.getString("TeacherName")).thenReturn(rows[0][2]);
            int hours = Integer.parseInt(rows[0][3]);
            when(rs.getInt("ActiveHours")).thenReturn(hours);
            when(rs.wasNull()).thenReturn(hours <= 0);
        } else {
            Boolean[] nexts = new Boolean[rows.length + 1];
            for (int i = 0; i < rows.length; i++) nexts[i] = true;
            nexts[rows.length] = false;
            when(rs.next()).thenReturn(true, java.util.Arrays.copyOfRange(nexts, 1, nexts.length));

            String[] groups = new String[rows.length];
            String[] subjects = new String[rows.length];
            String[] teachers = new String[rows.length];
            Integer[] hours = new Integer[rows.length];
            Boolean[] nulls = new Boolean[rows.length];
            for (int i = 0; i < rows.length; i++) {
                groups[i] = rows[i][0];
                subjects[i] = rows[i][1];
                teachers[i] = rows[i][2];
                hours[i] = Integer.parseInt(rows[i][3]);
                nulls[i] = hours[i] <= 0;
            }
            when(rs.getString("GroupName")).thenReturn(groups[0],
                    java.util.Arrays.copyOfRange(groups, 1, groups.length));
            when(rs.getString("SubjectName")).thenReturn(subjects[0],
                    java.util.Arrays.copyOfRange(subjects, 1, subjects.length));
            when(rs.getString("TeacherName")).thenReturn(teachers[0],
                    java.util.Arrays.copyOfRange(teachers, 1, teachers.length));
            when(rs.getInt("ActiveHours")).thenReturn(hours[0],
                    java.util.Arrays.copyOfRange(hours, 1, hours.length));
            when(rs.wasNull()).thenReturn(nulls[0],
                    java.util.Arrays.copyOfRange(nulls, 1, nulls.length));
        }

        when(stmt.executeQuery(anyString())).thenReturn(rs);
        return stmt;
    }

    /**
     * Wires two statements to the connection: first call -> weeksStmt, second -> mainStmt.
     */
    private void wireStatements(Statement weeksStmt, Statement mainStmt) throws Exception {
        when(mockConnection.createStatement())
                .thenReturn(weeksStmt)
                .thenReturn(mainStmt);
    }

    private void setupDefaultRepos() {
        when(groupRepository.findByGroupName(anyString())).thenReturn(Optional.empty());
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            g.setId(1L);
            return g;
        });

        when(subjectRepository.findBySubjectName(anyString())).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        when(teacherRepository.findByLastName(anyString())).thenReturn(Optional.empty());
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher t = inv.getArgument(0);
            t.setId(1L);
            t.setSubjects(new HashSet<>());
            return t;
        });

        when(teachingLoadRepository.save(any(TeachingLoad.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== Tests ====================

    @Test
    @DisplayName("Semester 1: uses Sem1_Weeks from Group_Weeks for pairsPerWeek calculation")
    void parseAccess_semester1_usesGroupWeeksTable() throws Exception {
        // КН-11: 10 weeks, 60 hours -> 60 / (10*2) = 3.0
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}}),
                mockMainStatement(new String[][]{{"КН-11", "Математика", "Петренко", "60"}})
        );
        setupDefaultRepos();

        String result = accessService.parseAccess(dummyFile(), 1);

        assertTrue(result.contains("1"));
        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(3.0, captor.getValue().getLessonsPerWeek(), 0.001,
                "pairsPerWeek should be 60 / (10 * 2) = 3.0");
    }

    @Test
    @DisplayName("Semester 2: uses Sem2_Weeks from Group_Weeks for pairsPerWeek calculation")
    void parseAccess_semester2_usesSem2Weeks() throws Exception {
        // КН-41: 8 weeks in sem2, 32 hours -> 32 / (8*2) = 2.0
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-41", "8"}}),
                mockMainStatement(new String[][]{{"КН-41", "Фізика", "Іваненко", "32"}})
        );
        setupDefaultRepos();

        String result = accessService.parseAccess(dummyFile(), 2);

        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(2.0, captor.getValue().getLessonsPerWeek(), 0.001,
                "pairsPerWeek should be 32 / (8 * 2) = 2.0");
    }

    @Test
    @DisplayName("Fallback to 15 weeks when Group_Weeks table does not exist")
    void parseAccess_noGroupWeeksTable_fallsBackToDefault() throws Exception {
        // No Group_Weeks table -> fallback 15 weeks, 30 hours -> 30 / (15*2) = 1.0
        wireStatements(
                mockGroupWeeksStatementMissing(),
                mockMainStatement(new String[][]{{"КН-21", "Хімія", "Сидоренко", "30"}})
        );
        setupDefaultRepos();

        accessService.parseAccess(dummyFile(), 1);

        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(1.0, captor.getValue().getLessonsPerWeek(), 0.001,
                "Should fall back to 15 weeks: 30 / (15 * 2) = 1.0");
    }

    @Test
    @DisplayName("Fallback to 15 weeks when group not found in Group_Weeks table")
    void parseAccess_groupNotInGroupWeeks_fallsBackToDefault() throws Exception {
        // Group_Weeks has "КН-11" but load is for "КН-31"
        // 45 / (15*2) = 1.5
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}}),
                mockMainStatement(new String[][]{{"КН-31", "Біологія", "Коваленко", "45"}})
        );
        setupDefaultRepos();

        accessService.parseAccess(dummyFile(), 1);

        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(1.5, captor.getValue().getLessonsPerWeek(), 0.001,
                "Unknown group defaults to 15 weeks: 45 / (15 * 2) = 1.5");
    }

    @Test
    @DisplayName("Rows with zero hours are skipped")
    void parseAccess_zeroHours_skipsRow() throws Exception {
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}}),
                mockMainStatement(new String[][]{{"КН-11", "Пуста", "Тестовий", "0"}})
        );

        String result = accessService.parseAccess(dummyFile(), 1);

        verify(teachingLoadRepository, never()).save(any(TeachingLoad.class));
        assertTrue(result.contains("0"), "No rows should be added for 0-hour subjects");
    }

    @Test
    @DisplayName("Fractional pairsPerWeek: 0.5 for alternating weeks")
    void parseAccess_fractionalPairsPerWeek() throws Exception {
        // 15 hours / (15*2) = 0.5
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-21", "15"}}),
                mockMainStatement(new String[][]{{"КН-21", "Філософія", "Бондаренко", "15"}})
        );
        setupDefaultRepos();

        accessService.parseAccess(dummyFile(), 1);

        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(0.5, captor.getValue().getLessonsPerWeek(), 0.001,
                "pairsPerWeek should be 15 / (15 * 2) = 0.5");
    }

    @Test
    @DisplayName("Multiple groups with different week counts are calculated independently")
    void parseAccess_multipleGroupsDifferentWeeks() throws Exception {
        // КН-11: 10 weeks, 60 hours -> 3.0
        // КН-41: 8 weeks, 32 hours  -> 2.0
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}, {"КН-41", "8"}}),
                mockMainStatement(new String[][]{
                        {"КН-11", "Алгебра", "Шевченко", "60"},
                        {"КН-41", "Геометрія", "Франко", "32"}
                })
        );
        setupDefaultRepos();

        accessService.parseAccess(dummyFile(), 1);

        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(2)).save(captor.capture());

        var saved = captor.getAllValues();
        TeachingLoad loadKN11 = saved.stream()
                .filter(l -> l.getGroup().getGroupName().equals("КН-11"))
                .findFirst().orElseThrow();
        TeachingLoad loadKN41 = saved.stream()
                .filter(l -> l.getGroup().getGroupName().equals("КН-41"))
                .findFirst().orElseThrow();

        assertEquals(3.0, loadKN11.getLessonsPerWeek(), 0.001, "КН-11: 60 / (10*2) = 3.0");
        assertEquals(2.0, loadKN41.getLessonsPerWeek(), 0.001, "КН-41: 32 / (8*2) = 2.0");
    }

    @Test
    @DisplayName("Existing group in repository is reused, not duplicated")
    void parseAccess_existingGroupIsReused() throws Exception {
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}}),
                mockMainStatement(new String[][]{{"КН-11", "Інформатика", "Мельник", "40"}})
        );

        // Pre-existing group
        Group existingGroup = new Group();
        existingGroup.setId(42L);
        existingGroup.setGroupName("КН-11");
        when(groupRepository.findByGroupName("КН-11")).thenReturn(Optional.of(existingGroup));

        when(subjectRepository.findBySubjectName(anyString())).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(teacherRepository.findByLastName(anyString())).thenReturn(Optional.empty());
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher t = inv.getArgument(0);
            t.setId(1L);
            t.setSubjects(new HashSet<>());
            return t;
        });
        when(teachingLoadRepository.save(any(TeachingLoad.class))).thenAnswer(inv -> inv.getArgument(0));

        accessService.parseAccess(dummyFile(), 1);

        verify(groupRepository, never()).save(any(Group.class));
        ArgumentCaptor<TeachingLoad> captor = ArgumentCaptor.forClass(TeachingLoad.class);
        verify(teachingLoadRepository, times(1)).save(captor.capture());
        assertEquals(42L, captor.getValue().getGroup().getId(),
                "Should reuse existing group with ID 42");
    }

    @Test
    @DisplayName("Return message contains row count and semester info")
    void parseAccess_returnMessageFormat() throws Exception {
        wireStatements(
                mockGroupWeeksStatement(new String[][]{{"КН-11", "10"}}),
                mockMainStatement(new String[][]{
                        {"КН-11", "Алгебра", "Шевченко", "60"},
                        {"КН-11", "Фізика", "Петренко", "40"}
                })
        );
        setupDefaultRepos();

        String result = accessService.parseAccess(dummyFile(), 1);

        assertTrue(result.contains("2"), "Result should mention 2 rows added");
        assertTrue(result.toLowerCase().contains("semester"),
                "Result should mention semester");
    }
}
