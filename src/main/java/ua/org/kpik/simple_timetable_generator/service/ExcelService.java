package ua.org.kpik.simple_timetable_generator.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.enaum.LessonWeekType;
import ua.org.kpik.simple_timetable_generator.entity.*;
import ua.org.kpik.simple_timetable_generator.repository.GroupRepository;
import ua.org.kpik.simple_timetable_generator.repository.SubjectRepository;
import ua.org.kpik.simple_timetable_generator.repository.TeacherRepository;
import ua.org.kpik.simple_timetable_generator.repository.TeachingLoadRepository;
import ua.org.kpik.simple_timetable_generator.repository.LessonRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelService {
    private final GroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingLoadRepository teachingLoadRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public String parseAndSaveExcel(MultipartFile file) { // todo semester 1 2 input like in AccessService
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowsAdded = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                if (row.getCell(0) == null || row.getCell(1) == null) break;

                String groupName = row.getCell(0).getStringCellValue().trim();
                String subjectName = row.getCell(1).getStringCellValue().trim();
                String teacherName = row.getCell(2).getStringCellValue().trim();

                if (teacherName == null || teacherName.trim().isEmpty() || teacherName.contains("Всього годин")) {
                    continue;
                }
                double hours =  row.getCell(3).getNumericCellValue();

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

                teacher.getSubjects().add(subject);
                teacherRepository.save(teacher);

                TeachingLoad load = new TeachingLoad();
                load.setGroup(group);
                load.setSubject(subject);
                load.setTeacher(teacher);
                load.setLessonsPerWeek(hours);
                teachingLoadRepository.save(load);

                rowsAdded++;
            }
            return "Successful " + rowsAdded + " rows added";

        } catch (Exception e) {
            throw new RuntimeException("Exception while reading .xlsx file: " + e.getMessage());
        }
    }

    public byte[] exportSplitTimetableToExcel() {
        List<Lesson> lessons = lessonRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // style
            Map<String, CellStyle> styles = createExcelStyles(workbook);

            // data grouping
            Map<String, Map<Integer, Map<Integer, Lesson>>> groupedData = lessons.stream()
                    .collect(Collectors.groupingBy(
                            l -> l.getGroup().getGroupName(),
                            Collectors.groupingBy(
                                    Lesson::getDayOfWeek,
                                    Collectors.toMap(Lesson::getLessonNumber, l -> l,
                                            (existing, replacement) -> existing
                                    )
                            )
                    ));

            String[] days = {"Пара", "Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця"};
            int maxLessons = 4;

            // different sheets for groups
            for (Map.Entry<String, Map<Integer, Map<Integer, Lesson>>> groupEntry : groupedData.entrySet()) {
                String groupName = groupEntry.getKey();
                Sheet sheet = workbook.createSheet("Група " + groupName);

                // header
                Row headerRow = sheet.createRow(0);
                headerRow.setHeightInPoints(25);
                for (int i = 0; i < days.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(days[i]);
                    cell.setCellStyle(styles.get("greyHeader"));
                    sheet.setColumnWidth(i, i == 0 ? 2000 : 7000);
                }

                Map<Integer, Map<Integer, Lesson>> daysData = groupEntry.getValue();

                // rows
                for (int lessonNum = 1; lessonNum <= maxLessons; lessonNum++) {
                    Row row = sheet.createRow(lessonNum);
                    row.setHeightInPoints(45);

                    // num of lesson
                    Cell numCell = row.createCell(0);
                    numCell.setCellValue(lessonNum);
                    numCell.setCellStyle(styles.get("greyHeader"));

                    // days
                    for (int dayNum = 1; dayNum <= 5; dayNum++) {
                        Cell cell = row.createCell(dayNum);

                        Lesson lesson = daysData.getOrDefault(dayNum, Collections.emptyMap()).get(lessonNum);

                        if (lesson != null) {
                            String room = lesson.getAuditory() != null ? lesson.getAuditory().getRoomNumber() : "?";
                            String text = String.format("%s\n%s\nауд. %s",
                                    lesson.getSubject().getSubjectName(),
                                    lesson.getTeacher().getLastName(),
                                    room);

                            cell.setCellValue(text);
                            cell.setCellStyle(styles.get("lesson"));
                        } else {
                            cell.setCellValue("--");
                            cell.setCellStyle(styles.get("empty"));
                        }
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Exception while creating .xlsx file: " + e.getMessage());
        }
    }

    public byte[] exportTotalTimetableToExcel() {
        List<Lesson> lessons = this.lessonRepository.findAll();
        List<Group> groups = this.groupRepository.findAll();
        groups.sort(Comparator.comparing(Group::getGroupName));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Розклад");

            // style

            Map<String, CellStyle> styles = createExcelStyles(workbook);
            // day | № | Group 1 | Group 2 ...
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("День");
            headerRow.createCell(1).setCellValue("Пара");
            headerRow.getCell(0).setCellStyle(styles.get("blueHeader"));
            headerRow.getCell(1).setCellStyle(styles.get("blueHeader"));

            for (int i = 0; i < groups.size(); i++) {
                Cell cell = headerRow.createCell(i + 2);
                cell.setCellValue(groups.get(i).getGroupName());
                cell.setCellStyle(styles.get("blueHeader"));
                sheet.setColumnWidth(i + 2, 5000);
            }

            // filling
            String[] dayNames = {"", "Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця"};
            int currentRowIdx = 1;

            for (int day = 1; day <= 5; day++) {
                int dayStartRow = currentRowIdx;

                for (int num = 1; num <= 4; num++) {
                    int lessonStartRow = currentRowIdx;

                    Row rowTop = sheet.createRow(currentRowIdx++);
                    Row rowBottom = sheet.createRow(currentRowIdx++);

                    rowTop.setHeightInPoints(25);
                    rowBottom.setHeightInPoints(25);

                    Cell dayCellTop = rowTop.createCell(0);
                    if (num == 1) dayCellTop.setCellValue(dayNames[day]);
                    dayCellTop.setCellStyle(styles.get("lesson"));
                    rowBottom.createCell(0).setCellStyle(styles.get("lesson"));

                    Cell numCellTop = rowTop.createCell(1);
                    numCellTop.setCellValue(num);
                    numCellTop.setCellStyle(styles.get("lesson"));
                    rowBottom.createCell(1).setCellStyle(styles.get("lesson"));

                    sheet.addMergedRegion(new CellRangeAddress(lessonStartRow, lessonStartRow + 1, 1, 1));

                    for (int gIdx = 0; gIdx < groups.size(); gIdx++) {
                        Group group = groups.get(gIdx);
                        int colIdx = gIdx + 2;

                        Cell cellTop = rowTop.createCell(colIdx);
                        Cell cellBottom = rowBottom.createCell(colIdx);

                        cellTop.setCellStyle(styles.get("lesson"));
                        cellBottom.setCellStyle(styles.get("lesson"));

                        int d = day;
                        int n = num;

                        List<Lesson> currentLessons = lessons.stream()
                                .filter(l -> l.getGroup().getId().equals(group.getId())
                                        && l.getDayOfWeek() == d
                                        && l.getLessonNumber() == n)
                                .toList();

                        Lesson everyWeek = currentLessons.stream().filter(l -> l.getLessonWeekType() == LessonWeekType.EVERY_WEEK).findFirst().orElse(null);
                        Lesson numerator = currentLessons.stream().filter(l -> l.getLessonWeekType() == LessonWeekType.NUMERATOR).findFirst().orElse(null);
                        Lesson denominator = currentLessons.stream().filter(l -> l.getLessonWeekType() == LessonWeekType.DENOMINATOR).findFirst().orElse(null);

                        if (everyWeek != null) {
                            String text = formatLessonText(everyWeek);
                            cellTop.setCellValue(text);
                            sheet.addMergedRegion(new CellRangeAddress(lessonStartRow, lessonStartRow + 1, colIdx, colIdx));
                        } else {
                            if (numerator != null) {
                                cellTop.setCellValue(formatLessonText(numerator));
                            }
                            if (denominator != null) {
                                cellBottom.setCellValue(formatLessonText(denominator));
                            }
                        }
                    }
                }
                sheet.addMergedRegion(new CellRangeAddress(dayStartRow, currentRowIdx - 1, 0, 0));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Exception while creating .xlsx file: " + e.getMessage());
        }
    }

    private Map<String, CellStyle> createExcelStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // 1st header style
        CellStyle greyHeader = workbook.createCellStyle();
        greyHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        greyHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greyHeader.setAlignment(HorizontalAlignment.CENTER);
        greyHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(greyHeader);
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        greyHeader.setFont(boldFont);
        styles.put("greyHeader", greyHeader);

        // 2nd header style
        CellStyle blueHeader = workbook.createCellStyle();
        blueHeader.cloneStyleFrom(greyHeader);
        blueHeader.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        styles.put("blueHeader", blueHeader);

        // lesson style
        CellStyle lesson = workbook.createCellStyle();
        lesson.setAlignment(HorizontalAlignment.CENTER);
        lesson.setVerticalAlignment(VerticalAlignment.CENTER);
        lesson.setWrapText(true);
        setBorder(lesson);
        styles.put("lesson", lesson);

        // window style
        CellStyle empty = workbook.createCellStyle();
        empty.cloneStyleFrom(lesson);
        Font italicFont = workbook.createFont();
        italicFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        italicFont.setItalic(true);
        empty.setFont(italicFont);
        styles.put("empty", empty);

        return styles;
    }

    private void setBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private String formatLessonText(Lesson l) {
        return String.format("%s\n%s\nауд. %s",
                l.getSubject().getSubjectName(),
                l.getTeacher().getLastName(),
                l.getAuditory() != null ? l.getAuditory().getRoomNumber() : "?");
    }
}