# EduResolve_Backend
Backend integration with EduResolve App to make it interactive &amp; data driven

---

## Keycloak Setup (OIDC Authentication)

### 1. Run Keycloak via Docker

```bash
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.2 \
  start-dev
```

Admin console → http://localhost:8180/admin  
Login with **admin / admin**

---

### 2. Create the Realm

1. Click **Create realm**
2. Realm name: `eduresolve`
3. Click **Create**

---

### 3. Create the Client

1. In the `eduresolve` realm → **Clients** → **Create client**
2. Client type: `OpenID Connect`
3. Client ID: `eduresolve-frontend`
4. Click **Next**
5. **Standard flow** enabled: ✓ (Authorization Code Flow)
6. Click **Next**
7. Valid redirect URIs: `http://localhost:4200/*`
8. Web origins: `http://localhost:4200`
9. Click **Save**

---

### 4. Create Application Roles

In the `eduresolve` realm → **Realm roles** → **Create role** — create all four:

| Role name |
|-----------|
| `student` |
| `teacher` |
| `admin`   |
| `parent`  |

---

### 5. Create Test Users (one per role)

Repeat the steps below for each row in the table.

**Steps (per user):**
1. **Users** → **Create new user**
2. Fill in: Username, Email, First name, Last name
3. Click **Create**
4. **Credentials** tab → **Set password** → disable "Temporary" → click **Save password**
5. **Role mapping** tab → **Assign role** → select the role from the table below

| Username | Email | First | Last | Role |
|---|---|---|---|---|
| `admin_test` | `admin@eduresolve.local` | Admin | User | `admin` |
| `teacher_test` | `teacher@eduresolve.local` | Teacher | User | `teacher` |
| `student_test` | `student@eduresolve.local` | Student | User | `student` |
| `parent_test` | `parent@eduresolve.local` | Parent | User | `parent` |

> **Important:** Each Keycloak email must exactly match the `email` column in the local PostgreSQL `user_login` table (created via `POST /api/auth/register`). The backend's `GET /api/auth/profile` endpoint links the two records by this email claim.

---

### 6. Registration flow

With Keycloak as the auth provider, user accounts exist in two places:

1. **Keycloak** — handles authentication (password, MFA, SSO)
2. **Local PostgreSQL** (`user_login` table) — stores school-specific data (className, schoolName, studentId, etc.)

**To register a new user:**

1. Call `POST /api/auth/register` (the existing signup form or API) to create the local DB record with all school details
2. In the Keycloak admin console, create a matching user with the same email and assign the correct role

The frontend signup page (`/auth/signup`) still creates the local DB user. Keycloak user creation is a manual admin step for now.

---

### 7. Environment variables (optional)

To point to a different Keycloak URL, set:

```bash
# Backend
KEYCLOAK_ISSUER_URI=http://your-keycloak-host/realms/eduresolve

# Frontend — update src/environments/environment.ts
keycloakIssuer: 'http://your-keycloak-host/realms/eduresolve'
```
