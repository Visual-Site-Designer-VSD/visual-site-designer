package dev.mainul35.plugins.flashcard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.mainul35.cms.sdk.annotation.PluginEntity;
import dev.mainul35.plugins.course.entity.Module;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plugin_flashcard_decks")
@PluginEntity(pluginId = "flashcard-plugin")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Deck extends dev.mainul35.cms.sdk.entity.PluginEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonIgnore
    private Module module;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlashCard> flashcards = new ArrayList<>();

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL)
    private List<StudySession> studySessions = new ArrayList<>();

    public Deck(String pluginId) {
        super(pluginId);
    }

    public void addFlashcard(FlashCard flashcard) {
        flashcards.add(flashcard);
        flashcard.setDeck(this);
    }

    public void removeFlashcard(FlashCard flashcard) {
        flashcards.remove(flashcard);
        flashcard.setDeck(null);
    }
}
