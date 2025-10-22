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

## Infrastructure

### Database (MySQL 8.0)
- Connection: `jdbc:mysql://localhost:3306/pointsync`
- Timezone: Asia/Seoul
- DDL Strategy: `update` (auto-migration)
- Required: MySQL server running on port 3306

### Redis
- Host: `localhost:6379`
- Purpose: Idempotent check-in deduplication cache
- Required: Redis server running on port 6379

### Monitoring
- Prometheus metrics via Micrometer
- Actuator endpoints enabled

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
Use Java records for response DTOs:
```java
public record CheckInResult(
    boolean attendedToday,
    LocalDate date,
    int todayPoint,
    int totalPoints
) {}
```

### Exception Handling
- Custom exceptions extend base exception classes
- Global exception handler in `@RestControllerAdvice`
- Return appropriate HTTP status codes (201 CREATED, 204 NO_CONTENT, etc.)

## Configuration Files

- `src/main/resources/application.yml` - Main configuration (DB credentials, JPA settings, Redis, logging)
- `build.gradle` - Dependencies and build configuration
- Java 17 required (configured in toolchain)

## Current Status

**Implemented:**
- User service & controller (registration, lookup, deletion)
- AttendanceLog service & controller (check-in, calendar, summary, streak tracking)
- TimeProvider pattern for timezone consistency
- Redis caching for idempotency

**Not Implemented:**
- PointLedgerService and PointLedgerController (stub files only - business logic needed)

## Important Notes

- **Database credentials in application.yml**: Password is hardcoded (not production-ready). Consider externalizing to environment variables.
- **Timezone**: Entire application operates in Asia/Seoul timezone. All date operations must respect KST.
- **Check-in points**: Currently hardcoded as `10` in `AttendanceLogService`. May need configuration in future.
- **Streak calculation**: Queries database for each day iteration - could be optimized with batch queries for very long streaks.
