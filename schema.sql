-- PulseRoute MySQL Database Initialization Script
-- Database: pulseroute

CREATE DATABASE IF NOT EXISTS pulseroute CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pulseroute;

-- Citizens / Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    mobile VARCHAR(15) NOT NULL,
    citizen_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    emergency_contact VARCHAR(15) NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_citizen_id (citizen_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed a test citizen user for verification if needed
-- Note: password will be hashed using BCrypt in application. 
-- Below is a sample user with citizen_id 'CIT-9901' and password 'Pulse@123' (BCrypt hashed)
INSERT INTO users (full_name, email, mobile, citizen_id, password, emergency_contact, address)
VALUES (
    'Aditya Sharma',
    'aditya.sharma@pulseroute.gov',
    '9876543210',
    'CIT-9901',
    '$2a$12$e6WwT41f.j1y06D4h3U.g.2K6zV7WbEwT48KjH0j65nL8k8F9U4lC', -- Pulse@123
    '9876500000',
    'Flat 402, Sea Green Heights, Worli Sea Face, Mumbai 400018'
) ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);
