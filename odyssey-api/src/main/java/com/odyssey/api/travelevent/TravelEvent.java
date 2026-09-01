package com.odyssey.api.travelevent;

import com.odyssey.api.experience.Experience;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "travel_events")
public class TravelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "experience_id", nullable = false)
    private Experience experience;

    public TravelEvent() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public Experience getExperience() {
        return experience;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExperience(Experience experience) {
        this.experience = experience;
    }
}