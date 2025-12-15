package dev.mainul35.plugins.lesson.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mainul35.cms.sdk.annotation.PluginEntity;
import dev.mainul35.plugins.course.entity.Module;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plugin_lesson_lessons")
@PluginEntity(pluginId = "lesson-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Lesson extends dev.mainul35.cms.sdk.entity.PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore
    private Module module;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_format", length = 20)
    private String contentFormat = "html";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private Media media;

    // FlashCard relationship removed - managed by flashcard-plugin

    /**
     * Constructor for creating a lesson with plugin ID
     */
    public Lesson(String pluginId) {
        super(pluginId);
    }
}
