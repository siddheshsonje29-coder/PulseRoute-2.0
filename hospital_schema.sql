-- PulseRoute Database Update for Hospital Admin, Ambulances & Emergency Requests
USE pulseroute;

-- 1. Hospitals Table
CREATE TABLE IF NOT EXISTS hospitals (
    hospital_id INT AUTO_INCREMENT PRIMARY KEY,
    hospital_code VARCHAR(50) NOT NULL UNIQUE,
    hospital_name VARCHAR(150) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    contact VARCHAR(25) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hosp_email (email),
    INDEX idx_hosp_code (hospital_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Ambulances Table
CREATE TABLE IF NOT EXISTS ambulances (
    ambulance_id INT AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL UNIQUE,
    driver_name VARCHAR(100) NOT NULL,
    driver_contact VARCHAR(25) NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, ASSIGNED, ON_TRIP, OFFLINE
    authorization_status VARCHAR(25) NOT NULL DEFAULT 'AUTHORIZED', -- AUTHORIZED, PENDING, REVOKED
    current_location VARCHAR(255) DEFAULT 'Hospital Bay',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_amb_hosp (hospital_id),
    INDEX idx_amb_avail (status, authorization_status),
    CONSTRAINT fk_amb_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Emergency Requests Table
CREATE TABLE IF NOT EXISTS emergency_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    emergency_type VARCHAR(50) NOT NULL DEFAULT 'Ambulance',
    user_location VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(35) NOT NULL DEFAULT 'PENDING', 
    hospital_id INT NULL,
    ambulance_id INT NULL,
    assigned_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    rejection_reason VARCHAR(255) NULL,
    notes TEXT NULL,
    INDEX idx_req_status (status),
    INDEX idx_req_user (user_id),
    INDEX idx_req_hosp (hospital_id),
    CONSTRAINT fk_req_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id) ON DELETE SET NULL,
    CONSTRAINT fk_req_ambulance FOREIGN KEY (ambulance_id) REFERENCES ambulances(ambulance_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Seed Seed Hospitals (Password: Admin@123)
INSERT INTO hospitals (hospital_code, hospital_name, email, password_hash, location, contact, status)
VALUES 
('HOSP-01', 'Lilavati Hospital & Research Centre', 'admin@lilavati.org', '$2a$12$siNH.NTcwtfE1E0wq0BU0uFG6FIvimcghQ2CgxqDh9UX1bIuZ5SSK', 'Bandra West, Mumbai', '+91 22 2675 1000', 'ACTIVE'),
('HOSP-02', 'KEM Hospital Trauma Care', 'admin@kem.org', '$2a$12$siNH.NTcwtfE1E0wq0BU0uFG6FIvimcghQ2CgxqDh9UX1bIuZ5SSK', 'Parel, Mumbai', '+91 22 2410 7000', 'ACTIVE'),
('HOSP-03', 'Nanavati Super Speciality Hospital', 'admin@nanavati.org', '$2a$12$siNH.NTcwtfE1E0wq0BU0uFG6FIvimcghQ2CgxqDh9UX1bIuZ5SSK', 'Vile Parle West, Mumbai', '+91 22 2626 7500', 'ACTIVE')
ON DUPLICATE KEY UPDATE hospital_name=VALUES(hospital_name);

-- 5. Seed Ambulances for Lilavati (Hospital 1)
INSERT INTO ambulances (hospital_id, vehicle_number, driver_name, driver_contact, status, authorization_status, current_location)
VALUES 
(1, 'MH-02-ER-101', 'Ramesh Shinde', '+91 98201 12233', 'AVAILABLE', 'AUTHORIZED', 'Lilavati Emergency Bay 1'),
(1, 'MH-02-ER-102', 'Anand Verma', '+91 98201 12244', 'AVAILABLE', 'AUTHORIZED', 'Lilavati Rapid Response Bay 2'),
(1, 'MH-02-ER-103', 'Deepak Patil', '+91 98201 12255', 'OFFLINE', 'PENDING', 'Workshop Maintenance'),
(2, 'MH-01-TM-201', 'Sanjay Mane', '+91 98191 13344', 'AVAILABLE', 'AUTHORIZED', 'KEM Trauma Wing Gate'),
(2, 'MH-01-TM-202', 'Vinod Salunkhe', '+91 98191 13355', 'AVAILABLE', 'AUTHORIZED', 'KEM North Bay'),
(3, 'MH-02-NV-301', 'Mahesh Jadhav', '+91 98331 14455', 'AVAILABLE', 'AUTHORIZED', 'Nanavati Front Bay')
ON DUPLICATE KEY UPDATE vehicle_number=VALUES(vehicle_number);
