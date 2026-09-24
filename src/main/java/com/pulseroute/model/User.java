package com.pulseroute.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing a registered PulseRoute citizen.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String fullName;
    private String email;
    private String mobile;
    private String citizenId;
    private String password;
    private String emergencyContact;
    private String address;
    private Timestamp createdAt;

    public User() {
    }

    public User(int id, String fullName, String email, String mobile, String citizenId,
                String password, String emergencyContact, String address, Timestamp createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.citizenId = citizenId;
        this.password = password;
        this.emergencyContact = emergencyContact;
        this.address = address;
        this.createdAt = createdAt;
    }

    public User(String fullName, String email, String mobile, String citizenId,
                String password, String emergencyContact, String address) {
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.citizenId = citizenId;
        this.password = password;
        this.emergencyContact = emergencyContact;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", citizenId='" + citizenId + '\'' +
                ", mobile='" + mobile + '\'' +
                '}';
    }
}
