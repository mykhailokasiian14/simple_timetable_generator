package ua.org.kpik.simple_timetable_generator.controller.web_controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ua.org.kpik.simple_timetable_generator.service.AccessService;
import ua.org.kpik.simple_timetable_generator.service.ExcelService;
import ua.org.kpik.simple_timetable_generator.service.TimetableService;

@Controller
@RequestMapping("/")
@AllArgsConstructor
public class PageControllerWeb {
    private final ExcelService excelService;
    private final AccessService accessService;
    private final TimetableService timetableService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/tutorial")
    public String tutorialPage() {
        return "tutorial";
    }

    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }


    @GetMapping("upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "semester", required = false, defaultValue = "1") int semester) {

        if ("excel".equals(fileType)) {
            excelService.parseAndSaveExcel(file);
        } else if ("access".equals(fileType)) {
            accessService.parseAccess(file, semester);
        }

        return "redirect:/generate";
    }

    @GetMapping("/generate")
    public String generatePage() {
        return "generate";
    }

    @PostMapping("/run-generation")
    public String runGeneration() {
        timetableService.solve();

        return "redirect:/result";
    }

    @GetMapping("/result")
    public String resultPage() {
        return "result";
    }
}
