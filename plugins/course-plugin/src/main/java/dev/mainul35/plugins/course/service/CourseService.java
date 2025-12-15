package dev.mainul35.plugins.course.service;

import dev.mainul35.cms.sdk.annotation.PluginService;
import dev.mainul35.plugins.course.entity.Course;
import dev.mainul35.plugins.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@PluginService(pluginId = "course-plugin")
@RequiredArgsConstructor
public class CourseService extends dev.mainul35.cms.sdk.service.PluginService {

    private final CourseRepository courseRepository;

    /**
     * Get all courses ordered by display order
     */
    public List<Course> getAllCourses() {
        logDebug("Fetching all courses");
        return courseRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Get course by ID
     */
    public Optional<Course> getCourseById(Long id) {
        logDebug("Fetching course with id: {}", id);
        return courseRepository.findById(id);
    }

    /**
     * Get course by ID with modules eagerly loaded
     */
    public Optional<Course> getCourseWithModules(Long id) {
        logDebug("Fetching course with modules, id: {}", id);
        return courseRepository.findByIdWithModules(id);
    }

    /**
     * Create a new course
     */
    @Transactional
    public Course createCourse(Course course) {
        logDebug("Creating new course: {}", course.getTitle());

        // Set plugin ID
        if (course.getPluginId() == null) {
            course.setPluginId("course-plugin");
        }

        if (course.getDisplayOrder() == null) {
            course.setDisplayOrder(0);
        }

        Course savedCourse = courseRepository.save(course);
        logInfo("Course created with id: {}", savedCourse.getId());

        return savedCourse;
    }

    /**
     * Update an existing course
     */
    @Transactional
    public Course updateCourse(Long id, Course courseDetails) {
        logDebug("Updating course with id: {}", id);

        return courseRepository.findById(id)
                .map(existingCourse -> {
                    existingCourse.setTitle(courseDetails.getTitle());
                    existingCourse.setDescription(courseDetails.getDescription());

                    if (courseDetails.getDisplayOrder() != null) {
                        existingCourse.setDisplayOrder(courseDetails.getDisplayOrder());
                    }

                    Course updated = courseRepository.save(existingCourse);
                    logInfo("Course updated: {}", updated.getId());
                    return updated;
                })
                .orElseThrow(() -> {
                    logError("Course not found with id: {}", id);
                    return new RuntimeException("Course not found with id: " + id);
                });
    }

    /**
     * Delete a course by ID
     */
    @Transactional
    public void deleteCourse(Long id) {
        logDebug("Deleting course with id: {}", id);

        if (!courseRepository.existsById(id)) {
            logError("Course not found with id: {}", id);
            throw new RuntimeException("Course not found with id: " + id);
        }

        courseRepository.deleteById(id);
        logInfo("Course deleted: {}", id);
    }

    /**
     * Search courses by title
     */
    public List<Course> searchCoursesByTitle(String keyword) {
        logDebug("Searching courses by title: {}", keyword);
        return courseRepository.findByTitleContainingIgnoreCase(keyword);
    }
}
