# NCERT Learning Platform - Deployment Guide

## Overview

This guide covers the deployment process for the NCERT Learning Platform, including both backend (Spring Boot) and frontend (Angular) components.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   Database      │
│   (Angular)     │◄──►│   (Spring Boot) │◄──►│  (PostgreSQL)   │
│   GitHub Pages  │    │   Render/Heroku │    │   Render/Heroku │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │              ┌─────────────────┐              │
         └──────────────►│   Redis Cache   │◄─────────────┘
                        │   (Optional)    │
                        └─────────────────┘
```

## Prerequisites

### Required Accounts
- GitHub account (for code hosting and CI/CD)
- Render account (for primary deployment)
- Heroku account (alternative deployment)
- Docker Hub account (for container registry)

### Required Tools
- Git
- Node.js 18+
- Java 17+
- Maven 3.8+
- Docker (optional)

### Environment Variables

#### Backend Environment Variables
```bash
# Database
DATABASE_URL=postgresql://username:password@host:port/database

# GitHub API
GITHUB_TOKEN=your_github_personal_access_token
GITHUB_OWNER=ncert-content
GITHUB_REPO=ncert-books

# OpenAI API
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-3.5-turbo

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# JVM Configuration
JAVA_OPTS=-Xmx512m -Xms256m
```

## Deployment Options

### Option 1: Render (Recommended)

#### Backend Deployment

1. **Create Render Account**
   - Sign up at [render.com](https://render.com)
   - Choose the free tier or appropriate paid plan

2. **Connect GitHub Repository**
   - Go to Dashboard → New → Web Service
   - Connect your GitHub repository
   - Select the `EduResolve_Backend` repository

3. **Configure Service**
   ```yaml
   # render.yaml (already included in repo)
   services:
     - type: web
       name: eduresolve-backend
       env: docker
       dockerfilePath: ./Dockerfile
       plan: free
       healthCheckPath: /actuator/health
   ```

4. **Set Environment Variables**
   - Navigate to Service → Environment
   - Add all required environment variables
   - Mark sensitive variables as "Secret"

5. **Add Database**
   - Go to Dashboard → New → PostgreSQL
   - Name: `eduresolve-db`
   - Plan: Free
   - Database name: `ncert_learning`

6. **Deploy**
   - Render will automatically deploy on push to main branch
   - Monitor deployment logs in the dashboard

#### Frontend Deployment

1. **Build Frontend**
   ```bash
   cd EduResolve-UI
   npm install
   npm run build
   ```

2. **Deploy to GitHub Pages**
   - Go to repository Settings → Pages
   - Source: Deploy from a branch
   - Branch: `gh-pages` or `main` with `/docs` folder
   - Use GitHub Actions workflow for automated deployment

### Option 2: Heroku (Alternative)

#### Backend Deployment

1. **Install Heroku CLI**
   ```bash
   # macOS
   brew tap heroku/brew && brew install heroku
   
   # Windows
   # Download from https://devcenter.heroku.com/articles/heroku-cli
   ```

2. **Login to Heroku**
   ```bash
   heroku login
   ```

3. **Create Heroku App**
   ```bash
   heroku create eduresolveve-backend
   ```

4. **Add PostgreSQL Addon**
   ```bash
   heroku addons:create heroku-postgresql:hobby-dev
   ```

5. **Set Environment Variables**
   ```bash
   heroku config:set GITHUB_TOKEN=your_token
   heroku config:set OPENAI_API_KEY=your_key
   heroku config:set SPRING_PROFILES_ACTIVE=production
   ```

6. **Deploy**
   ```bash
   git push heroku main
   ```

#### Frontend Deployment

Same as Render option - deploy to GitHub Pages.

### Option 3: Docker Deployment

#### Build Docker Image
```bash
cd EduResolve_Backend
docker build -t forgestackk/eduresolveve-backend:latest .
```

#### Push to Registry
```bash
docker login
docker push forgestackk/eduresolveve-backend:latest
```

#### Run Container
```bash
docker run -d \
  --name eduresolveve-backend \
  -p 8080:8080 \
  -e DATABASE_URL=your_db_url \
  -e GITHUB_TOKEN=your_token \
  -e OPENAI_API_KEY=your_key \
  forgestackk/eduresolveve-backend:latest
```

## CI/CD Pipeline

### GitHub Actions Workflow

The repository includes a comprehensive CI/CD pipeline (`.github/workflows/ci-cd.yml`) that:

1. **Triggers on**: Push to main/develop, Pull requests
2. **Runs tests**: Backend (Maven) + Frontend (Jest)
3. **Security scans**: Trivy vulnerability scanner
4. **Builds artifacts**: JAR file + Dist folder
5. **Deploys**: 
   - Staging (develop branch → Render)
   - Production (main branch → Heroku + GitHub Pages)
   - Docker images to Docker Hub

### Required Secrets

Add these to GitHub repository Settings → Secrets and variables → Actions:

```bash
HEROKU_API_KEY=your_heroku_api_key
HEROKU_APP_NAME=eduresolveve-backend
HEROKU_EMAIL=your_email@example.com
RENDER_API_KEY=your_render_api_key
RENDER_SERVICE_ID=your_render_service_id
DOCKER_USERNAME=your_docker_username
DOCKER_PASSWORD=your_docker_password
SLACK_WEBHOOK_URL=your_slack_webhook_url
GITHUB_TOKEN=your_github_token
```

## Database Setup

### Initial Migration

The application will automatically run database migrations on startup. For manual setup:

```sql
-- Connect to your PostgreSQL database
psql $DATABASE_URL

