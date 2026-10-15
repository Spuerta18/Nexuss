# Audit and Notifications — Domain Services — NexusMarket

## Introduction

Internal services the security layer and other services use to trace operations and notify users.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# AuditOperationService

## Description

Records which authenticated user performed which operation, with the current timestamp, through `AuditLogPort`.

## Operation

`void record(String userId, String operation)`

## Business Rules

* Every authenticated operation must be traceable to the `User` who performed it. The security layer (`infrastructure/security`) calls this service for every authenticated request, so traceability does not depend on each service remembering to call it.

---

# NotifyUserService

## Description

Sends a message to a user through a `NotificationChannel`, via `NotificationPort`. It is meant to be injected into services that need to notify a user of an outcome.

## Operation

`void notify(String userId, NotificationChannel channel, String message)`

> **Note:** no service uses it yet. `NotificationPort` is backed by `LoggingNotificationAdapter`, which only logs the message (see [Persistence](../../Adapters/Persistence.md)).
