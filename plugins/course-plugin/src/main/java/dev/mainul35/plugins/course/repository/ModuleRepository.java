package dev.mainul35.plugins.course.repository;

import dev.mainul35.plugins.course.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    /**
     * Find all modules ordered by display order ascending
     */
    List<Module> findAllByOrderByDisplayOrderAsc();

    /**
     * Find modules by course ID ordered by display order ascending
     */
    List<Module> findByCourseIdOrderByDisplayOrderAsc(Long courseId);

    /**
     * Search modules by title (case-insensitive)
     */
    List<Module> findByTitleContainingIgnoreCase(String keyword);

    /**
     * Count modules in a course
     */
    Long countByCourseId(Long courseId);

    // Note: Cross-plugin queries (with lessons, flashcards) removed
    // These will be handled through service layer using plugin dependencies
}
