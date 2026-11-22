# Job Application Tracker

A full-stack web app for logging job applications, moving them through stages and seeing progress on a dashboard.

**Status:** slice 1 of 6 (project skeleton). Target: v1.0.0 on Dec 1, 2026.

## Stack

- Backend: Java 17, Spring Boot 4.1, Spring Data JPA (Hibernate), Flyway, PostgreSQL
- Frontend: React with Vite
- Later: Spring Security with JWT, MapStruct, OpenAPI, GitHub Actions CI, Docker, AWS, Kubernetes

## Structure

```text
backend/    Spring Boot API, modular monolith (auth, user, application, common)
frontend/   React app
docs/       architecture document and notes
SETUP.md    how to install, run and push this project
```

## Run it

See [SETUP.md](SETUP.md).
