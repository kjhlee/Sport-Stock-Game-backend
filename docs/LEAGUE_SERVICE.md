# League Service — Developer Reference

## Overview

The **League Service** manages the league lifecycle, memberships, and invite system for the SportStock fantasy football platform. It provides APIs for creating leagues, managing members, generating invite codes, and controlling league states from creation through archival.

**Tech Stack**: Spring Boot 4.0.3, Spring Data JPA, PostgreSQL, Flyway, Lombok, Jakarta Validation, Java 21

**Port**: `8100` (default, configurable via `LEAGUE_SERVICE_PORT`)

---

## Core Concepts

### League Lifecycle

A league progresses through three states:

1. **INACTIVE** (initial state)
   - League is created but not yet started
   - Owner can invite members via invite codes
   - Members can join freely
   - Members can leave
   - Owner can remove members

2. **ACTIVE** (league has started)
   - Season is in progress
   - No new members can join
   - Members cannot leave
   - Owner cannot remove members
   - Initial stipend has been issued (or will be)

3. **ARCHIVED** (league has ended)
   - Season is complete
   - League is read-only
   - Historical data preserved

### Roles

- **OWNER** - League creator with administrative privileges
- **MEMBER** - Regular league participant

### Authentication

All endpoints require a valid JWT token in the `Authorization` header. User ID is extracted from JWT claims via the centralized `CurrentUserProvider` in `services/common`.

**Header format:**
```
Authorization: Bearer <accessToken>
```

Where `<accessToken>` is obtained from the user-authentication service's login/register endpoints.

**How it works:**
1. Client includes `Authorization: Bearer <token>` in request headers
2. `JwtAuthenticationFilter` validates token via `JwtValidationService`
3. User ID is extracted from JWT claims and set in `SecurityContextHolder`
4. `CurrentUserProvider` retrieves the user ID from `SecurityContextHolder` for authorization checks

---

## Base URL

```
/api/v1/leagues
```

---

## API Reference

### League Management

#### Create League

Creates a new league with the authenticated user as owner.

**Request:**
```http
POST /api/v1/leagues
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "name": "Sunday Night Warriors",
  "maxMembers": 12,
  "seasonStartAt": "2024-09-05T17:00:00Z",
  "seasonEndAt": "2025-02-10T23:59:59Z",
  "initialStipendAmount": 10000.00,
  "weeklyStipendAmount": 500.00,
  "weeklyPayoutDowUtc": 1
}
```

**Validation:**
- `name`: Required, max 255 characters
- `maxMembers`: Required, minimum 2
- `seasonStartAt`: Required, must be in future
- `seasonEndAt`: Required, must be after seasonStartAt
- `initialStipendAmount`: Required, must be positive
- `weeklyStipendAmount`: Required, must be positive
- `weeklyPayoutDowUtc`: Required, 0-6 (0=Sunday, 6=Saturday)

**Response (201 Created):**
```json
{
  "id": 1,
  "ownerUserId": 12345,
  "name": "Sunday Night Warriors",
  "status": "INACTIVE",
  "maxMembers": 12,
  "seasonStartAt": "2024-09-05T17:00:00Z",
  "seasonEndAt": "2025-02-10T23:59:59Z",
  "initialStipendAmount": 10000.00,
  "weeklyStipendAmount": 500.00,
  "weeklyPayoutDowUtc": 1,
  "startedAt": null,
  "createdAt": "2024-08-01T10:30:00Z",
  "memberCount": 1
}
```

---

#### Get League Details

Retrieves league information. Requires league membership.

**Request:**
```http
GET /api/v1/leagues/{leagueId}
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerUserId": 12345,
  "name": "Sunday Night Warriors",
  "status": "ACTIVE",
  "maxMembers": 12,
  "seasonStartAt": "2024-09-05T17:00:00Z",
  "seasonEndAt": "2025-02-10T23:59:59Z",
  "initialStipendAmount": 10000.00,
  "weeklyStipendAmount": 500.00,
  "weeklyPayoutDowUtc": 1,
  "startedAt": "2024-09-01T12:00:00Z",
  "createdAt": "2024-08-01T10:30:00Z",
  "memberCount": 8
}
```

**Errors:**
- `403 FORBIDDEN` - User is not a member of the league
- `404 NOT_FOUND` - League does not exist

---

#### List User's Leagues

Returns paginated list of leagues the user is a member of.

