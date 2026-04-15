package ua.org.kpik.simple_timetable_generator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.service.ExcelService;

@RestController
@RequestMapping("api/excel")
@RequiredArgsConstructor
public class ExcelController {
    private final ExcelService excelService;

    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }
        return excelService.parseAndSaveExcel(file);
    }

    @GetMapping("/download-split-timetable")
    public ResponseEntity<byte[]> downloadTimetable() {
        byte[] fileContent = excelService.exportTimetableToExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=groups_timetable.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileContent);
    }

    @GetMapping("/download-total-timetable")
    public ResponseEntity<byte[]> downloadTotalTimetable() {
        byte[] fileContent  = excelService.exportTotalTimetableToExcel();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=total_timetable.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileContent);
    }


}
