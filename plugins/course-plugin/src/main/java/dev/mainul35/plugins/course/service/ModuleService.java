package dev.mainul35.plugins.course.service;

import dev.mainul35.cms.sdk.annotation.PluginService;
import dev.mainul35.plugins.course.entity.Module;
import dev.mainul35.plugins.course.repository.CourseRepository;
import dev.mainul35.plugins.course.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@PluginService(pluginId = "course-plugin")
@RequiredArgsConstructor
public class ModuleService extends dev.mainul35.cms.sdk.service.PluginService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<Module> getAllModules() {
        logInfo("Fetching all modules");
        return moduleRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Module> getModulesByCourseId(Long courseId) {
        logInfo("Fetching modules for course: {}", courseId);
        return moduleRepository.findByCourseIdOrderByDisplayOrderAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Optional<Module> getModuleById(Long id) {
        logInfo("Fetching module with id: {}", id);
        return moduleRepository.findById(id);
    }

    @Transactional
    public Module createModule(Long courseId, Module module) {
        logInfo("Creating new module: {} in course: {}", module.getTitle(), courseId);

        if (module.getTitle() == null || module.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Module title cannot be empty");
        }

        return courseRepository.findById(courseId)
            .map(course -> {
                module.setCourse(course);

                // Set plugin ID
                if (module.getPluginId() == null) {
                    module.setPluginId("course-plugin");
                }

                if (module.getDisplayOrder() == null) {
                    module.setDisplayOrder(0);
                }

                Module savedModule = moduleRepository.save(module);
                logInfo("Module created with id: {}", savedModule.getId());
                return savedModule;
            })
            .orElseThrow(() -> {
                logError("Course not found with id: {}", courseId);
                return new RuntimeException("Course not found with id: " + courseId);
            });
    }

    @Transactional
    public Module updateModule(Long id, Module updatedModule) {
        logInfo("Updating module with id: {}", id);

        return moduleRepository.findById(id)
            .map(existingModule -> {
                if (updatedModule.getTitle() != null && !updatedModule.getTitle().trim().isEmpty()) {
                    existingModule.setTitle(updatedModule.getTitle());
                }
                if (updatedModule.getDescription() != null) {
                    existingModule.setDescription(updatedModule.getDescription());
                }
                if (updatedModule.getDisplayOrder() != null) {
                    existingModule.setDisplayOrder(updatedModule.getDisplayOrder());
                }

                Module saved = moduleRepository.save(existingModule);
                logInfo("Module updated: {}", saved.getId());
                return saved;
            })
            .orElseThrow(() -> {
                logError("Module not found with id: {}", id);
                return new RuntimeException("Module not found with id: " + id);
            });
    }

    @Transactional
    public void deleteModule(Long id) {
        logInfo("Deleting module with id: {}", id);

        if (!moduleRepository.existsById(id)) {
            throw new RuntimeException("Module not found with id: " + id);
        }

        moduleRepository.deleteById(id);
        logInfo("Module deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Module> searchModulesByTitle(String keyword) {
        logInfo("Searching modules with keyword: {}", keyword);
        return moduleRepository.findByTitleContainingIgnoreCase(keyword);
    }
}
