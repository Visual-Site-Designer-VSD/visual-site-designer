package dev.mainul35.plugins.flashcard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mainul35.cms.sdk.annotation.PluginEntity;
import dev.mainul35.plugins.lesson.entity.Lesson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plugin_flashcard_cards")
@PluginEntity(pluginId = "flashcard-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FlashCard extends dev.mainul35.cms.sdk.entity.PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "front_content", nullable = false, columnDefinition = "TEXT")
    private String frontContent;

    @Column(name = "back_content", nullable = false, columnDefinition = "TEXT")
    private String backContent;

    @Column(name = "content_format", length = 20)
    private String contentFormat = "html";

    @Column(name = "question_type", length = 20)
    private String questionType = "multiple_choice";

    @Column(name = "answer_options", columnDefinition = "TEXT")
    private String answerOptions;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "correct_answer_explanation", columnDefinition = "TEXT")
    private String correctAnswerExplanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @JsonIgnore
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    @JsonIgnore
    private Deck deck;

    public FlashCard(String pluginId) {
        super(pluginId);
    }

    public FlashCard(String frontContent, String backContent) {
        this.frontContent = frontContent;
        this.backContent = backContent;
    }
}
