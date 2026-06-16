# Simple Timetable Generator (STG.kpik)

A web application for **automatic university timetable generation**.  
Uses the [Timefold](https://timefold.ai/) AI solver to optimally assign lessons to days, time slots, and classrooms while respecting hard and soft constraints.

## Features

-  **Import teaching loads** from MS Access (`.accdb`) or Excel (`.xlsx`) files
-  **AI-powered schedule optimization** via Timefold Solver (constraint-based)
-  **Dynamic pairs-per-week calculation** — supports different semester durations per student group (via `Group_Weeks` table in Access)
-  **Numerator/Denominator week support** — fractional pairs (e.g. 0.5) are automatically split into alternating weeks
-  **Web UI** built with Thymeleaf + TailwindCSS + Alpine.js

##  Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.4.3, Spring Data JPA, Hibernate |
| **AI Solver** | Timefold Solver 1.10.0 |
| **Database** | PostgreSQL (via Docker Compose) |
| **Data Import** | UCanAccess 5.0.1 (JDBC for MS Access), Apache POI 5.2.5 (Excel) |
| **Frontend** | Thymeleaf, TailwindCSS, Alpine.js |
| **Build** | Maven, frontend-maven-plugin (Node v20.11.0) |
| **Testing** | JUnit 5, Mockito |

## Prerequisites

- **Java** 21+
- **Docker** (for PostgreSQL) or a local PostgreSQL instance
- **Maven** 3.9+ (or use the included `mvnw` wrapper)

##  Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/mykhailokasiian14/simple_timetable_generator.git
cd simple_timetable_generator
```

### 2. Start PostgreSQL

The project ships with a ready-to-use `compose.yaml`:

```bash
docker compose up -d
```

This spins up PostgreSQL with:
- **DB:** `mydatabase`
- **User:** `myuser`
- **Password:** `secret`
- **Port:** `5432`

### 3. Run the application

```bash
./mvnw spring-boot:run
```

> On the first run, Maven will automatically install Node.js, npm dependencies, and build TailwindCSS.

### 4. Open in browser

```
http://localhost:8080/
```

## Usage

1. Navigate to the **Upload page** (`/upload`)
2. **Select file type** — Access (`.accdb`) or Excel (`.xlsx`)
3. **For Access files:** choose the semester (1st or 2nd) — determines which hours column and weeks to use
4. **Upload** the teaching load file
5. Click **"Upload and continue"**
6. On the **generation page**, click **"Run"** — the solver will work for up to 3 minutes
7. **View the result** on the `/result` page

## Expected Access Database Schema (`.accdb`)

### Core tables (required)

| Table                   | Columns                                                                                                                        |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| **Groups**              | `groupId` (PK), `groupName` (group name)                                                                                       |
| **Subjects**            | `subjectId` (PK), `subjId` (subject name)                                                                                      |
| **Teachers**            | `teacherId` (PK), `teacherName` (teacher's last name)                                                                          |
| **DistributionOfHours** | `Group` (FK → Groups), `Subject` (FK → Subjects), `Teacher` (FK → Teacher), `1sem` (hours for sem 1), `2sem` (hours for sem 2) |

### Weeks table (optional)

| Table | Columns                                                                                     |
|-------|---------------------------------------------------------------------------------------------|
| **Group_Weeks** | `Group_Name` (TEXT, must match `groupName`), `Sem1_Weeks` (INTEGER), `Sem2_Weeks` (INTEGER) |

> If the `Group_Weeks` table is missing or a group is not found in it, the system falls back to **15 weeks**.

> ! If you have already similar db you need just to change names of tables and columns in SQL queries in AccessService.java from default to yours

### Pairs-per-week formula

```
pairsPerWeek = totalSemesterHours / (groupWeeks × 2)
```

Example: 60 hours, 10 weeks → `60 / (10 × 2) = 3.0` pairs per week.

## Solver Constraints

### Hard constraints (must not be violated)

| Constraint | Description |
|------------|-------------|
| **Teacher conflict** | A teacher cannot teach two lessons at the same time |
| **Group conflict** | A group cannot attend two lessons at the same time |
| **Auditory conflict** | A classroom cannot host two lessons at the same time |
| **No consecutive same subject** | The same subject cannot be scheduled in adjacent time slots on the same day |

> All three conflict constraints correctly handle numerator/denominator weeks — lessons on different week types do not conflict.

### Soft constraints (preferred but not mandatory)

| Constraint | Description |
|------------|-------------|
| **Diversify lesson times** | Lessons of the same subject for a group should be spread across different time slots |
| **One subject per day** | Preferably no more than one subject per day per group |
| **No gaps** | Minimize "windows" (idle slots) between lessons during the day |
| **Short Fridays & Wednesdays** | Prefer at most 3 lessons on Fridays and Wednesdays |
| **Room sharing penalty** | Penalize sharing the same classroom between different teachers |

## Project Structure

```
simple_timetable_generator/
├── compose.yaml                         # Docker Compose for PostgreSQL
├── pom.xml                              # Maven configuration
├── mvnw / mvnw.cmd                      # Maven Wrapper
│
├── src/main/java/ua/org/kpik/simple_timetable_generator/
│   ├── SimpleTimetableGeneratorApplication.java
│   ├── config/
│   │   └── DataLoader.java              # Initial classroom data seeding
│   ├── controller/
│   │   ├── rest_controller/
│   │   │   ├── AccessController.java
│   │   │   ├── ExcelController.java
│   │   │   └── TimetableController.java
│   │   └── web_controller/
│   │       └── PageControllerWeb.java   # Main web controller
│   ├── entity/
│   │   ├── Auditory.java
│   │   ├── Group.java
│   │   ├── Lesson.java                  # @PlanningEntity for Timefold
│   │   ├── Subject.java
│   │   ├── Teacher.java
│   │   └── TeachingLoad.java
│   ├── enaum/
│   │   └── LessonWeekType.java          # EVERY_WEEK / NUMERATOR / DENOMINATOR
│   ├── repository/                      # Spring Data JPA repositories
│   ├── service/
│   │   ├── AccessService.java           # .accdb file parsing
│   │   ├── ExcelService.java            # .xlsx file parsing (not recommended to use at the moment)
│   │   └── TimetableService.java        # Lesson generation & solver runner
│   └── solver/
│       ├── Timetable.java               # @PlanningSolution
│       └── TimetableConstraintProvider.java  # Hard/Soft constraints
│
├── src/main/resources/
│   ├── application.properties
│   ├── static/css/style.css             # Compiled TailwindCSS
│   └── templates/                       # Thymeleaf templates
│       ├── index.html
│       ├── upload.html
│       ├── generate.html
│       ├── result.html
│       ├── tutorial.html
│       └── about.html
│
├── src/main/frontend/                   # TailwindCSS source
│   ├── package.json
│   ├── tailwind.config.js
│   └── src/input.css
│
└── src/test/java/                       # Will be more tests
    └── .../service/
        └── AccessServiceTest.java       # Unit tests for AccessService
```

## Configuration

### application.properties

| Property | Value | Description |
|----------|-------|-------------|
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Recreates tables on every startup |
| `timefold.solver.termination.spent-limit` | `180s` | Max solver runtime (3 minutes) |

### Docker Compose (PostgreSQL)

| Parameter | Value |
|-----------|-------|
| Image | `postgres:latest` |
| DB name | `mydatabase` |
| User | `myuser` |
| Password | `secret` |
| Port | `5432` |

##  Testing

```bash
./mvnw test -Dtest=AccessServiceTest
```

`AccessService` test coverage:
- ✅ Pairs-per-week calculation with `Group_Weeks` table (semester 1 & 2)
- ✅ Fallback to 15 weeks (table missing / group not found)
- ✅ Skipping rows with 0 hours
- ✅ Fractional pairs (0.5 — alternating weeks)
- ✅ Multiple groups with different week counts
- ✅ Reuse of existing entities from the database


##  Author

**Mykhailo Kasiian** — РПЗ-231 [KPIK](https://kpik.org.ua/), [LinkedIn](https://www.linkedin.com/in/mykhailo-kasiian-855b4a362/), [Insta](https://www.instagram.com/aye.mishka16/), [Telegram](https://t.me/cmtgmiskkhaaa)
