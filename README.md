# Real Estate Management System

This is a Java-based Real Estate Management System portfolio project with a Spring Boot backend, MySQL persistence, JWT authentication, property and image management, and a vanilla JavaScript frontend.

## API Documentation

See [docs/API.md](docs/API.md) for the complete backend endpoint reference, including authentication, roles, request/response examples, and error responses.

## Project Overview

Implemented features include authentication and role-based authorization, property management and search, local property-image uploads, favorites, customer inquiries and agent replies, property visits, and separate customer, agent, and admin dashboards in the vanilla JavaScript frontend.

## Tech Stack

- Java 17
- Spring Boot 3.3.4
- MySQL
- Maven

## Project Structure

```text
real-estate-management-system/
├── database/schema.sql
├── docs/API.md
├── src/main/java/com/realestate/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── exception/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── RealEstateManagementSystemApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── static/ (index.html, styles.css, app.js)
├── src/test/java/com/realestate/
├── pom.xml
└── README.md
```

## Prerequisites

Before starting, make sure you have installed:
- Java 17+
- Maven
- MySQL Server

## MySQL Configuration

1. Start your MySQL server.
2. Create a database user or use the default root user.
3. Set database credentials in the environment; the password is not stored in source code:

```bash
export DB_USERNAME=root
export DB_PASSWORD='your-local-mysql-password'
```

`DB_USERNAME` defaults to `root`. `DB_URL` optionally overrides the connection URL; otherwise the app connects to `real_estate_db` on localhost.

Default connection settings are resolved from environment variables in `application.properties`; the password value comes from `DB_PASSWORD`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/real_estate_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

4. Create the database schema manually:

```bash
mysql -u root -p < database/schema.sql
```

For an existing database, apply the additive agent-deactivation migration once after selecting/connecting to `real_estate_db`:

```bash
mysql -u "$DB_USERNAME" -p real_estate_db < database/migrations/V1__add_user_active_status.sql
```

The migration adds `users.active` with a default of `TRUE`; it does not remove or rewrite existing accounts. Existing databases must receive this migration before running the updated application.

If the database already existed before property approvals were added, also run:

```bash
mysql -u "$DB_USERNAME" -p real_estate_db < database/migrations/V3__add_property_approval_status.sql
```

## Dependencies Added

The project dependencies include:

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- `spring-boot-starter-test`
- `spring-security-test` (test scope)

## How to Run the Project

Before starting, set `DB_PASSWORD` as described above. The local `dev` profile now supplies a development JWT secret automatically. From the project root, start the application with:

```bash
mvn spring-boot:run
```

The application starts on port `8080` by default. To build a runnable JAR, run `mvn package`, then start it with `java -jar target/real-estate-management-system-0.0.1-SNAPSHOT.jar`.

After starting the backend, open `http://localhost:8080/` to use the HTML/CSS/JavaScript frontend. The UI is served by Spring Boot from `src/main/resources/static` and calls the API on the same origin. Approved, available property browsing is public. Customer/agent/admin dashboards and protected actions still require a signed-in account.

## Authentication Setup

JWT signing requires a Base64-encoded secret that decodes to at least 32 bytes. Generate one locally and set it in your shell; do not commit it:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

The token lifetime defaults to 3600 seconds and can be overridden with `JWT_EXPIRATION_SECONDS`.

### Development role accounts

The opt-in `dev` Spring profile provisions three explicitly scoped test accounts at startup. It creates missing accounts and updates only these accounts' password hashes, roles, and active status; normal users are not modified. Passwords are BCrypt-hashed through the existing password encoder, never appear in frontend code, and can be overridden with environment variables.

Before starting the app, apply the existing-database migrations, configure your real local `DB_USERNAME` and `DB_PASSWORD`. The project defaults to the `dev` profile for local development and uses the development credentials shown below; to override them, set these optional variables:

```bash
export DEV_ADMIN_PASSWORD='AdminDev@12345'
export DEV_AGENT_PASSWORD='AgentDev@12345'
export DEV_CUSTOMER_PASSWORD='CustomerDev@12345'
mvn spring-boot:run
```

Use the common login page at `http://localhost:8080/`:

