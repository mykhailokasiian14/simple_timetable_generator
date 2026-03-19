package ua.org.kpik.simple_timetable_generator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.org.kpik.simple_timetable_generator.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}
