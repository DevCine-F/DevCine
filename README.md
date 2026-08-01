<div align="center">

# DevCine

**A full-featured cinema management platform**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-4.x-06B6D4?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

</div>

---

## Overview

DevCine handles the day-to-day operations of a cinema chain — from movie scheduling and online ticket sales to in-house POS ticketing, food & beverage sales, and customer loyalty programs.

**Core features:**

- Movie catalog, showtimes, room layouts, and seat maps
- Online ticket booking with VNPAY payment and QR code generation
- F&B / combo sales at POS (unlimited stock — no BOM/inventory)
- POS ticketing & QR check-in — pure RBAC (no work-shift requirement), with **Strict Cinema Scoping**: staff only sell / check in for their own cinema; cross-cinema actions are rejected with 403. Each POS order records `sold_by` (staff) and, for F&B, `cinema_id`.
- Promotions, vouchers, membership tiers, loyalty points
- Customer support tickets, lost & found tracking
- Admin dashboard with full audit logging

---

## Tech Stack

| Layer    | Stack                                                                         |
| -------- | ----------------------------------------------------------------------------- |
| Backend  | Java 25 · Spring Boot 4.0.6 · Spring Security · Spring Data JPA · Hibernate 7 |
| Frontend | Vue 3.5 · Vite 8 · Pinia 3 · Vue Router 5 · Tailwind CSS 4                    |
| Database | PostgreSQL 15+ on Supabase                                                    |
| Auth     | JWT with HMAC-SHA256                                                          |
| Build    | Maven (backend) · npm (frontend)                                              |

---

## Quick Start

```bash
# clone
git clone https://github.com/DevCine-F/devcine.git
cd devcine

# backend — set up environment
cd devcine-backend
cp .env.example .env        # fill in your database credentials

# frontend — install deps
cd ../devcine-frontend
npm install

# run everything
npm run dev:all              # backend :8080 + frontend :5173
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for full setup and workflow details.

---

## Project Structure

```
devcine/
├── devcine-backend/            Spring Boot REST API
│   └── src/.../
│       ├── entity/             33 JPA entities
│       ├── controller/         REST endpoints
│       ├── service/            business logic
│       ├── repository/         data access layer
│       ├── dto/                request/response objects
│       ├── config/             security, CORS, JWT
│       └── exception/          global error handling
│
├── devcine-frontend/           Vue.js single-page app
│   └── src/
│       ├── views/              page components
│       ├── components/         reusable UI
│       ├── stores/             Pinia state management
│       ├── routers/            route definitions
│       └── utils/              helpers and API client
│
├── docs/                       technical documentation
└── RULES.md                    development rules
```

---

## Documentation

| Document                                    | What it covers                                               |
| ------------------------------------------- | ------------------------------------------------------------ |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md)     | System design, tech choices, data flow                       |
| [DATABASE.md](docs/DATABASE.md)             | All 33 tables with columns, types, and relationships         |
| [API_CONTRACTS.md](docs/API_CONTRACTS.md)   | Every endpoint — methods, auth levels, sample payloads       |
| [CRITICAL_PATHS.md](docs/CRITICAL_PATHS.md) | Booking, payment, and check-in flows with protected file list |
| [SECURITY.md](docs/SECURITY.md)             | Validation, JWT config, rate limiting, CORS policy           |

---

## Team

| Member              | Role |
| ------------------- | ---- |
| Nguyễn Quang Huy    | —    |
| Văn Minh Khôi       | —    |
| Phạm Thị Quỳnh Anh  | —    |
| Nguyễn Ngọc Hà Linh | —    |

---

## License

Academic project — graduation thesis.
