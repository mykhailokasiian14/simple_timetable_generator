package ua.org.kpik.simple_timetable_generator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teaching_loads")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeachingLoad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private  Integer hoursPerWeek;
}
