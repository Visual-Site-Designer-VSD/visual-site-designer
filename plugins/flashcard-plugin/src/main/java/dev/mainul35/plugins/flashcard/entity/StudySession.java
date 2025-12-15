package dev.mainul35.plugins.flashcard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mainul35.cms.sdk.annotation.PluginEntity;
import dev.mainul35.plugins.course.entity.Module;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plugin_flashcard_study_sessions")
@PluginEntity(pluginId = "flashcard-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudySession extends dev.mainul35.cms.sdk.entity.PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "total_cards", nullable = false)
    private Integer totalCards = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    @JsonIgnore
    private Deck deck;

    public StudySession(String pluginId) {
        super(pluginId);
    }

    public double getPercentage() {
        if (totalCards == 0) {
            return 0.0;
        }
        return (score * 100.0) / totalCards;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}