**Request:**
```http
GET /api/v1/leagues?page=0&size=20
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "ownerUserId": 12345,
      "name": "Sunday Night Warriors",
      "status": "ACTIVE",
      "maxMembers": 12,
      "memberCount": 8,
      "seasonStartAt": "2024-09-05T17:00:00Z",
      "seasonEndAt": "2025-02-10T23:59:59Z",
      "createdAt": "2024-08-01T10:30:00Z"
    },
    {
      "id": 2,
      "ownerUserId": 67890,
      "name": "Fantasy Champions 2024",
      "status": "INACTIVE",
      "maxMembers": 10,
      "memberCount": 5,
      "seasonStartAt": "2024-09-05T17:00:00Z",
      "seasonEndAt": "2025-02-10T23:59:59Z",
      "createdAt": "2024-08-05T14:20:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 2,
  "totalPages": 1
}
```

---

#### Start League

Starts a league, transitioning from INACTIVE to ACTIVE. Only the owner can start a league. Requires minimum 2 members.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/start
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerUserId": 12345,
  "name": "Sunday Night Warriors",
  "status": "ACTIVE",
  "maxMembers": 12,
  "memberCount": 8,
  "seasonStartAt": "2024-09-05T17:00:00Z",
  "seasonEndAt": "2025-02-10T23:59:59Z",
  "initialStipendAmount": 10000.00,
  "weeklyStipendAmount": 500.00,
  "weeklyPayoutDowUtc": 1,
  "startedAt": "2024-09-01T12:00:00Z",
  "createdAt": "2024-08-01T10:30:00Z"
}
```

**Errors:**
- `403 FORBIDDEN` - User is not the league owner
- `409 CONFLICT` - League is not INACTIVE, or has fewer than 2 members

---

#### Archive League

Archives a league, marking it as complete. Only the owner can archive.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/archive
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerUserId": 12345,
  "name": "Sunday Night Warriors",
  "status": "ARCHIVED",
  "maxMembers": 12,
  "memberCount": 8,
  "seasonStartAt": "2024-09-05T17:00:00Z",
  "seasonEndAt": "2025-02-10T23:59:59Z",
  "startedAt": "2024-09-01T12:00:00Z",
  "createdAt": "2024-08-01T10:30:00Z"
}
```

**Errors:**
- `403 FORBIDDEN` - User is not the league owner
- `409 CONFLICT` - League is already ARCHIVED

---

### Invite Management

#### Create Invite

Generates a unique invite code for a league. Only the owner can create invites, and the league must be INACTIVE.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/invites
Content-Type: application/json
Authorization: Bearer <accessToken>

{
  "expiresAt": "2024-09-01T23:59:59Z",
  "maxUses": 5
}
```

**Validation:**
- `expiresAt`: Optional, must be in future
- `maxUses`: Optional, must be positive

**Response (201 Created):**
```json
{
  "id": 42,
  "code": "XJ4K9P2M7N5Q",
  "expiresAt": "2024-09-01T23:59:59Z",
  "maxUses": 5,
  "usesCount": 0,
  "createdAt": "2024-08-15T10:00:00Z"
}
```

**Invite Code Format**: 12-character uppercase alphanumeric (e.g., `XJ4K9P2M7N5Q`)

**Errors:**
- `403 FORBIDDEN` - User is not the league owner
- `409 CONFLICT` - League is not INACTIVE

---

#### Revoke Invite

Revokes an invite code, preventing further use.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/invites/{inviteId}/revoke
Authorization: Bearer <accessToken>
```

**Response (204 No Content)**

**Errors:**
- `403 FORBIDDEN` - User is not the league owner
- `404 NOT_FOUND` - Invite does not exist
- `409 CONFLICT` - Invite is already revoked

---

### Membership Management

#### Join League

Join a league using an invite code. The league must be INACTIVE, have available capacity, and the user must not already be a member.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/join
Content-Type: application/json
Authorization: Bearer <accessToken>

{
  "inviteCode": "XJ4K9P2M7N5Q"
}
```

**Response (201 Created):**
```json
{
  "id": 15,
  "userId": 67890,
  "role": "MEMBER",
  "joinedAt": "2024-08-16T14:30:00Z"
}
```

**Errors:**
- `400 BAD_REQUEST (INVALID_INVITE)` - Invite code is invalid, expired, revoked, or max uses reached
- `403 FORBIDDEN` - User is already a member
- `409 CONFLICT` - League is at maximum capacity or not INACTIVE

**Implementation Note**: Uses pessimistic locking (`findByIdForUpdate`) and atomic increment (`incrementUsesCount`) for thread-safe concurrent joins.

---

#### List Members

Returns paginated list of league members. Requires league membership.

**Request:**
```http
GET /api/v1/leagues/{leagueId}/members?page=0&size=20
Authorization: Bearer <accessToken>
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "userId": 12345,
      "role": "OWNER",
      "joinedAt": "2024-08-01T10:30:00Z"
    },
    {
      "id": 15,
      "userId": 67890,
      "role": "MEMBER",
      "joinedAt": "2024-08-16T14:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 8,
  "totalPages": 1
}
```

**Errors:**
- `403 FORBIDDEN` - User is not a member of the league

---

#### Remove Member

Remove a member from the league. Only the owner can remove members, and only before the league starts.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/members/{targetUserId}/remove
Authorization: Bearer <accessToken>
```

