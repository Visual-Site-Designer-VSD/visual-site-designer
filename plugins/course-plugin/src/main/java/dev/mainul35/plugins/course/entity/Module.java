package dev.mainul35.plugins.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mainul35.cms.sdk.annotation.PluginEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plugin_course_modules")
@PluginEntity(pluginId = "course-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Module extends dev.mainul35.cms.sdk.entity.PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Note: Relationships to Lesson, Deck, and StudySession are managed
    // through their respective plugins (lesson-plugin, flashcard-plugin)
    // and queried via repositories rather than direct JPA relationships

    /**
     * Constructor for creating a module with plugin ID
     */
    public Module(String pluginId) {
        super(pluginId);
    }
}
