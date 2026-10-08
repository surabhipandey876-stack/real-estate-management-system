# Real Estate Management System API

This document describes the HTTP endpoints currently implemented by the backend. Unless otherwise noted, request and response bodies use JSON (`Content-Type: application/json`). The API is served from the same origin as the frontend; for local development the base URL is `http://localhost:8080`.

## Authentication and conventions

Protected API requests require a JWT bearer token:

```http
Authorization: Bearer <accessToken>
```

The token is returned by `POST /api/auth/login`. Registration and login are public. Public customer registration assigns the `CUSTOMER` role; clients cannot choose `ADMIN` or `AGENT`.

Roles are `ADMIN`, `AGENT`, and `CUSTOMER`. The JWT filter returns `401 Unauthorized` for a supplied invalid or expired bearer token. Requests without credentials and requests without the required role or resource ownership are rejected by Spring Security and application authorization (normally `403 Forbidden` with this configuration). Validation and handled application errors use Spring's Problem Details format, for example:

For local testing only, the opt-in `dev` Spring profile creates missing role accounts using passwords supplied through `DEV_ADMIN_PASSWORD`, `DEV_AGENT_PASSWORD`, and `DEV_CUSTOMER_PASSWORD`. See the README for the development credentials and startup steps. Existing records at those email addresses are preserved, never overwritten.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request fields are invalid."
}
```

The `detail` value varies by error and endpoint. Responses with no body are indicated below.

## Authentication

### Register a customer

`POST /api/auth/register`

- **Purpose:** Create a customer account.
- **Authentication/role:** Public; creates only `CUSTOMER`.
- **Request body:** `firstName` and `lastName` (non-blank, max 100 characters), `email` (valid email, max 255), `password` (8–72 characters), optional `phoneNumber` (max 30).
- **Example request:**

  ```json
  {
    "firstName": "Pat",
    "lastName": "Example",
    "email": "pat@example.com",
    "password": "choose-a-strong-password",
    "phoneNumber": "555-0100"
  }
  ```

- **Success:** `201 Created`; response is a `UserResponse`, not a token:

  ```json
  {
    "id": 42,
    "firstName": "Pat",
    "lastName": "Example",
    "email": "pat@example.com",
    "phoneNumber": "555-0100",
    "role": "CUSTOMER"
  }
  ```

- **Important errors:** `400` invalid fields; `409 Conflict` if the email is already registered; `500 Internal Server Error` if the required `CUSTOMER` role is missing from the database.

### Log in

`POST /api/auth/login`

- **Purpose:** Verify credentials and issue a JWT.
- **Authentication/role:** Public.
- **Request body:** `email` (valid email) and non-blank `password`.
- **Example request:**

  ```json
  {
    "email": "pat@example.com",
    "password": "choose-a-strong-password"
  }
  ```

- **Success:** `200 OK`:

  ```json
  {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresAt": "2030-05-01T15:30:00Z",
    "user": {
      "id": 42,
      "firstName": "Pat",
      "lastName": "Example",
      "email": "pat@example.com",
      "phoneNumber": "555-0100",
      "role": "CUSTOMER",
      "active": true
    }
  }
  ```

- **Important errors:** `400` invalid fields; `401 Unauthorized` for an unknown email, incorrect password, or deactivated account.

## Customer profile

Customer identity is always taken from the authenticated token; these endpoints accept no user ID or email selector. Only `CUSTOMER` can access them.

- `GET /api/customers/me` — return the signed-in customer's profile.
- `PUT /api/customers/me` — update `firstName`, `lastName`, and optional `phoneNumber`. The account email and role are intentionally not editable here.
- **Success:** `200 OK`; a `UserResponse`.
- **Errors:** `400` invalid profile fields; `401` invalid/expired bearer token; `403` wrong role.

## Properties

Property collection and detail routes (`GET /api/properties` and `GET /api/properties/{id}`) are public. Anonymous visitors and customers see only properties whose approval state is `APPROVED` and listing status is `AVAILABLE`. Agents receive only their own properties; admins can view and manage all properties. Create, update, delete, approval, and image metadata routes remain protected.

Moderation is separate from listing availability (`status`). Existing records receive `approvalStatus: APPROVED` from the migration default. New properties created by an agent start with `PENDING`; Admin-created properties start `APPROVED`. Public visitors and customers can list and view only approved and available properties, while agents see only their assigned properties and admins see all properties.

- `PATCH /api/properties/{id}/approval` — Admin-only. Set `{"approvalStatus":"APPROVED"}` or `{"approvalStatus":"REJECTED"}`. The existing listing `status` is not changed.
- Apply `database/migrations/V3__add_property_approval_status.sql` once to existing databases before starting the updated application.

Property request fields are `title` (required, max 255), `description` (optional), `propertyType` (required, max 50), `price` (required, at least `0.01`, at most 13 integer and 2 fractional digits), `bedrooms` and `bathrooms` (optional, non-negative integers), `areaSqft` (optional, positive integer), `city` (required, max 100), `address` (required, max 255), `status` (required, max 30), and `agentId` (optional positive integer). The API requires non-blank `propertyType` and `status` strings but does not enforce a fixed enum; `HOUSE` and `AVAILABLE` are example values used by the frontend.

Example request body:

```json
{
  "title": "Sunny family home",
  "description": "Bright rooms and a landscaped garden.",
  "propertyType": "HOUSE",
  "price": 425000.00,
  "bedrooms": 3,
  "bathrooms": 2,
  "areaSqft": 1800,
  "city": "Boston",
  "address": "10 Main Street",
  "status": "AVAILABLE"
}
```

Example property response:

```json
{
  "id": 15,
  "title": "Sunny family home",
  "description": "Bright rooms and a landscaped garden.",
  "propertyType": "HOUSE",
  "price": 425000.00,
  "bedrooms": 3,
  "bathrooms": 2,
  "areaSqft": 1800,
  "city": "Boston",
  "address": "10 Main Street",
  "status": "AVAILABLE",
  "approvalStatus": "APPROVED",
  "agentId": 7,
  "createdAt": "2030-04-01T09:00:00",
  "updatedAt": "2030-04-02T11:30:00"
}
```

### Create property

`POST /api/properties`

- **Purpose:** Create a property listing.
- **Authentication/role:** `ADMIN` or `AGENT`.
- **Request body:** Property request above. An agent must omit `agentId`; the signed-in agent becomes the owner. An admin must provide an `agentId` belonging to an agent.
- **Example request:** Send the property request body above with `Authorization: Bearer <accessToken>`. For an admin, add `"agentId": 7`.
- **Success:** `201 Created`; a property response.
- **Important errors:** `400` invalid fields, missing/invalid admin `agentId`, or non-agent owner; `401` invalid/expired bearer token; `403` missing authentication, wrong role, or agent attempted to supply `agentId`.

### Search/list properties

`GET /api/properties`

- **Purpose:** List properties, optionally applying combined search filters.
- **Authentication/role:** Public; authentication is not required.
- **Visibility:** Anonymous visitors and customers receive only approved, available listings; agents receive only their own properties; admins can search all properties.
- **Query parameters:** All optional:
  - `city` — case-insensitive partial match against the listing's city or address.
  - `propertyType` — case-insensitive exact match.
  - `minPrice`, `maxPrice` — inclusive numeric bounds.
  - `minBedrooms` — minimum bedroom count.
  - `status` — case-insensitive exact match.
- **Example request:** `GET /api/properties?city=Boston&propertyType=HOUSE&minPrice=200000&maxPrice=500000&minBedrooms=2&status=AVAILABLE`
- **Success:** `200 OK`; an array of property responses, newest first. With no matching properties, the response is `[]`.
- **Important errors:** `400` invalid parameter values or `minPrice` greater than `maxPrice`; `401` invalid/expired bearer token, if supplied.

### Get property by ID

`GET /api/properties/{id}`

- **Purpose:** Get one property's details.
- **Authentication/role:** Public; authentication is not required.
- **Request:** Path parameter `id` (property ID).
- **Example request:** `GET /api/properties/15`
- **Success:** `200 OK`; one property response.
- **Important errors:** `401` invalid/expired bearer token, if supplied; `404 Not Found` property does not exist or is not approved and available for public/customer browsing; an authenticated agent also receives `404` when the property is outside their ownership.

### Update property

`PUT /api/properties/{id}`

- **Purpose:** Replace listing details.
- **Authentication/role:** `ADMIN`, or the `AGENT` who owns the property.
- **Request:** Path parameter `id`; JSON property request body. An admin can optionally set `agentId` to reassign the listing. An agent must omit `agentId`.
- **Example request:** `PUT /api/properties/15` with the property request body above and a bearer token.
- **Success:** `200 OK`; updated property response.
- **Important errors:** `400` invalid fields or invalid agent assignment; `401` invalid/expired bearer token; `403` missing authentication, wrong role, non-owner agent, or agent supplied `agentId`; `404` property does not exist.

### Delete property

`DELETE /api/properties/{id}`

- **Purpose:** Delete a listing and its associated stored image files.
- **Authentication/role:** `ADMIN`, or the `AGENT` who owns the property.
- **Request:** Path parameter `id`.
- **Example request:** `DELETE /api/properties/15`
- **Success:** `204 No Content`.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication, wrong role, or non-owner agent; `404` property does not exist; `409 Conflict` the property is referenced by records that prevent deletion.

## Property images

Image management uses the existing property's ownership rules: only an admin or the managing agent may upload/delete images. Metadata reads use the same property visibility as detail reads: customers can list images for approved properties, agents for their own properties, and admins for all properties. The returned `imageUrl` can be fetched without a token.

Uploads accept valid JPEG or PNG image contents, at most 5 MB and 12 megapixels per image, and at most 10 images per property. Images are stored locally; the existing `property_images` table stores metadata. The first image added is primary; deleting it promotes the next listed image.

### List property images

`GET /api/properties/{propertyId}/images`

- **Purpose:** List image metadata for one property, with the primary image first.
- **Authentication/role:** Any authenticated role.
- **Request:** Path parameter `propertyId`.
- **Example request:** `GET /api/properties/15/images`
- **Success:** `200 OK`; array of image responses, possibly empty:

  ```json
  [
    {
      "id": 31,
      "propertyId": 15,
      "imageUrl": "/uploads/property-images/9b1f3dd5-7f44-4b51-84d1-34a593d7895f.jpg",
      "primary": true,
      "createdAt": "2030-04-01T09:15:00"
    }
  ]
  ```

- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not allowed; `404` property does not exist or is not visible to this customer/agent.

### Upload property image

`POST /api/properties/{propertyId}/images`

- **Purpose:** Add an image to a listing.
- **Authentication/role:** `ADMIN`, or the `AGENT` who owns the property.
- **Request:** `multipart/form-data` with one `file` part. Accepted formats are JPEG and PNG; the content is inspected, not trusted from the filename alone.
- **Example request:**

  ```bash
  curl -X POST "http://localhost:8080/api/properties/15/images" \
    -H "Authorization: Bearer <accessToken>" \
    -F "file=@front-of-home.jpg;type=image/jpeg"
  ```

- **Success:** `201 Created`; one image response as shown above.
- **Important errors:** `400` empty, invalid, unsupported, oversized, over-dimension, or excess image; `401` invalid/expired bearer token; `403` missing authentication, wrong role, or non-owner agent; `404` property does not exist; `413 Payload Too Large` if the configured multipart request limit is exceeded; `500` local image storage failure.

### Delete property image

`DELETE /api/properties/{propertyId}/images/{imageId}`

- **Purpose:** Delete an image from its property and remove its local file.
- **Authentication/role:** `ADMIN`, or the `AGENT` who owns the property.
- **Request:** Path parameters `propertyId` and `imageId`.
- **Example request:** `DELETE /api/properties/15/images/31`
- **Success:** `204 No Content`.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication, wrong role, or non-owner agent; `404` property or image does not exist for that property; `500` local image storage failure.

### Retrieve an uploaded image

`GET /uploads/property-images/{generatedFileName}`

- **Purpose:** Fetch image bytes using the `imageUrl` returned by the image API.
- **Authentication/role:** Public; no token required.
- **Example request:** `GET /uploads/property-images/9b1f3dd5-7f44-4b51-84d1-34a593d7895f.jpg`
- **Success:** `200 OK`; image bytes with the corresponding media type.
- **Important errors:** `404 Not Found` if the file is not available.

## Favorites

All favorites endpoints require `CUSTOMER`. Customer identity comes from the token; there is no customer ID in the request.

### Add favorite

`POST /api/favorites/{propertyId}`

- **Purpose:** Add a property to the signed-in customer's favorites.
- **Authentication/role:** `CUSTOMER`.
- **Request:** Path parameter `propertyId`.
- **Example request:** `POST /api/favorites/15`
- **Success:** `201 Created`; a property response.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`; `404` property does not exist or is not approved; `409 Conflict` property is already a favorite.

