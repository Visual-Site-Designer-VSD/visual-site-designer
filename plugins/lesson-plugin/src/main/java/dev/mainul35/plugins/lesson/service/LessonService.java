package dev.mainul35.plugins.lesson.service;

import dev.mainul35.cms.sdk.annotation.PluginService;
import dev.mainul35.plugins.lesson.entity.Lesson;
import dev.mainul35.plugins.lesson.repository.LessonRepository;
import dev.mainul35.plugins.course.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@PluginService(pluginId = "lesson-plugin")
@RequiredArgsConstructor
public class LessonService extends dev.mainul35.cms.sdk.service.PluginService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;

    @Transactional(readOnly = true)
    public List<Lesson> getLessonsByModuleId(Long moduleId) {
        logInfo("Fetching lessons for module: {}", moduleId);
        return lessonRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId);
    }

    @Transactional(readOnly = true)
    public Optional<Lesson> getLessonById(Long id) {
        logInfo("Fetching lesson with id: {}", id);
        return lessonRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Lesson> getLessonWithMedia(Long id) {
        logInfo("Fetching lesson with media, id: {}", id);
        return lessonRepository.findByIdWithMedia(id);
    }

    @Transactional
    public Lesson createLesson(Long moduleId, Lesson lesson) {
        logInfo("Creating new lesson in module: {}", moduleId);

        if (lesson.getTitle() == null || lesson.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Lesson title cannot be empty");
        }

        return moduleRepository.findById(moduleId)
            .map(module -> {
                lesson.setModule(module);

                if (lesson.getPluginId() == null) {
                    lesson.setPluginId("lesson-plugin");
                }
                if (lesson.getDisplayOrder() == null) {
                    lesson.setDisplayOrder(0);
                }
                if (lesson.getContentFormat() == null) {
                    lesson.setContentFormat("html");
                }

                Lesson savedLesson = lessonRepository.save(lesson);
                logInfo("Lesson created with id: {}", savedLesson.getId());
                return savedLesson;
            })
            .orElseThrow(() -> {
                logError("Module not found with id: {}", moduleId);
                return new RuntimeException("Module not found with id: " + moduleId);
            });
    }

    @Transactional
    public Lesson updateLesson(Long id, Lesson updatedLesson) {
        logInfo("Updating lesson with id: {}", id);

        return lessonRepository.findById(id)
            .map(existingLesson -> {
                if (updatedLesson.getTitle() != null && !updatedLesson.getTitle().trim().isEmpty()) {
                    existingLesson.setTitle(updatedLesson.getTitle());
                }
                if (updatedLesson.getDescription() != null) {
                    existingLesson.setDescription(updatedLesson.getDescription());
                }
                if (updatedLesson.getContent() != null) {
                    existingLesson.setContent(updatedLesson.getContent());
                }
                if (updatedLesson.getDisplayOrder() != null) {
                    existingLesson.setDisplayOrder(updatedLesson.getDisplayOrder());
                }

                Lesson saved = lessonRepository.save(existingLesson);
                logInfo("Lesson updated: {}", saved.getId());
                return saved;
            })
            .orElseThrow(() -> {
                logError("Lesson not found with id: {}", id);
                return new RuntimeException("Lesson not found with id: " + id);
            });
    }

    @Transactional
    public void deleteLesson(Long id) {
        logInfo("Deleting lesson with id: {}", id);

        if (!lessonRepository.existsById(id)) {
            throw new RuntimeException("Lesson not found with id: " + id);
        }

        lessonRepository.deleteById(id);
        logInfo("Lesson deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Lesson> searchLessonsByTitle(String keyword) {
        logInfo("Searching lessons with keyword: {}", keyword);
        return lessonRepository.findByTitleContainingIgnoreCase(keyword);
    }
}
