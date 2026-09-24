package com.pulseroute.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model representing an SOS / Emergency Request.
 */
public class EmergencyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int requestId;
    private Integer userId;
    private String emergencyType; // Ambulance, Cardiac, Fire Rescue, Police Support, etc.
    private String userLocation;
    private Timestamp createdAt;
    private String status; // PENDING, RECEIVED, UNDER_REVIEW, APPROVED, AMBULANCE_ASSIGNED, DISPATCHED, ARRIVING, ARRIVED, COMPLETED, CANCELLED, REJECTED
    private Integer hospitalId;
    private Integer ambulanceId;
    private Timestamp assignedAt;
    private Timestamp completedAt;
    private String rejectionReason;
    private String notes;

    // Associated details for rich display
    private String userName;
    private String userMobile;
    private String userEmail;
    private String userEmergencyContact;
    private String hospitalName;
    private String vehicleNumber;
    private String driverName;
    private String driverContact;

    public EmergencyRequest() {}

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(String userLocation) {
        this.userLocation = userLocation;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Integer hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Integer getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Integer ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobile() {
        return userMobile;
    }

    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserEmergencyContact() {
        return userEmergencyContact;
    }

    public void setUserEmergencyContact(String userEmergencyContact) {
        this.userEmergencyContact = userEmergencyContact;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
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
}
