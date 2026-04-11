# OAuth 2.0 and OpenID Connect in a Microservice Architecture Demo

Bachelor thesis project demonstrating OAuth 2.0 and OpenID Connect in a microservice architecture.

## Stack

Spring Boot 3.3, Spring Cloud Gateway, Spring Security OAuth2 Resource Server, Keycloak 24, PostgreSQL 16, Docker Compose.

## Architecture

The system consists of three Spring Boot services and one Authorization Server. The API Gateway runs on port 8080 and acts as the single entry point. It validates JWT tokens and routes requests to the User Service (port 8081) or Order Service (port 8082). Both microservices independently validate JWT signatures using Keycloak's public key. Keycloak runs on port 8180 and handles authentication, token issuance, and user management. PostgreSQL on port 5432 stores data for Keycloak and both microservices in separate databases.

## How to run

Start the infrastructure with `docker compose up -d`. Open http://localhost:8180 and log in with admin / admin. Create a realm called diploma realm, then create a client called gateway client with Standard flow, Direct access grants, and Service accounts roles enabled. Copy the client secret. Create realm roles admin and user. Create two test users: testadmin with password admin123 and role admin, testuser with password user123 and role user.

Then start each service in its own terminal using `./gradlew bootRun`. Run the user service first, then the order service, then the gateway.

## API endpoints

All endpoints go through the gateway at http://localhost:8080.

User service: GET /api/users (admin only), GET /api/users/me (current user profile), GET /api/users/{id} (authenticated), PUT /api/users/{id} (owner or admin).

Order service: GET /api/orders (admin sees all, user sees own), GET /api/orders/{id} (owner or admin), POST /api/orders (authenticated).

## Authentication

The client requests a token directly from Keycloak at http://localhost:8180/realms/diploma realm/protocol/openid connect/token using grant_type password (for testing) or authorization_code (for production flows). The returned access token is sent in the Authorization header as Bearer when calling the gateway.

## Postman collection

The repository includes a Postman collection covering all authentication flows and API endpoints. Import it from the postman folder. Pre request scripts automatically save the access and refresh tokens to environment variables, so once you run the login request all other endpoints work without manual token copying.

## Tests performed

Seven core scenarios were verified end to end against the running system.

Test 1: Request without a token returns 401 Unauthorized.

Test 2: Request with a regular user token to an admin only endpoint returns 403 Forbidden.

Test 3: Request with an admin token to an admin only endpoint returns 200 OK.

Test 4: Request to GET /api/users/me with a valid user token returns 200 OK and automatically creates the user profile from JWT claims on first access.

Test 5: POST /api/orders with a valid user token creates a new order and returns 201 Created.

Test 6: GET /api/orders with a regular user token returns only that user's own orders.

Test 7: GET /api/orders with an admin token returns all orders from all users.

Additionally, the refresh token flow was verified by waiting for the access token to expire, then exchanging the refresh token for a new access token at the Keycloak token endpoint. The logout flow was verified by calling the Keycloak logout endpoint and confirming that the refresh token can no longer be used.