-- Run the migration script
\i src/main/resources/db/migration/V1__Create_Ncert_Tables.sql
```

### Sample Data

The migration script includes sample data for testing:
- 4 NCERT books (Class 9 & 10)
- Sample chapters and content blocks
- Sample quiz questions

## Monitoring and Logging

### Health Checks

- **Backend**: `/actuator/health`
- **Frontend**: Available at the deployed URL

### Logging

Configure logging levels in `application-prod.yaml`:

```yaml
logging:
  level:
    com.forgeStackk.EduResolve: INFO
    org.springframework.web: WARN
    org.hibernate.SQL: WARN
```

### Monitoring Services

1. **Render Dashboard**: Built-in metrics and logs
2. **Heroku Dashboard**: Metrics, logs, and performance monitoring
3. **Application Metrics**: Spring Boot Actuator endpoints

## Security Considerations

### Environment Variables
- Never commit sensitive data to version control
- Use platform-specific secret management
- Rotate API keys regularly

### HTTPS
- All deployments should use HTTPS
- Configure SSL certificates (handled automatically by Render/Heroku)

### CORS
Configure CORS in `application.yaml`:

```yaml
spring:
  web:
    cors:
      allowed-origins: "https://yourdomain.com"
      allowed-methods: "GET,POST,PUT,DELETE"
      allowed-headers: "*"
```

## Performance Optimization

### Backend
- Use connection pooling (HikariCP)
- Enable JVM optimizations
- Configure appropriate heap size

### Frontend
- Enable production builds
- Use lazy loading for large components
- Implement caching strategies

### Database
- Add appropriate indexes
- Use connection pooling
- Monitor query performance

## Troubleshooting

### Common Issues

1. **Build Failures**
   - Check Java version compatibility
   - Verify all dependencies are available
   - Review build logs for specific errors

2. **Database Connection Issues**
   - Verify DATABASE_URL format
   - Check database service status
   - Ensure proper credentials

3. **Environment Variable Issues**
   - Confirm all required variables are set
   - Check for typos in variable names
   - Verify secret access permissions

4. **Deployment Failures**
   - Review deployment logs
   - Check service health endpoint
   - Verify all dependencies are installed

### Debug Commands

```bash
# Check application logs (Render)
render logs eduresolveve-backend

# Check application logs (Heroku)
heroku logs --tail

# Test health endpoint
curl https://your-app-url.com/actuator/health

# Check database connection
psql $DATABASE_URL -c "SELECT COUNT(*) FROM ncert_books;"
```

## Maintenance

### Regular Tasks

1. **Update Dependencies**
   ```bash
   # Backend
   ./mvnw versions:display-dependency-updates
   
   # Frontend
   npm outdated
   ```

2. **Database Maintenance**
   ```sql
   -- Update statistics
   ANALYZE;
   
   -- Clean up old audit logs
   DELETE FROM audit_log WHERE timestamp < NOW() - INTERVAL '1 year';
   ```

3. **Security Updates**
   - Monitor for security advisories
   - Update dependencies regularly
   - Review access permissions

### Backup Strategy

1. **Database Backups**
   - Configure automatic backups (Render/Heroku)
   - Export regular backups to local storage
   - Test restore procedures

2. **Code Backups**
   - Git provides version control
   - Tag releases for easy rollback
   - Maintain feature branches

## Scaling

### Horizontal Scaling

1. **Backend**
   - Add more web service instances
   - Load balance between instances
   - Use Redis for session storage

2. **Database**
   - Upgrade to higher-tier plans
   - Implement read replicas
   - Consider sharding for large datasets

### Vertical Scaling

1. **Increase Resources**
   - Upgrade to larger instance sizes
   - Add more memory and CPU
   - Optimize JVM settings

## Rollback Procedures

### Backend Rollback

1. **Render**
   - Go to Service → Deployments
   - Click "Rollback" on previous deployment
   - Monitor rollback progress

2. **Heroku**
   ```bash
   heroku rollback
   ```

3. **Manual**
   ```bash
   git checkout previous-commit
   git push heroku main
   ```

### Frontend Rollback

1. **GitHub Pages**
   - Revert to previous commit
   - Wait for GitHub Pages to rebuild

2. **CDN**
   - Invalidate CDN cache
   - Wait for propagation

## Support

### Documentation
- [API Documentation](API.md)
- [Database Documentation](DATABASE.md)
- [Frontend Documentation](../EduResolve-UI/docs/FRONTEND.md)

### Contact
- Create GitHub issues for bugs
- Use discussions for questions
- Review existing issues before creating new ones

This deployment guide provides comprehensive instructions for deploying the NCERT Learning Platform to production environments.
