package com.odyssey.api.need;

import com.odyssey.api.trip.Trip;
import jakarta.persistence.*;

@Entity
@Table(name = "needs")
public class Need {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NeedType type;

    @Enumerated(EnumType.STRING)
    private NeedStatus status;

    @Column(length = 2000)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    public Need() {
    }

    public Long getId() {
        return id;
    }

    public NeedType getType() {
        return type;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setType(NeedType type) {
        this.type = type;
    }

    public void setStatus(NeedStatus status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}