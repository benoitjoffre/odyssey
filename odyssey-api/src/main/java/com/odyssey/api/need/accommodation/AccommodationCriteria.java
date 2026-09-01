package com.odyssey.api.need.accommodation;

import com.odyssey.api.need.Need;
import jakarta.persistence.*;

@Entity
@Table(name = "accommodation_criteria")
public class AccommodationCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "need_id", nullable = false, unique = true)
    private Need need;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private int travelers;

    @Column(nullable = false)
    private int rooms;

    public AccommodationCriteria() {
    }

    public Long getId() {
        return id;
    }

    public Need getNeed() {
        return need;
    }

    public void setNeed(Need need) {
        this.need = need;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getTravelers() {
        return travelers;
    }

    public void setTravelers(int travelers) {
        this.travelers = travelers;
    }

    public int getRooms() {
        return rooms;
    }

    public void setRooms(int rooms) {
        this.rooms = rooms;
    }
}