### List my favorites

`GET /api/favorites`

- **Purpose:** List the signed-in customer's saved properties.
- **Authentication/role:** `CUSTOMER`.
- **Request:** No parameters or body.
- **Example request:** `GET /api/favorites`
- **Success:** `200 OK`; an array of property responses, newest favorite first; empty list if none.
- Only approved properties are included. A saved property that is later rejected is hidden from the customer's list.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`.

### Remove favorite

`DELETE /api/favorites/{propertyId}`

- **Purpose:** Remove a property from the signed-in customer's favorites.
- **Authentication/role:** `CUSTOMER`.
- **Request:** Path parameter `propertyId`.
- **Example request:** `DELETE /api/favorites/15`
- **Success:** `204 No Content`.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`; `404 Not Found` property is not in this customer's favorites.

## Customer inquiries

### Submit inquiry

`POST /api/inquiries`

- **Purpose:** Submit an inquiry about a property.
- **Authentication/role:** `CUSTOMER`.
- **Request body:** `propertyId` (positive integer) and `message` (non-blank, max 5000 characters). Customer ID is derived from the token.
- **Example request:**

  ```json
  {
    "propertyId": 15,
    "message": "Is the property available for a viewing this weekend?"
  }
  ```

- **Success:** `201 Created`:

  ```json
  {
    "id": 72,
    "propertyId": 15,
    "propertyTitle": "Sunny family home",
    "customerId": 42,
    "customerEmail": "pat@example.com",
    "message": "Is the property available for a viewing this weekend?",
    "status": "OPEN",
    "createdAt": "2030-04-02T10:00:00"
  }
  ```

- **Important errors:** `400` invalid fields; `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`; `404` property does not exist or is not approved.

### List visible inquiries

`GET /api/inquiries`

- **Purpose:** List inquiries visible to the signed-in user.
- **Authentication/role:** Any of `ADMIN`, `AGENT`, or `CUSTOMER`.
- **Request:** No parameters or body.
- **Visibility:** Customers see their own inquiries; agents see inquiries for their properties; admins see all inquiries.
- **Example request:** `GET /api/inquiries`
- **Success:** `200 OK`; an array of inquiry responses, newest first; each includes `responseMessage` and `respondedAt` when an agent or admin has replied.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not allowed.

### Reply to an inquiry

`PUT /api/inquiries/{inquiryId}/reply`

- **Purpose:** Send a response to a customer's property inquiry.
- **Authentication/role:** `ADMIN`, or the `AGENT` who manages the inquiry's property.
- **Request body:** `message` (non-blank, max 5000 characters).
- **Success:** `200 OK`; updated inquiry response with `status: "ANSWERED"`, `responseMessage`, and `respondedAt`.
- **Important errors:** `400` invalid message; `403` wrong role; `404` inquiry does not exist or is not assigned to this agent.

## Property visits

### Schedule visit

`POST /api/visits`

- **Purpose:** Schedule a visit for a property.
- **Authentication/role:** `CUSTOMER`.
- **Request body:** `propertyId` (positive integer) and `scheduledAt` (future ISO local date-time).
- **Example request:**

  ```json
  {
    "propertyId": 15,
    "scheduledAt": "2030-05-01T14:30:00"
  }
  ```

- **Success:** `201 Created`:

  ```json
  {
    "id": 90,
    "propertyId": 15,
    "propertyTitle": "Sunny family home",
    "customerId": 42,
    "customerEmail": "pat@example.com",
    "agentId": 7,
    "scheduledAt": "2030-05-01T14:30:00",
    "status": "PENDING",
    "createdAt": "2030-04-02T10:15:00"
  }
  ```

- **Important errors:** `400` invalid fields or visit time not in the future; `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`; `404` property does not exist or is not approved.

### List visible visits

`GET /api/visits`

- **Purpose:** List visits visible to the signed-in user.
- **Authentication/role:** Any of `ADMIN`, `AGENT`, or `CUSTOMER`.
- **Request:** No parameters or body.
- **Visibility:** Customers see their own visits; agents see visits for properties currently assigned to them; admins see all visits. Lists are ordered by scheduled time.
- **Example request:** `GET /api/visits`
- **Success:** `200 OK`; an array of visit responses; empty list if none.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not allowed.

### Accept or reject a visit request

`PATCH /api/visits/{visitId}/status`

- **Purpose:** Accept or reject a pending visit request.
- **Authentication/role:** The agent currently assigned to the visit's property, or an `ADMIN`.
- **Request body:** `{"status":"ACCEPTED"}` or `{"status":"REJECTED"}`.
- **Success:** `200 OK`; the updated visit response.
- **Important errors:** `400` invalid status; `401` invalid/expired bearer token; `403` wrong role; `404` visit does not exist or is not for a property currently assigned to this agent; `409` visit is no longer pending.

### Cancel visit

`DELETE /api/visits/{visitId}`

- **Purpose:** Cancel a pending visit. The visit record remains and its status becomes `CANCELLED`.
- **Authentication/role:** The customer who scheduled it (`CUSTOMER`) only.
- **Request:** Path parameter `visitId`.
- **Example request:** `DELETE /api/visits/90`
- **Success:** `204 No Content`.
- **Important errors:** `401` invalid/expired bearer token; `403` missing authentication or role is not `CUSTOMER`; `404` visit does not exist or belongs to another customer; `409 Conflict` visit is no longer pending and cannot be cancelled.

Visit statuses are `PENDING`, `ACCEPTED`, `REJECTED`, and `CANCELLED`. Customers may cancel only their own pending requests. Agents can decide only pending requests for properties currently assigned to them.

## Admin dashboard

All `/api/admin/**` endpoints require `ADMIN`. Admins can see every user, agent, property, inquiry, and visit through the corresponding admin and existing list endpoints.

- `GET /api/admin/statistics` returns `totalUsers`, `totalAgents`, `totalCustomers`, `totalProperties`, `availableProperties`, `soldOrRentedProperties`, `totalInquiries`, `pendingInquiries`, `totalVisits`, and `pendingVisitRequests`. Pending inquiries are inquiries with `OPEN` status; pending visits have `PENDING` status. Sold/rented properties count listing statuses `SOLD` and `RENTED`.
- `GET /api/admin/users` returns the user directory as `UserResponse` objects.
- `GET /api/admin/agents` returns users with the `AGENT` role.
- `POST /api/admin/agents` creates an agent account using `firstName`, `lastName`, `email`, `password`, and optional `phoneNumber`. The endpoint assigns the existing `AGENT` role and stores a BCrypt password hash. Duplicate email addresses are rejected; only admins may call this endpoint.
- `PUT /api/admin/agents/{agentId}` updates an agent's `firstName`, `lastName`, `email`, and optional `phoneNumber`. It cannot change the agent's role or password.
- `PATCH /api/admin/agents/{agentId}/deactivate` marks the agent inactive without deleting their account or related records. Deactivated agents cannot log in, and active JWTs are rejected immediately on their next request. `users.active` is additive and defaults to `TRUE`; apply `database/migrations/V1__add_user_active_status.sql` once to existing databases.
- `PATCH /api/admin/agents/{agentId}/activate` reactivates a deactivated agent.
- `DELETE /api/admin/agents/{agentId}` deletes an agent only when no property, visit, inquiry, or favorite records reference the account. Returns `409 Conflict` when related records must be preserved; deactivate the agent instead.
- `PUT /api/admin/users/{userId}/role` accepts `{"role":"AGENT"}` (or `CUSTOMER`/`ADMIN`) and updates the existing user's role. Admins cannot change their own role, demote the last administrator, or demote an agent while that agent owns properties. Reassign the agent's properties first.

The frontend displays separate customer, agent, and admin dashboard views after the existing login endpoint succeeds. Agent property list results are ownership-filtered in the backend; agent inquiry replies and visits are likewise scoped to their managed properties.

### Existing database update for inquiry replies

New databases receive the inquiry reply fields from `database/schema.sql`. For an existing database, run this once:

```sql
ALTER TABLE inquiries
    ADD COLUMN response_message TEXT NULL,
    ADD COLUMN responded_at DATETIME NULL;
```

## Health and frontend

### Health status

`GET /api/health`

- **Purpose:** Confirm the backend health endpoint is responding.
- **Authentication/role:** Public.
- **Request:** No parameters or body.
- **Example request:** `GET /api/health`
- **Success:** `200 OK`:

  ```json
  {
    "status": "UP",
    "message": "Backend is running"
  }
  ```

### Frontend entry page

`GET /`

- **Purpose:** Serve the frontend entry page.
- **Authentication/role:** Public.
- **Request:** No parameters or body.
- **Example request:** `GET /`
- **Success:** `200 OK`; HTML document.
- **Important errors:** `404 Not Found` if the frontend resource is unavailable.

The frontend's static assets are served at `/index.html`, `/styles.css`, and `/app.js`.