| Role | Email | Password | Dashboard |
|---|---|---|---|
| ADMIN | `dev.admin@hearthhome.local` | `AdminDev@12345` | `#admin-dashboard` |
| AGENT | `dev.agent@hearthhome.local` | `AgentDev@12345` | `#agent-dashboard` |
| CUSTOMER | `dev.customer@hearthhome.local` | `CustomerDev@12345` | `#customer-dashboard` |

On every startup with the `dev` profile, these exact accounts are reconciled to the listed development passwords and roles. No other user's password is changed. Never enable the `dev` profile in a production deployment. Public registration remains enabled and always creates `CUSTOMER` accounts; the role is assigned by the backend, never supplied by the registration form.

### Maven tests on newer Java runtimes

The Maven Surefire test JVM enables Byte Buddy's experimental Java compatibility using the `net.bytebuddy.experimental` system property. This is limited to test execution and allows the bundled Mockito/Byte Buddy version to run on Java runtimes newer than its officially supported versions; it does not change application runtime behavior.

### Register a customer

`POST /api/auth/register`

```json
{
  "firstName": "Pat",
  "lastName": "Example",
  "email": "pat@example.com",
  "password": "choose-a-strong-password",
  "phoneNumber": "555-0100"
}
```

Public registration always creates a `CUSTOMER`; elevated roles are not selectable during signup. The `roles` table should contain `ADMIN`, `AGENT`, and `CUSTOMER` rows, as inserted by `database/schema.sql`.

### Log in

`POST /api/auth/login`

```json
{
  "email": "pat@example.com",
  "password": "choose-a-strong-password"
}
```

The response includes a JWT bearer token. Send it on protected requests using `Authorization: Bearer <token>`.

## Property Management API

Property records have a moderation state separate from listing availability. Existing properties are migrated to `APPROVED` to preserve current customer visibility. Agent-created properties start `PENDING`; customers can browse and retrieve only `APPROVED` properties. Admins can approve or reject any property with `PATCH /api/properties/{id}/approval` and `{"approvalStatus":"APPROVED"}` or `{"approvalStatus":"REJECTED"}`. Apply `database/migrations/V3__add_property_approval_status.sql` once to an existing database.

All property routes require a valid bearer token. `ADMIN`, `AGENT`, and `CUSTOMER` may list properties and fetch one by ID. Only `ADMIN` and `AGENT` may create, update, or delete:

- `POST /api/properties` — create. An agent creates under their own account. An admin must provide `agentId` for an agent who owns the property.
- `GET /api/properties` — customers and admins can list all properties; agents see only their own listings.
- `GET /api/properties?city=Boston&propertyType=HOUSE&minPrice=100000&maxPrice=500000&minBedrooms=2&status=AVAILABLE` — optionally filter by city (case-insensitive partial match), exact property type, inclusive price bounds, minimum bedrooms, and exact status. Filters may be combined; results are ordered newest first. `minPrice` must not exceed `maxPrice`.
- `GET /api/properties/{id}` — fetch one property.
- `PUT /api/properties/{id}` — replace the property details. Agents can update only properties they own; admins can update any property and optionally reassign it with `agentId`.
- `DELETE /api/properties/{id}` — delete. Agents can delete only properties they own; admins can delete any property. A property referenced by other records cannot be deleted and returns a conflict response.

The property request body includes `title`, `propertyType`, `price`, `city`, `address`, and `status`. Optional fields are `description`, `bedrooms`, `bathrooms`, and `areaSqft`.

## Property Images

Agents can upload and remove images for their own property listings. Admins can manage images for any listing. Authenticated users with one of the existing roles can view a listing's image metadata; the image files are served from their generated URLs.

- `POST /api/properties/{propertyId}/images` — multipart form upload with a `file` field. Supports JPEG and PNG, up to 5 MB per image and 10 images per property.
- `GET /api/properties/{propertyId}/images` — list a property's image URLs; the primary image is listed first.
- `DELETE /api/properties/{propertyId}/images/{imageId}` — remove an image (agents must own the property).
- `GET /uploads/property-images/{generatedFileName}` — retrieve an uploaded image.

