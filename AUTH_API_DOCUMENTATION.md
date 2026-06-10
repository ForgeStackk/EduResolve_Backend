# EduResolve Authentication System

This document describes the authentication system for the EduResolve application.

## Overview

The authentication system allows users to:
1. **Register** - Create a new account on their first login
2. **Login** - Access the system with existing credentials
3. **Logout** - End their session

## API Endpoints

All endpoints are prefixed with `/api/auth` and accept JSON requests.

### 1. Register (POST /api/auth/register)

**Purpose:** Create a new user account

**Request Body:**
```json
{
  "name": "John Doe",
  "className": "10A",
  "email": "john@example.com",
  "password": "SecurePassword123",
  "role": "student",
  "phoneNumber": "+1-234-567-8900"
}
```

**Response (Success):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "student",
  "className": "10A",
  "phoneNumber": "+1-234-567-8900",
  "success": true,
  "message": "Registration successful!"
}
```

**Response (Error - Email Already Exists):**
```json
{
  "success": false,
  "message": "Email already registered. Please login instead."
}
```

**Status Codes:**
- `200 OK` - Registration successful
- `400 Bad Request` - Invalid input or email already exists

---

### 2. Login (POST /api/auth/login)

**Purpose:** Authenticate a user and retrieve their information

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```

**Response (Success):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "student",
  "className": "10A",
  "phoneNumber": "+1-234-567-8900",
  "success": true,
  "message": "Login successful!"
}
```

**Response (Error - Invalid Credentials):**
```json
{
  "success": false,
  "message": "Invalid password. Please try again."
}
```

**Response (Error - User Not Found):**
```json
{
  "success": false,
  "message": "Email not found. Please register first."
}
```

**Status Codes:**
- `200 OK` - Login successful
- `401 Unauthorized` - Invalid credentials

---

### 3. Logout (POST /api/auth/logout)

**Purpose:** End the user session

**Request Body:** Empty or any value (logout is client-side handled)

**Response:**
```json
"Logout successful! Session cleared."
```

**Status Codes:**
- `200 OK` - Logout successful

---

## Database Schema

The system uses a PostgreSQL table `user_login`:

```sql
CREATE TABLE user_login (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  class_name VARCHAR(10),
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20),
  phone_number VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Fields:**
- `id` - Unique identifier (auto-generated)
- `name` - Full name of the user
- `class_name` - School/class (optional)
- `email` - Unique email address
- `password` - Hashed password (BCrypt)
- `role` - User role (student, teacher, admin)
- `phone_number` - Contact number (optional)
- `created_at` - Account creation timestamp
- `updated_at` - Last updated timestamp

---

## Security Features

1. **Password Hashing:** Passwords are hashed using BCrypt with a strength factor of 10
2. **Email Uniqueness:** Email addresses are unique, preventing duplicate accounts
3. **CORS Protection:** API is protected with CORS headers
4. **Input Validation:** All inputs are validated on both frontend and backend

---

## Frontend Implementation

### Login Component Location
- File: `src/pages/Login.jsx`
- Features:
  - Toggle between login and registration forms
  - Form validation on client-side
  - Error display
  - Automatic redirect based on user role

### Authentication Context
- File: `src/context/AuthContext.jsx`
- Features:
  - Stores user information
  - Manages login/logout state
  - Persists user data in localStorage
  - Auto-restore user on page reload

### Using AuthContext in Components
```javascript
import { useAuth } from '../context/AuthContext';

export function MyComponent() {
  const { user, isLoggedIn, logout } = useAuth();
  
  if (!isLoggedIn) {
    return <Redirect to="/login" />;
  }
  
  return <div>Welcome, {user.name}!</div>;
}
```

---

## Roles & Permissions

Currently supported roles:
- **student** - Can report issues, track progress
- **teacher** - Can manage student issues, provide feedback
- **admin** - Full system access

---

## Testing the API

### Using cURL

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "className": "10A",
    "email": "john@example.com",
    "password": "password123",
    "role": "student",
    "phoneNumber": "1234567890"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

## Troubleshooting

### "Network error" in login form
- Ensure the backend is running on `http://localhost:8080`
- Check CORS headers in the backend
- Verify the request is being sent correctly

### "Email not found" on login
- User hasn't registered yet
- Email address is spelled differently

### "Invalid password" on login
- Password is case-sensitive
- Caps Lock might be on
- Password was typed incorrectly

### Database connection issues
- Verify PostgreSQL is running
- Check database credentials in `application.yaml`
- Ensure the database `eduresolve` exists

---

## Configuration

### Backend Configuration
File: `src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/eduresolve
    username: postgres
    password: ForgeSt@ckk_1999
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### Frontend Configuration
Modify the API base URL in `src/pages/Login.jsx`:
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  // ...
});
```

---

## Future Enhancements

1. **JWT Tokens** - Implement JWT for stateless authentication
2. **Email Verification** - Send confirmation emails for new registrations
3. **Password Reset** - Allow users to reset forgotten passwords
4. **OAuth Integration** - Support Google/GitHub login
5. **Rate Limiting** - Prevent brute-force attacks
6. **Two-Factor Authentication** - Add extra security layer
