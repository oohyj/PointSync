# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PointSync is a Spring Boot attendance tracking and points management system that gamifies daily check-ins. Users accumulate points through attendance activities, with streak tracking and a transactional points ledger.

## Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.project.pointsync.PointsyncApplicationTests

# Run tests with detailed output
./gradlew test --info
```

### Development
```bash
# Start with DevTools for hot reload
./gradlew bootRun

# Check dependencies
./gradlew dependencies

# View build tasks
./gradlew tasks
```

## Architecture

### Layered Structure
Standard Spring Boot 4-layer architecture:
- **Controllers** (`/controller`) - REST endpoints with `/api/` prefix
- **Services** (`/service`) - Business logic with `@Transactional` management
- **Repositories** (`/repository`) - Spring Data JPA interfaces extending `JpaRepository`
- **Domain** (`/domain`) - JPA entities with Lombok annotations

### Core Domain Model
```
User (1) ──────────┬────── (N) AttendanceLog
                   └────── (N) PointLedger
```

- **User**: Email-based identification (unique constraint), name limited to 50 chars
- **AttendanceLog**: Daily check-ins with composite unique constraint `(user_id, attend_date)` - max 1 per user per day
- **PointLedger**: Transaction log for point changes (amount + reason)
- **BaseTimeEntity**: Abstract parent with auto-managed `createdAt`/`updatedAt` timestamps

### Critical Patterns

#### TimeProvider Abstraction (KST Timezone Handling)
All time operations use the `TimeProvider` interface configured to Asia/Seoul timezone:
```java
public interface TimeProvider {
    ZoneId zone();
    LocalDate today();           // KST-aware current date
    long secondsUntilMidnight(); // For Redis TTL calculations
}
```
Implementation: `KstTimeProvider` with injected `Clock` bean. **Always use TimeProvider for date operations** - never use `LocalDate.now()` directly to ensure timezone consistency.

#### Idempotent Check-In Strategy
Three-tier idempotency in `AttendanceLogService.checkIn()`:
1. **Redis cache check** (fast path) - key pattern: `attendance:{userId}:{date}`
2. **Database unique constraint** on `(user_id, attend_date)` prevents duplicate inserts
3. **Exception absorption** - `DataIntegrityViolationException` treated as successful idempotent operation

Cache TTL is dynamically set to seconds until midnight (KST) using `TimeProvider.secondsUntilMidnight()`.

#### Entity Factory Methods
Domain entities use **protected constructors** with public static factory methods:
```java
// Good
User user = User.createUser("John Doe", "john@example.com");

// Bad - constructor is protected
User user = new User(...); // Won't compile
```

#### Streak Calculation Algorithm
- **Current Streak**: Iterates backwards from today until gap found (O(n) where n = streak length)
- **Longest Streak**: Scans 365-day window to find longest continuous sequence
- Both use `existsByUserIdAndAttendDate()` repository method

#### PointLedger Integration Pattern
Points are **decoupled from attendance**:
- Check-ins do NOT automatically create point records (AttendanceLogService returns `todayPoint: 0`)
- Points must be explicitly created via `POST /api/points` with userId, amount, and reason
- This separation enables flexible point rules (bonuses, penalties, manual adjustments)
- Total points are calculated on-demand via `sumAmountByUserId()` repository query

## Infrastructure

### Database (MySQL 8.0)
- Connection: Configured via environment variables
  - `DB_URL` (e.g., `jdbc:mysql://localhost:3306/pointsync`)
  - `DB_USERNAME`
  - `DB_PASSWORD`
- Timezone: Asia/Seoul
- DDL Strategy: `update` (auto-migration)
- Required: MySQL server running on port 3306

### Redis
- Host/Port: Configured via environment variables
  - `REDIS_HOST` (default: localhost)
  - `REDIS_PORT` (default: 6379)
- Purpose: Idempotent check-in deduplication cache
- Required: Redis server running

### Monitoring
- Prometheus metrics via Micrometer
- Actuator endpoints enabled

## REST API Endpoints

### User Management (`/api/users`)
- `POST /api/users` - Register new user (email, name)
- `GET /api/users/{id}` - Get user by ID
- `DELETE /api/users/{id}` - Delete user

### Attendance (`/api/attendance`)
- `POST /api/attendance/check-in` - Daily check-in (idempotent, KST-based)
- `GET /api/attendance/calendar` - Get attendance dates for date range
- `GET /api/attendance/summary` - Get summary (today's status, total points, streaks)

### Points (`/api/points`)
- `POST /api/points` - Create point record (body: `{userId, amount, reason}`)
  - Positive amount = credit, negative = debit
- `GET /api/points/total?userId={id}` - Get user's total points
- `GET /api/points/history?userId={id}&page={n}&size={n}` - Get paginated transaction history (newest first)

## Code Conventions

### Transaction Management
- Write operations: `@Transactional`
- Read operations: `@Transactional(readOnly = true)`
- Service layer owns transaction boundaries, not controllers

### Lazy Loading
- `open-in-view` is **disabled** in configuration
- All entity relationships use `fetch = FetchType.LAZY`
- Always fetch related entities within transactional service methods

### Query Optimization
Use projection queries in repositories to fetch only needed fields:
```java
@Query("select a.attendDate from AttendanceLog a where ...")
List<LocalDate> findDatesByUserIdAndRange(...);
```

### DTOs
Use Java records for response DTOs with **package-by-feature** organization:
```java
public record CheckInResult(
    boolean attendedToday,
    LocalDate date,
    int todayPoint,
    int totalPoints
) {}
```

DTO package structure:
- `dto/User/` - UserResDto
- `dto/AttendanceLog/` - CheckInResult, SummaryResult
- `dto/PointLedger/` - PointLedgerReqDto, PointLedgerResDto, PointTotalResDto, PointLedgerListResDto

### Exception Handling
- Custom exceptions extend base exception classes
- Global exception handler in `@RestControllerAdvice`
- Return appropriate HTTP status codes (201 CREATED, 204 NO_CONTENT, etc.)

## Configuration Files

- `src/main/resources/application.yml` - Main configuration (JPA settings, Redis, logging)
  - Uses environment variables for DB and Redis credentials
  - Defaults to empty strings if environment variables not set
- `build.gradle` - Dependencies and build configuration
- Java 17 required (configured in toolchain)

### Environment Variables
Required for running the application:
```bash
export DB_URL=jdbc:mysql://localhost:3306/pointsync
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

## Current Status

**Fully Implemented:**
- User service & controller (registration, lookup, deletion)
- AttendanceLog service & controller (check-in, calendar, summary, streak tracking)
- PointLedger service & controller (create records, get total, paginated history)
- TimeProvider pattern for timezone consistency
- Redis caching for idempotency

## Important Notes

- **Configuration**: All sensitive credentials (database, Redis) use environment variables - production-ready pattern.
- **Timezone**: Entire application operates in Asia/Seoul timezone. All date operations must respect KST.
- **Point allocation**: Check-ins do NOT automatically award points. Points must be created separately via PointLedger API to allow flexible reward rules.
- **Streak calculation**: Queries database for each day iteration - could be optimized with batch queries for very long streaks.
- **PointLedger reason field**: Limited to 50 characters (database constraint).
