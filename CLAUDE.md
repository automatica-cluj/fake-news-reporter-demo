# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fake News Reporter is a Spring Boot 3.2.0 application for reporting and managing fake news sources. The application has two main user flows:
- **Public users**: View verified reports and submit new fake news reports
- **Admin users**: Review, approve/reject, and manage submitted reports

## Development Commands

### Local Development (H2 Database)
```bash
# Run application
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Build JAR
mvn clean package

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

### Docker Development (PostgreSQL)
```bash
# Build and start
docker-compose up --build

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View logs
docker-compose logs -f app
```

### Database Access
- **H2 Console** (local): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:fakenews`
  - Username: `sa`
  - Password: (empty)

- **Default Admin Login**:
  - Username: `admin`
  - Password: `admin123`

## Architecture

### Multi-Profile Configuration Strategy

The application uses Spring profiles to support different environments:

- **`local`** (default): H2 in-memory database for rapid development
- **`prod`**: PostgreSQL for production deployment
- **`test`**: H2 with test-specific settings for CI/CD

Configuration is split across:
- `application.yml` - Common settings
- `application-local.yml` - H2 configuration
- `application-prod.yml` - PostgreSQL with environment variable interpolation
- `application-test.yml` - Test profile settings

**Important**: When modifying database-related code, test against both H2 (local) and PostgreSQL (docker-compose) to ensure compatibility.

### Security Architecture

The application uses Spring Security with form-based authentication:

- **Public endpoints**: `/`, `/report`, `/reports`, static resources
- **Admin endpoints**: `/admin/**` requires `ROLE_ADMIN`
- **Authentication**: Custom `UserDetailsService` with BCrypt password encoding
- **CSRF**: Enabled for all forms (disabled only for H2 console)

Security configuration is centralized in `SecurityConfig.java`. When adding new endpoints:
- Add to `.requestMatchers()` in the security filter chain
- Ensure CSRF token is included in forms (Thymeleaf does this automatically with `th:action`)

### Data Flow Pattern

**Report Submission Flow**:
1. User submits form → `HomeController.submitReport()` (POST `/report`)
2. Form validation via `@Valid ReportForm`
3. Service layer creates `FakeNewsReport` with `approved=false`
4. Entity saved via Spring Data JPA repository
5. Redirect to confirmation page

**Admin Approval Flow**:
1. Admin views pending reports → `AdminController.dashboard()` (GET `/admin/dashboard`)
2. Admin approves → `AdminController.approveReport()` (POST `/admin/approve/{id}`)
3. Service layer updates report: sets `approved=true`, `approvedAt`, `approvedBy`
4. Repository saves changes
5. Redirect back to dashboard

**Key insight**: Reports are soft-approved (boolean flag) rather than moved between tables. This simplifies querying and maintains audit trail.

### Entity Relationships

```
User (users table)
├── id (BIGINT, PK)
├── username (VARCHAR, unique)
├── password (VARCHAR, BCrypt)
├── role (VARCHAR, e.g., "ROLE_ADMIN")
└── enabled (BOOLEAN)

FakeNewsReport (fake_news_reports table)
├── id (BIGINT, PK)
├── newsSource (VARCHAR) - Name of the fake news source
├── url (VARCHAR) - URL of the source
├── category (VARCHAR) - Enum: Politics, Health, Science, Technology, Entertainment, Finance
├── description (TEXT) - Details about why it's fake
├── reportedAt (TIMESTAMP) - Submission timestamp
├── approved (BOOLEAN) - Approval status
├── approvedAt (TIMESTAMP, nullable)
└── approvedBy (VARCHAR, nullable) - Username of approving admin
```

**No foreign key relationship** between User and FakeNewsReport - reports are anonymous submissions. Only the approving admin is tracked by username string.

### Data Initialization

`DataInitializer.java` runs on application startup using `@PostConstruct`:
- Creates default admin user if not exists
- Inserts sample fake news reports for development/demo purposes
- Uses `CommandLineRunner` pattern with Spring Boot

