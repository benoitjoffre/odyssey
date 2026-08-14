package com.odyssey.api.experience;
import jakarta.persistence.*;

@Entity
@Table(name = "experiences")
public class Experience {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  private String description;

  private String destination;

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

  public String getDestination() {
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

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public void setCategory(ExperienceCategory category) {
    this.category = category;
  }

  public void setDurationDays(Number durationDays) {
    this.durationDays = durationDays;
  }
}
