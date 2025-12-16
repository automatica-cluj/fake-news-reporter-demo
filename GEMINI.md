# Project Overview

This is a Spring Boot application for reporting and managing fake news sources. Users can submit reports of fake news, which are then reviewed and approved by administrators.

**Key Technologies:**

*   **Backend:** Spring Boot
*   **Frontend:** Thymeleaf
*   **Database:** H2 (local), PostgreSQL (production)
*   **Security:** Spring Security
*   **Build:** Maven

**Architecture:**

The application follows a standard Model-View-Controller (MVC) architecture.

*   `src/main/java/com/automatica/fakenews/controller`: Contains the web controllers for handling user requests.
*   `src/main/java/com/automatica/fakenews/model`: Defines the JPA entities for the application.
*   `src/main/java/com/automatica/fakenews/repository`: Contains the Spring Data repositories for database access.
*   `src/main/java/com/automatica/fakenews/service`: Implements the business logic of the application.
*   `src/main/resources/templates`: Contains the Thymeleaf templates for the user interface.

# Building and Running

**Prerequisites:**

*   Java 17+
*   Maven 3.6+

**Run Locally (H2 database):**

```bash
mvn spring-boot:run
```

The application will be available at http://localhost:8080.

**Run with Docker (PostgreSQL database):**

```bash
docker-compose up --build
```

**Build for Production:**

```bash
mvn clean package
```

# Development Conventions

*   **Testing:** Unit and integration tests are located in `src/test/java`.
*   **Code Style:** The project follows standard Java conventions.
*   **Configuration:** Application configuration is managed through `application.yml` and profile-specific files (`application-local.yml`, `application-prod.yml`).