**When adding new initialization logic**: Implement idempotency checks (e.g., `if (!userRepository.existsByUsername(...)`) to prevent duplicate data on application restarts.

### Thymeleaf Template Structure

Templates follow a simple layout pattern without a formal layout framework:

```
templates/
├── index.html           # Homepage with verified reports list
├── reports.html         # Full list of verified reports
├── report-form.html     # Public report submission form
├── login.html           # Admin login page
└── admin/
    └── dashboard.html   # Admin dashboard with pending reports
```

**Thymeleaf patterns used**:
- `th:each` for iteration over report lists
- `th:text` for safe HTML escaping
- `th:href` and `th:action` for URL generation
- Thymeleaf Security extras for role-based display (`sec:authorize`)
- Form binding with `th:object` and `th:field`

**When adding new pages**: Follow the existing pattern of simple, single-purpose templates rather than creating complex component hierarchies.

## CI/CD with Semantic Versioning

This project uses GitHub Actions with automatic semantic versioning based on commit messages.

### Commit Message Format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<optional body>

<optional footer>
```

**Version Bumping**:
- `feat:` → Minor version bump (1.0.0 → 1.1.0)
- `fix:`, `perf:`, `refactor:` → Patch version bump (1.0.0 → 1.0.1)
- `BREAKING CHANGE:` or `feat!:` → Major version bump (1.0.0 → 2.0.0)
- `docs:`, `chore:`, `test:`, `ci:` → No release

**Examples**:
```bash
git commit -m "feat(reports): add pagination to reports list"
git commit -m "fix(auth): resolve session timeout issue"
git commit -m "feat!: redesign report approval API

BREAKING CHANGE: Admin endpoints now require API key authentication"
```

### Workflows

- **CI** (`ci.yml`): Runs on all pushes/PRs - tests, code quality checks, build verification
- **Build & Push** (`build-push.yml`): Runs on main branch - semantic versioning, Docker build, GHCR push, GitHub release creation

**Docker Images**: `ghcr.io/automatica-cluj/demo-project` with tags: `latest`, `v{version}`, `sha-{commit}`, `main`, `{timestamp}`

## Common Modifications

### Adding a New Report Category

1. Update `FakeNewsReport.java` - add to category validation or enum
2. Update `report-form.html` - add option to category dropdown
3. Update `DataInitializer.java` - optionally add sample data for new category
4. No database migration needed (using `ddl-auto: update`)

### Adding New Admin Endpoints

1. Add method to `AdminController.java`
2. Update `SecurityConfig.java` - ensure `/admin/**` pattern covers it
3. Create Thymeleaf template in `templates/admin/`
4. Ensure CSRF token is included in forms

### Changing Database Schema

**Local/Development**: Schema updates automatically with `ddl-auto: update`

**Production**:
- Consider using Flyway/Liquibase for production migrations (not currently configured)
- Or carefully test `ddl-auto: update` in staging environment first
- **Current setup uses `ddl-auto: update` in production** - be cautious with destructive changes

### Adding Dependencies

Edit `pom.xml` and add dependency in appropriate section:
```xml
<dependency>
    <groupId>group.id</groupId>
    <artifactId>artifact-id</artifactId>
    <version>1.0.0</version>
</dependency>
```

Run `mvn dependency:resolve` to verify, then test locally with `mvn spring-boot:run`.

## Testing Notes

Currently, the project has minimal test coverage (no test files found in `src/test`).

**To add tests**:
```bash
# Create test file structure
mkdir -p src/test/java/com/automatica/fakenews

# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

Use `@SpringBootTest` for integration tests and `@WebMvcTest` for controller tests. Ensure tests use the `test` profile (`@ActiveProfiles("test")`).

## Important Constraints

- **Java 17 required**: Project uses Spring Boot 3.2.0 which requires Java 17+
- **Maven 3.6+**: For building the project
- **Multi-database support**: Code must work with both H2 and PostgreSQL - avoid database-specific SQL
- **Anonymous reporting**: Reports do not link to user accounts - maintain this design
- **Admin credentials**: Default credentials are for development only - production deployments must change these
