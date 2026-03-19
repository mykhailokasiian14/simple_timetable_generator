package ua.org.kpik.simple_timetable_generator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lessonId;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "auditory_id")
    private Auditory auditory;

    private Integer dayOfWeek;
    private Integer lessonNumber;
}
