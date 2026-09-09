package com.odyssey.api.need.transfer;

import com.odyssey.api.need.Need;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "transfer_criteria")
public class TransferCriteria {

  // Constructor
  public TransferCriteria() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  public Long getId() {
      return id;
  }

  @OneToOne
  @JoinColumn(name = "need_id", nullable = false, unique = true)
  private Need need;

  public Need getNeed() {
      return need;
  }

  public void setNeed(Need need) {
      this.need = need;
  }

  @Column(nullable = false)
  private String pickupLocation;

  public String getPickupLocation() {
      return pickupLocation;
  }

  public void setPickupLocation(String pickupLocation) {
      this.pickupLocation = pickupLocation;
  }

  @Column(nullable = false)
  private String dropoffLocation;

  public String getDropoffLocation() {
      return dropoffLocation;
  }

  public void setDropoffLocation(String dropoffLocation) {
      this.dropoffLocation = dropoffLocation;
  }

  
  private int travelers;

  public int getTravelers() {
      return travelers;
  }

  public void setTravelers(int travelers) {
      this.travelers = travelers;
  }
}
