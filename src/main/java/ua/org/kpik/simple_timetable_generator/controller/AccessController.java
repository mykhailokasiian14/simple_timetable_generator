package ua.org.kpik.simple_timetable_generator.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.service.AccessService;


@RestController("/api/access")
@RequiredArgsConstructor
public class AccessController {
    private final AccessService accessService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }
        return accessService.parseAndSaveAccess(file);
    }
}