**Response (204 No Content)**

**Errors:**
- `403 FORBIDDEN` - User is not the league owner, or attempting to remove self
- `404 NOT_FOUND` - Target user is not a member
- `409 CONFLICT` - League has already started

---

#### Leave League

User leaves a league. Only allowed in INACTIVE leagues, and owner cannot leave.

**Request:**
```http
POST /api/v1/leagues/{leagueId}/leave
Authorization: Bearer <accessToken>
```

**Response (204 No Content)**

**Errors:**
- `403 FORBIDDEN` - User is the owner (owners cannot leave)
- `404 NOT_FOUND` - User is not a member
- `409 CONFLICT` - League is not INACTIVE

---

## Data Model

### League Entity

**Table:** `league.leagues`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGSERIAL | PK | League ID |
| owner_user_id | BIGINT | NOT NULL | User ID of league creator |
| name | VARCHAR(255) | NOT NULL | League name |
| status | VARCHAR(16) | NOT NULL, CHECK | INACTIVE, ACTIVE, or ARCHIVED |
| max_members | INT | NOT NULL, >1 | Maximum league capacity |
| season_start_at | TIMESTAMPTZ | NOT NULL | Season start date |
| season_end_at | TIMESTAMPTZ | NOT NULL, >season_start_at | Season end date |
| initial_stipend_amount | NUMERIC(19,4) | NOT NULL, >0 | Starting capital for members |
| weekly_stipend_amount | NUMERIC(19,4) | NOT NULL, >0 | Weekly stipend amount |
| weekly_payout_dow_utc | SMALLINT | NOT NULL, 0-6 | Day of week for payouts (0=Sunday) |
| started_at | TIMESTAMPTZ | | When league transitioned to ACTIVE |
| initial_stipend_issued_at | TIMESTAMPTZ | | When initial stipend was issued |
| created_at | TIMESTAMPTZ | NOT NULL | Record creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL | Record last update timestamp |

**Indexes:**
- `idx_leagues_owner_user_id` on (owner_user_id)
- `idx_leagues_status` on (status)
- `idx_leagues_created_at` on (created_at)
- `idx_leagues_season_start_at` on (season_start_at)
- `idx_leagues_season_end_at` on (season_end_at)
- `idx_leagues_started_at` on (started_at)
- `idx_leagues_owner_status` on (owner_user_id, status)
- `idx_leagues_status_created` on (status, created_at DESC)

---

### LeagueMember Entity

**Table:** `league.league_members`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGSERIAL | PK | Member ID |
| league_id | BIGINT | NOT NULL, FK leagues | Reference to league |
| user_id | BIGINT | NOT NULL | User ID of member |
| role | VARCHAR(16) | NOT NULL, CHECK | OWNER or MEMBER |
| joined_at | TIMESTAMPTZ | NOT NULL | When user joined league |
| created_at | TIMESTAMPTZ | NOT NULL | Record creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL | Record last update timestamp |

**Constraints:**
- UNIQUE (league_id, user_id) - user can only join league once
- FK league_id → leagues(id) ON DELETE CASCADE

**Indexes:**
- `idx_league_members_league_id` on (league_id)
- `idx_league_members_user_id` on (user_id)
- `idx_league_members_role` on (role)

---

### LeagueInvite Entity

**Table:** `league.league_invites`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGSERIAL | PK | Invite ID |
| league_id | BIGINT | NOT NULL, FK leagues | Reference to league |
| code | VARCHAR(64) | NOT NULL, UNIQUE | Unique invite code |
| created_by | BIGINT | NOT NULL | User ID who created invite |
| expires_at | TIMESTAMPTZ | | Optional expiration date |
| max_uses | INT | >0 if set | Optional maximum use count |
| uses_count | INT | NOT NULL, ≤max_uses | Current use count |
| revoked_at | TIMESTAMPTZ | | Revocation timestamp |
| created_at | TIMESTAMPTZ | NOT NULL | Record creation timestamp |
| updated_at | TIMESTAMPTZ | NOT NULL | Record last update timestamp |

