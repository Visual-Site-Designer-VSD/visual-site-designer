package dev.mainul35.plugins.lesson.controller;

import dev.mainul35.cms.sdk.annotation.PluginController;
import dev.mainul35.plugins.lesson.entity.Lesson;
import dev.mainul35.plugins.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PluginController(pluginId = "lesson-plugin", basePath = "/api/modules")
@RequestMapping("/api/modules/{moduleId}/lessons")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class LessonController extends dev.mainul35.cms.sdk.controller.PluginController {

    private final LessonService lessonService;

    @GetMapping
    public ResponseEntity<List<Lesson>> getLessonsByModuleId(@PathVariable Long moduleId) {
        try {
            List<Lesson> lessons = lessonService.getLessonsByModuleId(moduleId);
            return ResponseEntity.ok(lessons);
        } catch (Exception e) {
            logError("Error fetching lessons for module: {}", moduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<Lesson> getLessonById(@PathVariable Long moduleId, @PathVariable Long lessonId) {
        try {
            return lessonService.getLessonById(lessonId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logError("Error fetching lesson with id: {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Lesson> createLesson(@PathVariable Long moduleId, @RequestBody Lesson lesson) {
        try {
            Lesson createdLesson = lessonService.createLesson(moduleId, lesson);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdLesson);
        } catch (IllegalArgumentException e) {
            logError("Validation error creating lesson", e);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Module not found")) {
                return ResponseEntity.notFound().build();
            }
            logError("Error creating lesson in module: {}", moduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(@PathVariable Long moduleId, @PathVariable Long lessonId, @RequestBody Lesson lesson) {
        try {
            Lesson updatedLesson = lessonService.updateLesson(lessonId, lesson);
            return ResponseEntity.ok(updatedLesson);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            logError("Error updating lesson with id: {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long moduleId, @PathVariable Long lessonId) {
        try {
            lessonService.deleteLesson(lessonId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            logError("Error deleting lesson with id: {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
