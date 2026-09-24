package com.pulseroute.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing a registered Hospital.
 */
public class Hospital implements Serializable {
    private static final long serialVersionUID = 1L;

    private int hospitalId;
    private String hospitalCode;
    private String hospitalName;
    private String email;
    private String passwordHash;
    private String location;
    private String contact;
    private String status;
    private Timestamp createdAt;

    public Hospital() {}

    public Hospital(int hospitalId, String hospitalCode, String hospitalName, String email, 
                    String passwordHash, String location, String contact, String status, Timestamp createdAt) {
        this.hospitalId = hospitalId;
        this.hospitalCode = hospitalCode;
        this.hospitalName = hospitalName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.location = location;
        this.contact = contact;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