**Constraints:**
- UNIQUE (code)
- FK league_id → leagues(id) ON DELETE CASCADE
- CHECK: uses_count <= max_uses when max_uses is set

**Indexes:**
- `idx_league_invites_code` on (code)
- `idx_league_invites_league_id` on (league_id)
- `idx_league_invites_created_by` on (created_by)
- `idx_league_invites_expires_at` on (expires_at)
- `idx_league_invites_revoked_at` on (revoked_at)

---

## Database Migrations (Flyway)

### V1__create_leagues.sql

Creates `leagues` table with:
- Primary key on id
- 8 indexes for query optimization
- CHECK constraints on status, max_members, stipend amounts, day of week, season dates
- Timestamps with default CURRENT_TIMESTAMP and update trigger

### V2__create_league_members.sql

Creates `league_members` table with:
- Foreign key to leagues with CASCADE delete
- Unique constraint on (league_id, user_id)
- CHECK constraint on role (OWNER, MEMBER)
- 3 indexes on league_id, user_id, role

### V3__create_league_invites.sql

Creates `league_invites` table with:
- Foreign key to leagues with CASCADE delete
- Unique constraint on code
- CHECK constraint on uses_count vs max_uses
- 5 indexes for efficient invite lookups

---

## Business Rules & Validations

### League Creation
- Name is required (max 255 characters)
- Maximum members must be at least 2
- Season end must be after season start
- Both season dates must be in the future
- Stipend amounts must be positive
- Weekly payout day must be 0-6 (Sunday-Saturday)
- Creator automatically becomes first member with OWNER role
- Initial status is INACTIVE

### Starting a League
- Only owner can start
- Must have at least 2 members
- League must be INACTIVE
- Sets startedAt timestamp
- Transitions status to ACTIVE
- **TODO**: Trigger initial stipend issuance to wallet service

### Invite System
- Only owner can create invites
- League must be INACTIVE
- Codes are 12-character uppercase alphanumeric
- Generated using UUID with up to 3 collision retries
- Optional expiration date (must be in future)
- Optional max uses (must be positive)
- Uses count increments atomically on join
- Can be revoked to prevent further use

### Joining a League
- League must be INACTIVE
- Must have valid invite code
- Invite must not be revoked
- Invite must not be expired
- Invite must have uses remaining (if max_uses set)
- League must not be at capacity
- User cannot already be a member
- Uses pessimistic locking to prevent race conditions
- Atomically increments invite uses count

### Membership Management
- Members can only leave INACTIVE leagues
- Owner cannot leave league
- Owner can remove members only before league starts
- Owner cannot remove themselves
- Membership grants access to league endpoints

### League Archival
- Only owner can archive
- Cannot archive already-archived league
- Sets status to ARCHIVED

---

## Error Responses

All errors follow a consistent shape:

```json
{
  "timestamp": "2024-08-16T15:30:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed for field 'maxMembers': must be greater than or equal to 2"
}
```

### HTTP Status Codes

| Status | Code | When |
|--------|------|------|
| 400 | `VALIDATION_ERROR` | Request validation failed (field errors listed) |
| 400 | `BAD_REQUEST` | Invalid request parameters or missing required header |
| 400 | `INVALID_INVITE` | Invite code is invalid, expired, revoked, or max uses reached |
| 403 | `FORBIDDEN` | User lacks permission (not owner, not member, etc.) |
| 404 | `NOT_FOUND` | League, member, or invite not found |
| 409 | `CONFLICT` | Operation conflicts with current state (already started, at capacity, etc.) |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

### Common Error Scenarios

**Validation Errors (400):**
```json
{
  "timestamp": "2024-08-16T15:30:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "maxMembers: must be greater than or equal to 2, seasonEndAt: must be after seasonStartAt"
}
```

**Access Denied (403):**
```json
{
  "timestamp": "2024-08-16T15:30:00Z",
  "status": 403,
  "code": "FORBIDDEN",
  "message": "Only the league owner can perform this action"
}
```

**Invalid Invite (400):**
```json
{
  "timestamp": "2024-08-16T15:30:00Z",
  "status": 400,
  "code": "INVALID_INVITE",
  "message": "Invite code has reached maximum uses"
}
```

**State Conflict (409):**
```json
{
  "timestamp": "2024-08-16T15:30:00Z",
  "status": 409,
  "code": "CONFLICT",
  "message": "League must be INACTIVE to start"
}
```

---

## Service Architecture

### Layered Structure

```
Controller Layer (LeagueController)
    ↓
Service Layer (LeagueService)
    ↓
Repository Layer (LeagueRepository, LeagueMemberRepository, LeagueInviteRepository)
    ↓
Database (PostgreSQL)
```

