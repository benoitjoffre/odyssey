package com.odyssey.api.intent;

import com.odyssey.api.traveler.Traveler;
import jakarta.persistence.*;
import com.odyssey.api.experience.ExperienceCategory;

@Entity
@Table(name = "intents")
public class Intent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private IntentStatus status;

    @Enumerated(EnumType.STRING)
    private ExperienceCategory category;

    @ManyToOne
    @JoinColumn(name = "traveler_id", nullable = false)
    private Traveler traveler;

    public Intent() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IntentStatus getStatus() {
        return status;
    }

    public Traveler getTraveler() {
        return traveler;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(IntentStatus status) {
        this.status = status;
    }

    public ExperienceCategory getCategory() {
        return category;
    }

    public void setCategory(ExperienceCategory category) {
        this.category = category;
    }

    public void setTraveler(Traveler traveler) {
        this.traveler = traveler;
    }
}