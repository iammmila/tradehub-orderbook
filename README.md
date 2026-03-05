<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React" />
  <img src="https://img.shields.io/badge/Apache%20Kafka-7.6-231F20?style=for-the-badge&logo=apachekafka&logoColor=white" alt="Kafka" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Eureka-Service%20Discovery-FF6F00?style=for-the-badge" alt="Eureka" />
 <a href="https://documenter.getpostman.com/view/25398672/2sBXcLec9b" target="_blank">
  <img src="https://img.shields.io/badge/Postman-API%20Docs-FF6C37?style=for-the-badge&logo=postman&logoColor=white" alt="Postman API Docs" />
</a>
</p>

<h1 align="center">📈 TradeHub OrderBook</h1>

<p align="center">
  <strong>A production-grade, event-driven electronic trading platform built with microservices architecture</strong>
</p>

<p align="center">
  Real-time order matching · Smart order routing · Live WebSocket notifications · Multi-exchange support
</p>

---

## Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Architecture](#-architecture)
- [Services in Detail](#-services-in-detail)
  - [API Gateway](#-api-gateway)
  - [Discovery Service](#-discovery-service)
  - [Auth Service](#-auth-service)
  - [Order Service](#-order-service)
  - [Trade Service](#-trade-service)
  - [Notification Service](#-notification-service)
  - [Frontend (React)](#-frontend-react)
- [Event-Driven Communication](#-event-driven-communication)
- [Database Design](#-database-design)
- [Security Architecture](#-security-architecture)
- [Smart Order Router (SOR)](#-smart-order-router-sor)
- [Real-Time Notifications](#-real-time-notifications)
- [API Reference](#-api-reference)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [Testing](#-testing)
- [Contributing](#-contributing)

---

## 🌐 Overview

**TradeHub OrderBook** is a full-stack electronic trading platform that simulates a real-world multi-exchange order book system. It is built as a distributed microservices ecosystem where each service owns its domain, communicates asynchronously via Apache Kafka, and registers dynamically through Netflix Eureka service discovery.

The platform allows users to place, match, and track orders across multiple simulated exchanges (London, NASDAQ, Tokyo), view live order books, receive real-time trade notifications, and leverage a **Smart Order Router** that automatically finds the best execution venue.

Whether you're exploring financial technology, learning microservices patterns, or building a portfolio project — TradeHub OrderBook demonstrates enterprise-level architecture with clean separation of concerns, event sourcing patterns, and a polished React frontend.

---

## ✨ Key Features

### Trading Engine
- **Real-time order matching** — Continuous limit order book with price-time priority
- **Multi-exchange support** — Trade across XLON (London), XNAS (NASDAQ), and XTKS (Tokyo)
- **Smart Order Router (SOR)** — Automatic venue selection based on best available price, fees, and fill probability
- **Advanced order types** — Limit, Market, Hidden Limit (iceberg), and Minimum Execution Size orders
- **Order lifecycle management** — Create, cancel, replace orders with full audit trail

### Real-Time Experience
- **WebSocket push notifications** — Instant trade and order updates via STOMP over SockJS
- **Live order book** — Aggregated or per-exchange bid/ask depth visualization
- **Unread notification badges** — Always know when something happens to your orders

### Authentication & Security
- **JWT-based authentication** — Stateless, secure token validation at the gateway
- **Google OAuth2 / OpenID Connect** — One-click sign-in with Google
- **Email verification** — Account email confirmation flow with expiring tokens
- **Password reset** — Secure email-based password recovery
- **Role-based access control** — Admin and User roles with method-level security

### Developer Experience
- **Service discovery** — Zero-config inter-service communication via Eureka
- **Kafka event streaming** — Fully decoupled services with asynchronous event propagation
- **Docker Compose** — One-command infrastructure setup (Kafka, Zookeeper, PostgreSQL, Kafka UI)
- **BDD testing** — Cucumber-based behavior-driven development with Gherkin feature files
- **Database migrations** — Flyway-managed schema versioning

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                         │
│                                                                          │
│    ┌─────────────┐      ┌──────────────┐      ┌───────────────────┐     │
│    │  React SPA  │      │  REST / HTTP │      │  WebSocket/STOMP  │     │
│    │  (Port 3000)│      │   Clients    │      │     Clients       │     │
│    └──────┬──────┘      └──────┬───────┘      └────────┬──────────┘     │
│           │                    │                       │                  │
└───────────┼────────────────────┼───────────────────────┼─────────────────┘
            │                    │                       │
            ▼                    ▼                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       API GATEWAY (Port 8080)                            │
│                                                                          │
│   ┌─────────────┐  ┌──────────────────┐  ┌────────────────────────┐     │
│   │   Routing   │  │  JWT Validation  │  │    Load Balancing      │     │
│   │   Engine    │  │  (Introspect)    │  │    (Eureka-aware)      │     │
│   └─────────────┘  └──────────────────┘  └────────────────────────┘     │
│                                                                          │
│   Routes:  /auth/** → AUTH    /orders/** → ORDER    /trades/** → TRADE  │
│            /orderbook/** → ORDER    /ws/** → NOTIFICATION               │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    DISCOVERY SERVICE (Port 8761)                          │
│                      Netflix Eureka Server                               │
│                                                                          │
│              All services register and discover here                     │
└──────────────────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┬──────────────────┐
           ▼               ▼               ▼                  ▼
┌─────────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────────┐
│  AUTH SERVICE   │ │ORDER SERVICE│ │TRADE SERVICE│ │ NOTIFICATION SVC │
│   (Port 8082)  │ │ (Port 8081) │ │ (Port 8083) │ │   (Port 8090)    │
│                 │ │             │ │             │ │                  │
│ • Registration  │ │ • Order     │ │ • Trade     │ │ • Kafka Consumer │
│ • Login / JWT   │ │   Matching  │ │   Storage   │ │ • WebSocket Push │
│ • OAuth2 Google │ │ • Order Book│ │ • Trade     │ │ • Notification   │
│ • Email Verify  │ │ • Smart     │ │   History   │ │   Storage        │
│ • Password Reset│ │   Routing   │ │ • Kafka     │ │ • Read/Unread    │
│ • User Profiles │ │ • Kafka     │ │   Consumer  │ │   Management     │
│                 │ │   Producer  │ │             │ │                  │
│  [auth_db]      │ │ [order_db]  │ │ [trade_db]  │ │ [notification_db]│
└─────────────────┘ └──────┬──────┘ └──────┬──────┘ └────────┬─────────┘
                           │               │                  │
                           ▼               ▼                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                       APACHE KAFKA (Port 9092)                           │
│                                                                          │
│   ┌──────────────────┐          ┌──────────────────────┐                │
│   │  orders.events   │          │   trades.events      │                │
│   │                  │          │                      │                │
│   │ • OrderCreated   │          │ • TradeCreated       │                │
│   │ • OrderCancelled │          │                      │                │
│   │ • OrderReplaced  │          │                      │                │
│   │ • OrderPartFilled│          └──────────────────────┘                │
│   │ • OrderFilled    │                                                  │
│   └──────────────────┘          ┌──────────────────────┐                │
│                                 │    Kafka UI          │                │
│                                 │    (Port 8085)       │                │
│                                 └──────────────────────┘                │
│                                                                          │
│                     Zookeeper (Port 2181)                                │
└──────────────────────────────────────────────────────────────────────────┘
```

### Communication Patterns

| Pattern | From → To | Purpose |
|---------|-----------|---------|
| **Synchronous REST** | Frontend → Gateway → Services | User-facing API calls |
| **Token Introspect** | Gateway → Auth Service | JWT validation on every request |
| **Kafka Events** | Order Service → Kafka → Trade Service | Async trade persistence |
| **Kafka Events** | Order Service → Kafka → Notification Service | Real-time order/trade notifications |
| **WebSocket STOMP** | Notification Service → Frontend | Push notifications to browser |
| **Eureka Discovery** | All Services ↔ Discovery Service | Dynamic service registration |

---

## 🔧 Services in Detail

### 🚪 API Gateway

> **Port:** `8080` · **Framework:** Spring Cloud Gateway (WebFlux)

The API Gateway is the single entry point for all client traffic. It provides:

- **Intelligent routing** — Routes requests to the correct downstream service based on URL path
- **JWT validation** — Intercepts every request, validates the token by calling Auth Service's introspect endpoint, and injects user identity headers (`X-User-Id`, `X-Username`, `X-Roles`)
- **Load balancing** — Eureka-aware load balancing across service instances
- **CORS management** — Configurable cross-origin resource sharing for the React frontend
- **WebSocket proxying** — Transparently proxies WebSocket connections to Notification Service

**Route Map:**

| Path Pattern | Target Service | Description |
|-------------|---------------|-------------|
| `/api/v1/auth/**` | Auth Service | Authentication & user management |
| `/api/v1/orders/**` | Order Service | Order CRUD operations |
| `/api/v1/orderbook/**` | Order Service | Order book queries |
| `/api/v1/exchanges/**` | Order Service | Exchange metadata |
| `/api/v1/route/**` | Order Service | Smart routing endpoints |
| `/api/v1/trades/**` | Trade Service | Trade history |
| `/api/v1/notifications/**` | Notification Service | Notification management |
| `/ws/**` | Notification Service | WebSocket connections |

**Public paths** (no JWT required): `/api/v1/auth/*`, `/actuator/*`, `/ws/*`, `/oauth2/*`

---

### 🔍 Discovery Service

> **Port:** `8761` · **Framework:** Netflix Eureka Server

A centralized service registry that enables dynamic service discovery. Each microservice registers itself on startup and can discover other services by name rather than hardcoded URLs. This enables:

- Zero-configuration inter-service communication
- Automatic load balancing across multiple instances
- Health monitoring of all registered services
- Graceful handling of service restarts

---

### 🔐 Auth Service

> **Port:** `8082` · **Database:** `auth_db` (PostgreSQL)

The authentication and identity provider for the entire platform.

**Capabilities:**

| Feature | Description |
|---------|-------------|
| **Local Registration** | Username/email/password sign-up with BCrypt hashing |
| **JWT Login** | Issues signed JWT tokens (1-hour expiry) with user claims |
| **Google OAuth2** | OIDC integration — login with Google, auto-provision local accounts |
| **Token Introspection** | Gateway-facing endpoint to validate tokens and extract identity |
| **Email Verification** | Send verification emails with expiring tokens via SMTP |
| **Password Reset** | Secure email-based password recovery with expiring tokens |
| **User Profiles** | View and update profile information, change password |
| **Role Management** | `ROLE_USER` and `ROLE_ADMIN` with method-level authorization |

**JWT Token Claims:**
```json
{
  "sub": "johndoe",
  "uid": 42,
  "verified": true,
  "roles": ["ROLE_USER"],
  "iat": 1709640000,
  "exp": 1709643600
}
```

**OAuth2 Flow:**
```
User clicks "Sign in with Google"
    → Redirects to Google consent screen
    → Google returns OIDC token to Auth Service
    → Auth Service creates/links local account
    → Generates JWT token
    → Redirects to frontend with token in URL fragment
    → Frontend stores token and authenticates
```

**Demo Accounts** (seeded on dev startup):

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ROLE_ADMIN |
| `user123` | `user123` | ROLE_USER |

---

### 📊 Order Service

> **Port:** `8081` · **Database:** `order_db` (PostgreSQL) · **Kafka Producer**

The heart of the trading platform. Manages the full order lifecycle from creation through matching to completion.

**Order Types:**

| Type | Description |
|------|-------------|
| `LIMIT` | Execute at a specific price or better |
| `MARKET` | Execute immediately at the best available price |
| `HIDDEN_LIMIT` | Limit order invisible in the public order book (iceberg) |
| `MIN_EXECUTION_SIZE` | Limit order with a minimum fill quantity threshold |

**Order Lifecycle:**

```
   ┌──────────┐     Match Found      ┌──────────────────┐     Fully Matched     ┌────────┐
   │   NEW    │ ──────────────────► │ PARTIALLY_FILLED │ ──────────────────► │ FILLED │
   └──────────┘                      └──────────────────┘                      └────────┘
        │                                    │
        │  User Cancel                       │  User Cancel
        ▼                                    ▼
   ┌───────────┐                        ┌───────────┐
   │ CANCELLED │                        │ CANCELLED │
   └───────────┘                        └───────────┘
```

**Matching Engine:**
- Price-time priority matching algorithm
- Iterates through resting orders sorted by best price then earliest time
- Supports partial fills — remaining quantity stays on the book
- Publishes `TradeCreatedEvent` to Kafka for every matched trade
- Publishes order lifecycle events (`OrderCreatedEvent`, `OrderFilledEvent`, etc.)

**Supported Exchanges:**

| Code | Exchange | Region |
|------|----------|--------|
| `XLON` | London Stock Exchange | Europe |
| `XNAS` | NASDAQ | North America |
| `XTKS` | Tokyo Stock Exchange | Asia |

Each exchange has its own fee structure (maker/taker basis points) used by the Smart Order Router for venue selection.

**Order Book:**
- Real-time bid/ask aggregation per instrument
- Per-exchange or cross-exchange aggregated views
- Configurable depth levels
- Filters out hidden orders from public book

---

### 💹 Trade Service

> **Port:** `8083` · **Database:** `trade_db` (PostgreSQL) · **Kafka Consumer**

Consumes trade events from the Order Service and maintains the canonical trade history.

**Responsibilities:**
- Listens to `trades.events` Kafka topic for `TradeCreatedEvent` messages
- Persists each trade with full details (instrument, price, quantity, buyer/seller IDs, exchange)
- Provides paginated trade history queries with optional instrument filtering
- Supports Specification-based dynamic queries

**Trade Record:**
```
Trade #1042
├── Instrument:  AAPL
├── Price:       $185.50
├── Quantity:    100
├── Buy Order:   #2001  (User: 42)
├── Sell Order:  #2015  (User: 87)
├── Exchange:    XNAS
└── Executed At: 2026-03-05T14:32:00
```

---

### 🔔 Notification Service

> **Port:** `8090` · **Database:** `notification_db` (PostgreSQL) · **Kafka Consumer · WebSocket Server**

The real-time notification engine that bridges Kafka events to user-facing WebSocket push notifications.

**Event Processing:**

| Kafka Event | Notification Created | Recipients |
|-------------|---------------------|------------|
| `OrderCreatedEvent` | "Order created: AAPL" | Order owner |
| `OrderCancelledEvent` | "Order cancelled: AAPL" | Order owner |
| `OrderReplacedEvent` | "Order updated: AAPL — price/qty changed" | Order owner |
| `OrderPartiallyFilledEvent` | "Order partially filled: AAPL" | Order owner |
| `OrderFilledEvent` | "Order filled: AAPL" | Order owner |
| `TradeCreatedEvent` | "Trade executed: AAPL" | **Both** buyer and seller |

**Notification Delivery:**
1. Kafka consumer receives event
2. Notification entity created and persisted to database
3. Notification DTO pushed to user's WebSocket queue (`/user/queue/notifications`)
4. Frontend receives via STOMP subscription and shows toast + updates badge

**Features:**
- Paginated notification history (newest first)
- Unread count endpoint for badge display
- Mark single or all notifications as read
- Flyway-managed database migrations
- Ownership validation (users only see their own notifications)

---

### ⚛ Frontend (React)

> **Port:** `3000` · **Framework:** React 19 · **Styling:** SCSS · **Charts:** Recharts

A modern single-page application that provides the complete trading experience.

**Pages & Routes:**

| Route | Page | Access |
|-------|------|--------|
| `/` | Landing Page | Public |
| `/login` | Sign In | Public |
| `/register` | Create Account | Public |
| `/forgot-password` | Password Recovery | Public |
| `/reset-password` | Set New Password | Public |
| `/verify-email` | Email Confirmation | Public |
| `/oauth2/success` | OAuth2 Callback | Public |
| `/app/dashboard` | Dashboard | Protected |
| `/app/orders` | Order Management | Protected |
| `/app/trades` | Trade History | Protected |
| `/app/trading` | Live Trading | Protected |
| `/app/settings` | User Settings | Protected |

**Key Components:**

| Component | Purpose |
|-----------|---------|
| **OrderEntryCard** | Place new orders — instrument, side, type, price, quantity, exchange/auto-route |
| **OrderBookPanel** | Live bid/ask depth display — aggregated or per-exchange |
| **OpenOrdersWidget** | View and manage active orders |
| **NotificationContainer** | Real-time toast notifications via WebSocket |
| **NotificationModal** | Full notification history with read/unread management |
| **Dashboard** | Overview with profile, stats, and recent activity |
| **GoogleButton** | One-click Google OAuth2 sign-in |

**Frontend Architecture:**
- **API Layer** — Centralized Axios instance with JWT interceptor
- **Context** — React Context for auth state and user profile
- **WebSocket** — STOMP client over SockJS with auto-reconnect (3s delay, heartbeat every 10s)
- **Form Validation** — Yup schema-based validation
- **Protected Routes** — Token-gated route wrapper
- **Responsive Design** — SCSS-based styling with component-level modules

---

## 📡 Event-Driven Communication

TradeHub uses **Apache Kafka** as the backbone for asynchronous inter-service communication. This ensures services are decoupled, resilient, and independently scalable.

### Kafka Topics

#### `orders.events` — Order Lifecycle

Published by **Order Service** · Consumed by **Notification Service**

```
OrderCreatedEvent         → When a new order is placed
OrderCancelledEvent       → When an order is cancelled by the user
OrderReplacedEvent        → When an order's price or quantity is modified
OrderPartiallyFilledEvent → When an order is partially matched
OrderFilledEvent          → When an order is completely matched
```

#### `trades.events` — Trade Execution

Published by **Order Service** · Consumed by **Trade Service** + **Notification Service**

```
TradeCreatedEvent → When two orders match and a trade is executed
  ├── instrument, price, quantity
  ├── buyOrderId, sellOrderId
  ├── buyerUserId, sellerUserId
  ├── exchangeCode
  └── createdAt
```

### Event Flow Diagram

```
 Order Service                    Kafka                    Consumers
 ─────────────                   ─────                    ─────────

   Create Order ──────► orders.events ──────► Notification Service
                            │                      │
   Match Orders             │                      ├─ Create notification
       │                    │                      └─ Push via WebSocket
       │                    │
       └──────────► trades.events ──────┬──► Trade Service
                                        │       └─ Persist trade record
                                        │
                                        └──► Notification Service
                                                └─ Notify buyer & seller
```

---

## 🗄 Database Design

Each microservice owns its dedicated PostgreSQL database, ensuring strict domain boundaries.

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │                        PostgreSQL Cluster                           │
 │                                                                     │
 │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌────────────────┐  │
 │  │ auth_db  │   │ order_db │   │ trade_db │   │notification_db │  │
 │  │          │   │          │   │          │   │                │  │
 │  │ • users  │   │ • orders │   │ • trades │   │• notifications │  │
 │  │ • roles  │   │          │   │          │   │                │  │
 │  │ • email_ │   │          │   │          │   │ Managed by     │  │
 │  │   tokens │   │          │   │          │   │ Flyway         │  │
 │  │ • reset_ │   │          │   │          │   │                │  │
 │  │   tokens │   │          │   │          │   │                │  │
 │  └──────────┘   └──────────┘   └──────────┘   └────────────────┘  │
 └─────────────────────────────────────────────────────────────────────┘
```

### Key Schema Details

**Users** — Supports both local and OAuth2 (Google) accounts with nullable passwords for OAuth users. Email verification status tracked per user.

**Orders** — Full order state with remaining quantity tracking for partial fills, exchange routing metadata (manual vs. SOR), visibility flag for hidden orders, and audit timestamp.

**Trades** — Immutable trade records linking buyer and seller orders with execution details.

**Notifications** — User-scoped notifications with type classification, read/unread tracking, and entity linking for navigation.

---

## 🔒 Security Architecture

### Authentication Flow

```
                        ┌──────────────────┐
                        │    Frontend       │
                        │  (React SPA)     │
                        └────────┬─────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              Local Login   Google OAuth2   Token Stored
              (POST /login)  (Redirect)    (localStorage)
                    │            │            │
                    ▼            ▼            ▼
               ┌─────────────────────────┐
               │     Auth Service        │
               │                         │
               │  • BCrypt password      │
               │    verification         │
               │  • OIDC claim mapping   │
               │  • JWT token generation │
               │    (HMAC SHA-256,       │
               │     1-hour expiry)      │
               └────────────┬────────────┘
                            │
                   JWT Token returned
                            │
                            ▼
              ┌──────────────────────────┐
              │      API Gateway         │
              │                          │
              │  Every request:          │
              │  1. Extract Bearer token │
              │  2. Call /introspect     │
              │  3. Inject X-User-Id,    │
              │     X-Username, X-Roles  │
              │  4. Forward to service   │
              └──────────────────────────┘
```

### Security Layers

| Layer | Mechanism | Details |
|-------|-----------|---------|
| **Transport** | HTTPS (production) | TLS termination at load balancer |
| **Authentication** | JWT (HMAC SHA-256) | 1-hour token expiry, signed with secret key |
| **Gateway Filtering** | `JwtAuthGatewayFilter` | Validates every non-public request |
| **Authorization** | `@PreAuthorize` | Role-based method-level security |
| **Ownership** | Service-level checks | Users can only access their own resources |
| **Password Storage** | BCrypt | Adaptive hashing algorithm |
| **OAuth2** | Google OIDC | Delegated authentication with account linking |
| **CSRF** | Disabled | Stateless API architecture (JWT-protected) |
| **CORS** | Configured | Restricted origins with credentials support |

---

## 🧭 Smart Order Router (SOR)

The Smart Order Router automatically determines the best exchange for order execution based on real-time market conditions.

### How It Works

```
  Incoming Order (AUTO routing)
           │
           ▼
  ┌─────────────────────┐
  │  SmartOrderRouter    │
  │                      │
  │  1. Query all venues │──► VenueQuoteService
  │  2. Get quotes       │     ├── XLON quote
  │  3. Rank by price    │     ├── XNAS quote
  │  4. Factor in fees   │     └── XTKS quote
  │  5. Select best      │
  └──────────┬───────────┘
             │
             ▼
  ┌─────────────────────┐
  │  VenueRankingService │
  │                      │
  │  Score = f(price,    │
  │    maker/taker fees, │
  │    fill probability) │
  └──────────┬───────────┘
             │
             ▼
     Best venue selected
     Route reason logged
```

### Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/route/quote` | Preview — shows the single best venue for an order |
| `GET /api/v1/route/plan` | Full plan — ranks all venues with scores and reasons |

### Exchange Fee Structure

Each exchange has individual maker/taker fee rates (in basis points) that affect routing decisions. The SOR considers the net effective price after fees to ensure the user gets the truly best execution.

---

## 🔔 Real-Time Notifications

### WebSocket Architecture

```
  Notification Service                     Frontend
  ────────────────────                    ─────────

  Kafka Event Received                    On Mount:
         │                                  │
         ▼                                  ▼
  Save to Database                    Create STOMP Client
         │                            (SockJS transport)
         ▼                                  │
  SimpMessagingTemplate                     ▼
  .convertAndSendToUser()            Subscribe to:
         │                            /user/queue/notifications
         ▼                                  │
  STOMP Message Broker ─────────────────────┘
                                            │
                                            ▼
                                    Dispatch CustomEvent
                                    ("notif:ws")
                                            │
                                    ┌───────┴──────┐
                                    │              │
                                Toast popup   Update badge
                                             (unread count)
```

**Connection Details:**
- Transport: SockJS (WebSocket with fallback)
- Protocol: STOMP messaging
- Heartbeat: 10 seconds (in/out)
- Auto-reconnect: 3-second delay
- Authentication: JWT token passed as query parameter

---

## 📖 API Reference
- **Postman API Documentation:** https://documenter.getpostman.com/view/25398672/2sBXcLec9b  
  *(All endpoints are routed via API Gateway. Most require `Authorization: Bearer <token>`.)*
### Auth Service — `/api/v1/auth`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/login` | Authenticate and receive JWT | — |
| `POST` | `/register` | Create new account | — |
| `GET` | `/introspect` | Validate token (gateway use) | Bearer |
| `POST` | `/forgot-password` | Request password reset email | — |
| `POST` | `/reset-password` | Reset password with token | — |
| `POST` | `/verify-email/request` | Request verification email | — |
| `POST` | `/verify-email/confirm` | Confirm email address | — |

### User Management — `/api/v1/users`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/me` | Current user profile | Bearer |
| `PUT` | `/me` | Update profile | Bearer |
| `PUT` | `/me/password` | Change password | Bearer |
| `GET` | `/{id}` | Get user by ID | Bearer |
| `GET` | `/` | List all users | Bearer |

### Order Service — `/api/v1/orders`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/` | Create new order | Bearer |
| `GET` | `/my` | User's orders (paginated) | Bearer |
| `GET` | `/` | All orders (filtered) | Bearer |
| `GET` | `/{id}` | Get order by ID | Bearer |
| `DELETE` | `/{id}` | Cancel order | Bearer |
| `PATCH` | `/{id}` | Replace/update order | Bearer |

### Order Book — `/api/v1/orderbook`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/` | Order book (per-exchange or aggregated) | Bearer |

### Exchanges — `/api/v1/exchanges`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/` | List supported exchanges | Bearer |

### Smart Routing — `/api/v1/route`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/quote` | Smart routing preview (best venue) | Bearer |
| `GET` | `/plan` | Full routing plan (all venues ranked) | Bearer |

### Trade Service — `/api/v1/trades`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/my` | User's trades (paginated) | Bearer |
| `GET` | `/` | All trades (filtered by instrument) | Bearer |

### Notification Service — `/api/v1/notifications`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/` | List notifications (paginated) | Bearer |
| `GET` | `/unread-count` | Unread notification count | Bearer |
| `POST` | `/{id}/read` | Mark notification as read | Bearer |
| `POST` | `/read-all` | Mark all as read | Bearer |

---

## 🧰 Tech Stack

### Backend

| Technology | Purpose |
|-----------|---------|
| **Java 21+** | Runtime platform |
| **Spring Boot 4.0.3** | Application framework |
| **Spring Cloud Gateway** | API gateway (WebFlux/reactive) |
| **Spring Cloud Netflix Eureka** | Service discovery |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | Database access (Hibernate) |
| **Spring Kafka** | Event streaming integration |
| **Spring WebSocket** | STOMP messaging |
| **JJWT 0.11.5** | JWT token creation/validation |
| **Flyway** | Database schema migrations |
| **Thymeleaf** | Email template rendering |
| **OpenFeign** | Declarative REST clients |
| **Cucumber 7.15** | BDD testing framework |

### Frontend

| Technology | Purpose |
|-----------|---------|
| **React 19** | UI framework |
| **React Router 7** | Client-side routing |
| **Axios** | HTTP client with interceptors |
| **STOMP.js** | WebSocket STOMP client |
| **SockJS** | WebSocket transport with fallback |
| **Recharts** | Data visualization / charts |
| **Yup** | Schema-based form validation |
| **SCSS** | Styling / CSS preprocessor |
| **React Hot Toast** | Toast notifications |
| **React Icons** | Icon library |
| **React Helmet Async** | Document head management |

### Infrastructure

| Technology | Purpose |
|-----------|---------|
| **PostgreSQL 16** | Relational database |
| **Apache Kafka 7.6** | Event streaming platform |
| **Apache Zookeeper** | Kafka coordination |
| **Kafka UI** | Cluster monitoring dashboard |
| **Docker Compose** | Container orchestration |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21+** — [Download](https://adoptium.net/)
- **Node.js 18+** — [Download](https://nodejs.org/)
- **Docker & Docker Compose** — [Download](https://www.docker.com/)
- **PostgreSQL 16** — [Download](https://www.postgresql.org/) (or use Docker)

### 1. Start Infrastructure

Launch Kafka, Zookeeper, Kafka UI, and PostgreSQL:

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** on port `2181`
- **Kafka** on port `9092`
- **Kafka UI** on port `8085` → [http://localhost:8085](http://localhost:8085)
- **PostgreSQL** on port `5432`

### 2. Create Databases

Connect to PostgreSQL and create the required databases:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE order_db;
CREATE DATABASE trade_db;
CREATE DATABASE notification_db;
```

### 3. Start Backend Services

Start each service in order (or in parallel — Eureka handles registration):

```bash
# Terminal 1 — Discovery Service (start first)
cd discovery-service
./mvnw spring-boot:run

# Terminal 2 — Auth Service
cd auth-service
./mvnw spring-boot:run

# Terminal 3 — Order Service
cd order-service
./mvnw spring-boot:run

# Terminal 4 — Trade Service
cd trade-service
./mvnw spring-boot:run

# Terminal 5 — Notification Service
cd notification-service
./mvnw spring-boot:run

# Terminal 6 — API Gateway (start after services register)
cd api-gateway
./mvnw spring-boot:run
```

### 4. Start Frontend

```bash
cd ui
npm install
npm start
```

The app will be available at [http://localhost:3000](http://localhost:3000)

### 5. Verify

- **Eureka Dashboard:** [http://localhost:8761](http://localhost:8761) — See all registered services
- **Kafka UI:** [http://localhost:8085](http://localhost:8085) — Monitor topics and messages
- **Frontend:** [http://localhost:3000](http://localhost:3000) — Open the trading platform
- **API Gateway:** [http://localhost:8080](http://localhost:8080) — Direct API calls

### Default Ports

| Service | Port |
|---------|------|
| Frontend (React) | 3000 |
| API Gateway | 8080 |
| Order Service | 8081 |
| Auth Service | 8082 |
| Trade Service | 8083 |
| Notification Service | 8090 |
| Eureka Discovery | 8761 |
| Kafka | 9092 |
| Kafka UI | 8085 |
| PostgreSQL | 5432 |
| Zookeeper | 2181 |

---

## 📁 Project Structure

```
tradehub-orderbook/
│
├── docker-compose.yml          # Infrastructure (Kafka, Zookeeper, PostgreSQL, Kafka UI)
│
├── discovery-service/          # Netflix Eureka service registry
│   └── src/main/java/          #   Single @EnableEurekaServer application
│
├── api-gateway/                # Spring Cloud Gateway
│   └── src/main/java/
│       ├── config/             #   Routes, CORS, WebClient config
│       └── filter/             #   JWT validation gateway filter
│
├── auth-service/               # Authentication & user management
│   └── src/main/java/
│       ├── controller/         #   Auth, User REST endpoints
│       ├── service/            #   Login, Register, Email, OAuth2, JWT
│       ├── model/              #   User, Role, Token entities
│       ├── repository/         #   JPA repositories
│       ├── security/           #   JWT filter, Security config, OAuth2 handlers
│       ├── dto/                #   Request/Response DTOs
│       └── config/             #   Data seeder, email config
│
├── order-service/              # Order management & matching engine
│   └── src/main/java/
│       ├── controller/         #   Order, OrderBook, Exchange, Route endpoints
│       ├── service/            #   Matching engine, Order book, Smart router
│       ├── model/              #   Order entity, enums (Type, Side, Status)
│       ├── kafka/              #   Event publishers & event classes
│       ├── exchange/           #   Exchange registry & metadata
│       ├── dto/                #   Request/Response DTOs
│       └── config/             #   Kafka config, data seeder
│
├── trade-service/              # Trade persistence & history
│   └── src/main/java/
│       ├── controller/         #   Trade REST endpoints
│       ├── service/            #   Trade storage, queries
│       ├── model/              #   Trade entity
│       ├── kafka/              #   TradeCreatedEvent consumer
│       ├── dto/                #   Request/Response DTOs
│       └── config/             #   Kafka config
│
├── notification-service/       # Real-time notifications
│   └── src/main/java/
│       ├── controller/         #   Notification REST endpoints
│       ├── service/            #   Notification CRUD & delivery
│       ├── model/              #   Notification entity
│       ├── kafka/              #   Order & Trade event consumers
│       ├── websocket/          #   STOMP/WebSocket configuration
│       ├── dto/                #   Notification DTOs
│       └── config/             #   Kafka, Flyway, security config
│
└── ui/                         # React 19 frontend
    ├── public/                 #   Static assets
    └── src/
        ├── api/                #   Axios API clients (auth, orders, trades, etc.)
        ├── auth/               #   Auth hooks (useAuthToken)
        ├── components/         #   Reusable UI components
        │   ├── Dashboard/      #     Dashboard widgets
        │   ├── Orders/         #     Order management UI
        │   ├── Navbar/         #     Navigation bar
        │   ├── NotificationContainer/  # Toast notifications
        │   ├── NotificationModal/      # Notification history
        │   └── ...             #     More components
        ├── context/            #   React Context providers
        ├── hooks/              #   Custom React hooks
        ├── pages/              #   Page-level components
        ├── routes/             #   Route definitions & guards
        ├── schema/             #   Yup validation schemas
        ├── utils/              #   Helper utilities
        └── ws/                 #   WebSocket client & bridge
```

---

## 🧪 Testing

### Backend — BDD with Cucumber

The Order Service and Trade Service include **Behavior-Driven Development** tests using Cucumber and Gherkin syntax.

```bash
# Run Order Service tests
cd order-service
./mvnw test

# Run Trade Service tests
cd trade-service
./mvnw test
```

Tests execute against an in-memory H2 database and cover:
- Order creation and validation
- Order matching scenarios
- Order cancellation and replacement
- Trade creation from matching

### Frontend

```bash
cd ui
npm test
```

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Areas for Contribution

- Additional exchange integrations
- Advanced order types (Stop-Loss, Trailing Stop)
- Performance benchmarking & optimization
- UI/UX enhancements and mobile responsiveness
- Additional test coverage
- Monitoring & observability (Prometheus, Grafana)
- Kubernetes deployment manifests

---

<p align="center">
  <strong>Built with passion for financial technology and clean architecture</strong>
</p>

<p align="center">
  <sub>TradeHub OrderBook — Where microservices meet the markets</sub>
</p>