### Key Design Patterns

**DTO Pattern:**
- Request DTOs with Jakarta validation annotations
- Response DTOs as immutable records with factory methods
- Clear separation between API contracts and entities

**Repository Pattern:**
- Spring Data JPA repositories with custom queries
- Pessimistic locking for concurrent-safe operations
- Atomic updates with `@Modifying` queries

**Transaction Management:**
- Explicit `@Transactional` on all service methods
- Read-only optimization where applicable
- Proper transaction boundary definition

**Exception Handling:**
- Custom exception hierarchy extending `LeagueException`
- Global exception handler with standardized responses
- Specific HTTP status codes for each error type

---

## Configuration

### Application Properties

```properties
# Application
spring.application.name=league
server.port=${LEAGUE_SERVICE_PORT:8100}

# Database
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:6432}/${DB_NAME:sportstock}?currentSchema=league
spring.datasource.username=${DB_USER:sportstock}
spring.datasource.password=${DB_PASSWORD:localdev}

# Flyway
spring.flyway.locations=classpath:db/migration
spring.flyway.schemas=league

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.default_schema=league
spring.jpa.open-in-view=false

# External Config
spring.config.import=optional:file:.env[.properties],optional:file:../../.env[.properties]
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| LEAGUE_SERVICE_PORT | 8100 | Service port |
| DB_HOST | localhost | PostgreSQL host |
| DB_PORT | 6432 | PostgreSQL port |
| DB_NAME | sportstock | Database name |
| DB_USER | sportstock | Database user |
| DB_PASSWORD | localdev | Database password |

---

## Development Setup

### Prerequisites
- Java 21
- Maven 3.9.9+
- Docker & Docker Compose
- PostgreSQL 16+

### Database Setup

Reset and start database:
```bash
docker compose down -v && docker compose up --build -d
```

This creates the `league` schema and runs all Flyway migrations.

### Build and Run

Build the service:
```bash
./mvnw -pl services/league clean package
```

Run the service:
```bash
./mvnw -pl services/league spring-boot:run
```

Or run both from root:
```bash
./mvnw clean install
./mvnw -pl services/league spring-boot:run
```

Service will start on `http://localhost:8100`

### Testing with cURL

**Create a league:**
```bash
curl -X POST http://localhost:8100/api/v1/leagues \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "name": "Test League",
    "maxMembers": 10,
    "seasonStartAt": "2025-09-05T17:00:00Z",
    "seasonEndAt": "2026-02-10T23:59:59Z",
    "initialStipendAmount": 10000.00,
    "weeklyStipendAmount": 500.00,
    "weeklyPayoutDowUtc": 1
  }'
```

**Create an invite:**
```bash
curl -X POST http://localhost:8100/api/v1/leagues/1/invites \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "expiresAt": "2025-09-01T23:59:59Z",
    "maxUses": 10
  }'
```

**Join with invite:**
```bash
curl -X POST http://localhost:8100/api/v1/leagues/1/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "inviteCode": "XJ4K9P2M7N5Q"
  }'
```

**Start league:**
```bash
curl -X POST http://localhost:8100/api/v1/leagues/1/start \
  -H "Authorization: Bearer <accessToken>"
```

---

## Known Limitations & TODOs

### Current Limitations

1. **Authentication**: Currently uses simple header-based user ID extraction. Needs OAuth/Spring Security integration.

2. **Stipend Issuance**: Starting a league does not yet trigger initial stipend issuance. Wallet service integration pending.

3. **Event System**: No events/webhooks for league state changes. Consumers must poll.

4. **Audit Log**: No audit trail for administrative actions (member removals, invite revocations, etc.).

5. **Invite Analytics**: Invite usage tracking is basic (just use count). No tracking of who used each invite.

### Planned Enhancements

- [ ] OAuth 2.0 / OIDC integration for authentication
- [ ] Wallet service integration for stipend management
- [ ] Event publishing (Spring Events or message queue) for league lifecycle changes
- [ ] Audit logging for administrative actions
- [ ] Enhanced invite analytics (usage history, per-user tracking)
- [ ] League settings modification (name, stipend amounts, etc.)
- [ ] Bulk operations (bulk member removal, bulk invite generation)
- [ ] League search and discovery features
- [ ] Transfer league ownership functionality

---

## Related Services

- **Wallet Service** (planned): Manages user balances, stipend issuance, and transaction history
- **Ingestion Service**: Provides NFL data (teams, players, games, stats) for fantasy leagues
- **Portfolio Service** (planned): Manages user stock portfolios within leagues

---

## Contact & Support

For questions or issues, refer to the main project documentation or contact the development team.
