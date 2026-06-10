package ua.org.kpik.simple_timetable_generator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ua.org.kpik.simple_timetable_generator.entity.*;
import ua.org.kpik.simple_timetable_generator.repository.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final GroupRepository groupRepository;
    private final AuditoryRepository auditoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (groupRepository.count() > 0) {
            System.out.println("Дані вже є в базі, DataLoader пропускаємо...");
            return;
        }

        System.out.println("Завантажую ТЕСТОВІ ДАНІ для хардкорного розкладу...");

        Auditory a1 = new Auditory(); a1.setRoomNumber("1");
        Auditory a2 = new Auditory(); a2.setRoomNumber("2");
        Auditory a3 = new Auditory(); a3.setRoomNumber("3");
        Auditory a4 = new Auditory(); a4.setRoomNumber("4");
        Auditory a5 = new Auditory(); a5.setRoomNumber("5");
        Auditory a6 = new Auditory(); a6.setRoomNumber("6");
        Auditory a7 = new Auditory(); a7.setRoomNumber("7");
        Auditory a8 = new Auditory(); a8.setRoomNumber("8");
        Auditory a9 = new Auditory(); a9.setRoomNumber("9");
        Auditory a10 = new Auditory(); a10.setRoomNumber("10");
        Auditory a11 = new Auditory(); a11.setRoomNumber("11");
        Auditory a12 = new Auditory(); a12.setRoomNumber("12");
        Auditory a13 = new Auditory(); a13.setRoomNumber("13");
        Auditory a14 = new Auditory(); a14.setRoomNumber("14");
        Auditory a15 = new Auditory(); a15.setRoomNumber("15");
        Auditory a16 = new Auditory(); a16.setRoomNumber("16");
        Auditory a17 = new Auditory(); a17.setRoomNumber("17");
        Auditory a18 = new Auditory(); a18.setRoomNumber("18");
        Auditory a19 = new Auditory(); a19.setRoomNumber("19");
        Auditory a20 = new Auditory(); a20.setRoomNumber("20");
        Auditory a21 = new Auditory(); a21.setRoomNumber("21");
        Auditory a22 = new Auditory(); a22.setRoomNumber("22");
        Auditory a23 = new Auditory(); a23.setRoomNumber("23");
        Auditory a24 = new Auditory(); a24.setRoomNumber("24");
        Auditory a25 = new Auditory(); a25.setRoomNumber("25");
        Auditory a26 = new Auditory(); a26.setRoomNumber("26");
        Auditory a27 = new Auditory(); a27.setRoomNumber("27");
        Auditory a28 = new Auditory(); a28.setRoomNumber("28");
        Auditory a29 = new Auditory(); a29.setRoomNumber("29");
        Auditory a30 = new Auditory(); a30.setRoomNumber("30");
        Auditory a31 = new Auditory(); a31.setRoomNumber("31");
        Auditory a32 = new Auditory(); a32.setRoomNumber("32");
        Auditory a33 = new Auditory(); a33.setRoomNumber("33");
        Auditory a34 = new Auditory(); a34.setRoomNumber("34");
        Auditory a35 = new Auditory(); a35.setRoomNumber("35");
        Auditory a36 = new Auditory(); a36.setRoomNumber("36");
        Auditory a37 = new Auditory(); a37.setRoomNumber("37");
        Auditory a38 = new Auditory(); a38.setRoomNumber("38");
        Auditory a39 = new Auditory(); a39.setRoomNumber("39");
        Auditory a40 = new Auditory(); a40.setRoomNumber("40");
        Auditory a41 = new Auditory(); a41.setRoomNumber("41");
        Auditory a42 = new Auditory(); a42.setRoomNumber("42");
        Auditory a43 = new Auditory(); a43.setRoomNumber("43");
        Auditory a44 = new Auditory(); a44.setRoomNumber("44");
        Auditory a45 = new Auditory(); a45.setRoomNumber("45");
        Auditory a46 = new Auditory(); a46.setRoomNumber("46");
        Auditory a47 = new Auditory(); a47.setRoomNumber("47");
        Auditory a48 = new Auditory(); a48.setRoomNumber("48");
        Auditory a49 = new Auditory(); a49.setRoomNumber("49");
        Auditory a50 = new Auditory(); a50.setRoomNumber("50");
        Auditory a51 = new Auditory(); a51.setRoomNumber("51");
        Auditory a52 = new Auditory(); a52.setRoomNumber("52");
        Auditory a53 = new Auditory(); a53.setRoomNumber("53");
        Auditory a54 = new Auditory(); a54.setRoomNumber("54");
        Auditory a55 = new Auditory(); a55.setRoomNumber("55");
        Auditory a56 = new Auditory(); a56.setRoomNumber("56");

        auditoryRepository.saveAll(List.of(
                a1, a2, a3, a4, a5, a6,
                a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20,
                a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34,
                a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48,
                a49, a50, a51, a52, a53, a54, a55, a56
        ));

        System.out.println("ТЕСТОВІ ДАНІ ПРО АУДИТОРІЇ УСПІШНО ЗАВАНТАЖЕНО!");
        System.out.print("Переходіть за цим посиланням для початку роботи -> ");
        System.out.println("http://localhost:8080/");
    }
}
