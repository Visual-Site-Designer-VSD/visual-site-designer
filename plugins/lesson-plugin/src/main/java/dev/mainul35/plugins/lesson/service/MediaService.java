package dev.mainul35.plugins.lesson.service;

import dev.mainul35.cms.sdk.annotation.PluginService;
import dev.mainul35.plugins.lesson.entity.Media;
import dev.mainul35.plugins.lesson.repository.MediaRepository;
import dev.mainul35.plugins.lesson.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@PluginService(pluginId = "lesson-plugin")
@RequiredArgsConstructor
public class MediaService extends dev.mainul35.cms.sdk.service.PluginService {

    private final MediaRepository mediaRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public Optional<Media> getMediaByLessonId(Long lessonId) {
        logInfo("Fetching media for lesson: {}", lessonId);
        return mediaRepository.findByLessonId(lessonId);
    }

    @Transactional(readOnly = true)
    public Optional<Media> getMediaById(Long id) {
        logInfo("Fetching media with id: {}", id);
        return mediaRepository.findById(id);
    }

    @Transactional
    public Media createMedia(Long lessonId, Media media) {
        logInfo("Creating media for lesson: {}", lessonId);

        return lessonRepository.findById(lessonId)
            .map(lesson -> {
                media.setLesson(lesson);

                if (media.getPluginId() == null) {
                    media.setPluginId("lesson-plugin");
                }

                Media savedMedia = mediaRepository.save(media);
                logInfo("Media created with id: {}", savedMedia.getId());
                return savedMedia;
            })
            .orElseThrow(() -> {
                logError("Lesson not found with id: {}", lessonId);
                return new RuntimeException("Lesson not found with id: " + lessonId);
            });
    }

    @Transactional
    public void deleteMedia(Long id) {
        logInfo("Deleting media with id: {}", id);

        if (!mediaRepository.existsById(id)) {
            throw new RuntimeException("Media not found with id: " + id);
        }

        mediaRepository.deleteById(id);
        logInfo("Media deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Media> getMediaByType(String mediaType) {
        logInfo("Fetching media of type: {}", mediaType);
        return mediaRepository.findByMediaType(mediaType);
    }
}
