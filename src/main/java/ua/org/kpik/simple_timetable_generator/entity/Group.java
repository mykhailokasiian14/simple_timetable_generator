package ua.org.kpik.simple_timetable_generator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="groups")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String groupName;
}
