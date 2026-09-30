package com.odyssey.api.experience;
import com.odyssey.api.destination.Destination;
import jakarta.persistence.*;

@Entity
@Table(name = "experiences")
public class Experience {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "destination")
  private String legacyDestination;

  @ManyToOne
  @JoinColumn(name = "destination_id")
  private Destination destination;

  @Enumerated(EnumType.STRING)
  private ExperienceCategory category;

  private Number durationDays;
  

  public Experience() {
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

  public String getLegacyDestination() {
    return legacyDestination;
  }

  public String getDestination() {
    return legacyDestination;
  }

  public Destination getDestinationEntity() {
    return destination;
  }

  public ExperienceCategory getCategory() {
    return category;
  }

  public Number getDurationDays() {
    return durationDays;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setLegacyDestination(String legacyDestination) {
    this.legacyDestination = legacyDestination;
  }

  public void setDestination(Destination destination) {
    this.destination = destination;
  }

  public void setDestinationEntity(Destination destination) {
    this.destination = destination;
  }

  @Deprecated
  public void setDestination(String legacyDestination) {
    this.legacyDestination = legacyDestination;
  }

  public void setCategory(ExperienceCategory category) {
    this.category = category;
  }

  public void setDurationDays(Number durationDays) {
    this.durationDays = durationDays;
  }
}
