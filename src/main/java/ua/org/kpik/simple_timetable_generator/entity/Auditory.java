package ua.org.kpik.simple_timetable_generator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auditories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Auditory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String roomNumber;
}