Files are stored locally under `uploads/property-images` by default. Set `PROPERTY_IMAGE_DIR` to change the storage directory; ensure the application user can read and write there. Image files are not stored in MySQL. The existing `property_images` table stores their URLs and primary-image metadata. The property editor supports uploading multiple images, and the property detail view displays and manages them.

## Favorites API

Only authenticated customers can manage favorites. Each endpoint uses the customer identity from the bearer token, so a customer cannot specify or access another customer's favorites:

- `POST /api/favorites/{propertyId}` — add a property to the signed-in customer's favorites. Returns `409 Conflict` if it is already favorited.
- `GET /api/favorites` — list the signed-in customer's favorite properties, newest first.
- `DELETE /api/favorites/{propertyId}` — remove the property from the signed-in customer's favorites. Returns `404 Not Found` if that property is not in their favorites.

The existing `favorites` table in `database/schema.sql` already includes foreign keys and a unique `(customer_id, property_id)` constraint to prevent duplicate entries at the database level.

## Customer Inquiries API

- `POST /api/inquiries` — customer submits an inquiry for a property with `propertyId` and a non-blank `message`.
- `GET /api/inquiries` — customers see only their own inquiries; agents see inquiries for their own properties; admins see all inquiries.

Inquiry submission is customer-only. The customer identity is taken from the bearer token, not from request data. The inquiry's property is required; on a database initialized from the schema, `inquiries.property_id` is non-null.
Agents can reply only to inquiries for their own properties using `PUT /api/inquiries/{inquiryId}/reply`; admins can reply to any inquiry. Replies are included in inquiry responses and shown to the customer.

For databases initialized before the reply fields were added, run this migration once:

```sql
ALTER TABLE inquiries
    ADD COLUMN response_message TEXT NULL,
    ADD COLUMN responded_at DATETIME NULL;
```

Admins can use `/api/admin/statistics`, `/api/admin/users`, and `/api/admin/agents` to view system totals and account directories. `POST /api/admin/agents` creates a hashed-password AGENT account from Manage Agents; it is admin-only and rejects existing email addresses. `PUT /api/admin/agents/{agentId}` edits an agent's name, email, and phone. `PATCH /api/admin/agents/{agentId}/deactivate` and `/activate` disable or restore account access. `DELETE /api/admin/agents/{agentId}` deletes only agents with no linked property, visit, inquiry, or favorite records; linked accounts must be deactivated instead to preserve records. Deactivated accounts cannot log in; previously issued tokens are rejected on their next request. `PUT /api/admin/users/{userId}/role` lets an admin change a user's existing role, with guardrails preventing self-demotion, demotion of the last administrator, and demotion of an agent who still owns properties.

If `real_estate_db` was initialized before this change, update its existing table once. First check that there are no existing inquiries without a property:

```sql
SELECT id FROM inquiries WHERE property_id IS NULL;
```

If the query returns no rows, apply:

```sql
ALTER TABLE inquiries MODIFY property_id BIGINT NOT NULL;
```

Resolve any rows returned by the check before applying the constraint.

## Property Visit Scheduling API

- `POST /api/visits` — a customer schedules a visit with `propertyId` and a future `scheduledAt` value in ISO local date-time format (for example, `2030-05-01T14:30:00`).
- `GET /api/visits` — customers see their own visits, agents see visits for properties they manage, and admins see all visits.
- `DELETE /api/visits/{visitId}` — the customer who scheduled a pending visit can cancel it. Cancellation changes its status to `CANCELLED`; it does not delete the record. Only the owning customer can cancel it.

The existing `property_visits` table already stores the property, customer, managing agent, scheduled date/time, status, and creation date with foreign keys. No schema change is needed for a database already created from `database/schema.sql`.

## How to Test the Database Connection

Before running the database test, ensure MySQL is started and `DB_PASSWORD` is set in the current shell. `DB_USERNAME` defaults to `root`; `DB_URL` can override the default URL.

Then run:

```bash
mvn test
```

This will execute `DatabaseConnectionTest`, which tries to connect to MySQL and verifies the connection.

## Notes

This repository includes the backend feature APIs and the vanilla JavaScript frontend described above.
