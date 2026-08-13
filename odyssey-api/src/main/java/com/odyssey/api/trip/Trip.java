package com.odyssey.api.trip;

import com.odyssey.api.traveler.Traveler;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    @ManyToOne
    @JoinColumn(name = "traveler_id", nullable = false)
    private Traveler traveler;

    public Trip() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Traveler getTraveler() {
        return traveler;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public void setTraveler(Traveler traveler) {
        this.traveler = traveler;
    }
}