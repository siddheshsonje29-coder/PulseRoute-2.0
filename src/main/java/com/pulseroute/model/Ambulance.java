package com.pulseroute.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing an Ambulance under a Hospital.
 */
public class Ambulance implements Serializable {
    private static final long serialVersionUID = 1L;

    private int ambulanceId;
    private int hospitalId;
    private String vehicleNumber;
    private String driverName;
    private String driverContact;
    private String status; // AVAILABLE, ASSIGNED, ON_TRIP, OFFLINE
    private String authorizationStatus; // AUTHORIZED, PENDING, REVOKED
    private String currentLocation;
    private Timestamp createdAt;

    // Additional display field
    private String hospitalName;

    public Ambulance() {}

    public Ambulance(int ambulanceId, int hospitalId, String vehicleNumber, String driverName,
                     String driverContact, String status, String authorizationStatus, 
                     String currentLocation, Timestamp createdAt) {
        this.ambulanceId = ambulanceId;
        this.hospitalId = hospitalId;
        this.vehicleNumber = vehicleNumber;
        this.driverName = driverName;
        this.driverContact = driverContact;
        this.status = status;
        this.authorizationStatus = authorizationStatus;
        this.currentLocation = currentLocation;
        this.createdAt = createdAt;
    }

    public int getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(int ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverContact() {
        return driverContact;
    }

    public void setDriverContact(String driverContact) {
        this.driverContact = driverContact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthorizationStatus() {
        return authorizationStatus;
    }

    public void setAuthorizationStatus(String authorizationStatus) {
        this.authorizationStatus = authorizationStatus;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public boolean isDispatchable() {
        return "AUTHORIZED".equalsIgnoreCase(authorizationStatus) && "AVAILABLE".equalsIgnoreCase(status);
    }
}
