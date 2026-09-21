package com.odyssey.api.traveler;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "travelers")
public class Traveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String whatsappNumber;

    private String preferredLanguage;
    
    @Column(nullable = false)
    private boolean onboardingCompleted = false;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String auth0Subject;

    public Traveler() {
    }

    public Traveler(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    
    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public String getAuth0Subject() {
        return auth0Subject;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }

    public void setAuth0Subject(String auth0Subject) {
        this.auth0Subject = auth0Subject;
    }
}