package com.odyssey.api.booking;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentNotification;
import com.odyssey.api.need.Need;
import com.odyssey.api.quote.Quote;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_requests")
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BookingRequestStatus status;

    @Column(length = 2000)
    private String notes;

    @OneToOne
    @JoinColumn(name = "need_id", nullable = false, unique = true)
    private Need need;

    @ManyToOne
    @JoinColumn(name = "assigned_agent_id")
    private Agent assignedAgent;

    @OneToMany(mappedBy = "bookingRequest", cascade = CascadeType.REMOVE)
    private List<Quote> quotes = new ArrayList<>();

    @OneToMany(mappedBy = "bookingRequest", cascade = CascadeType.REMOVE)
    private List<AgentNotification> notifications = new ArrayList<>();

    public BookingRequest() {
    }

    public Long getId() {
        return id;
    }

    public BookingRequestStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Need getNeed() {
        return need;
    }

    public void setStatus(BookingRequestStatus status) {
        this.status = status;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setNeed(Need need) {
        this.need = need;
    }

    public Agent getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(Agent assignedAgent) {
        this.assignedAgent = assignedAgent;
    }
}