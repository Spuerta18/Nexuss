# Returns and Refunds — Domain Services — NexusMarket

## Introduction

Return requests, their decision, and refunds.

Part of the [Domain Services](../Domain%20Services.md) catalog, which holds the design conventions, the complete service tables and the collaboration diagram.

---

# Returns and Refunds

A buyer can ask to return a delivered order through a `ReturnRequest`. Approving the request puts the returned stock back and creates a `Refund`, which an administrator then processes.

| Step | Service | Return request | Refund | Stock effect |
| ---- | ------- | -------------- | ------ | ------------ |
| Request | `RequestReturnService` | → `REQUESTED` | — | — |
| Approve | `ApproveReturnService` | `REQUESTED` → `APPROVED` | created `PENDING` | every line back to its warehouse |
| Reject | `RejectReturnService` | `REQUESTED` → `REJECTED` | — | — |
| Process refund | `ProcessRefundService` | — | `PENDING` → `PROCESSED` | — |

## Common Business Rules

* Each service authorizes the actor by role, through `ValidateRoleAuthorizationService`, before anything is loaded.
* The entities make their own transitions (`ReturnRequest.approve` / `reject`, `Refund.process`); a return request is decided exactly once and a refund is processed exactly once.

## Common Exceptions

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The actor is not active or its role is not allowed. |
| `EntityNotFoundException` | The order, return request or refund does not exist. |
| `BusinessRuleViolationException` | The return request is already decided, or the refund already processed. |

---

# RequestReturnService

## Description

Lets a buyer request the return of one of its delivered orders.

## Parameters

`execute(User actor, ReturnRequest returnRequest)`

| Parameter     | Type          | Description |
| ------------- | ------------- | ----------- |
| actor         | User          | Buyer who placed the order. |
| returnRequest | ReturnRequest | New model: its `identifier`, a reference to the `order` to return, and the `reason` given by the buyer. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `BUYER`.
2. Load the order and check, through `AuthorizeOrderAccessService`, that it belongs to the actor.
3. Check that the order is `DELIVERED`, that the return request identifier is not used, and that the order has no return request yet.
4. Create the return request (`REQUESTED`, `requestedAt` = now, `buyer` = the order's buyer) and save it.

## Business Rules

* Only the buyer who placed the order may request its return; administrators and supervisors may not request it on the buyer's behalf.
* Only `DELIVERED` orders can be returned.
* An order has at most one return request.

## Exceptions

In addition to the common exceptions:

| Exception | When |
| --------- | ---- |
| `OperationNotAllowedException` | The order belongs to another buyer. |
| `BusinessRuleViolationException` | The order is not `DELIVERED`. |
| `DuplicateResourceException` | The return request identifier is already used, or the order already has a return request. |

---

# ApproveReturnService

## Description

Approves a return request: every order line goes back to the warehouse it was shipped from, and a `PENDING` refund for the order total is created.

## Parameters

`execute(User actor, ReturnRequest returnRequestReference)`

| Parameter              | Type          | Description |
| ---------------------- | ------------- | ----------- |
| actor                  | User          | Administrator. |
| returnRequestReference | ReturnRequest | Reference to the return request to approve. |

**Returns:** `ReturnRequest`

## Flow

1. Authorize the actor as `ADMINISTRATOR`.
2. Load the return request and call `ReturnRequest.approve(actor)`, validated before any stock is touched.
3. For every order line, register the return of its quantity to its warehouse through `RegisterInventoryReturnService`, with the same actor.
4. Save the return request.
5. Create the refund (`PENDING`, identifier `REF-<return request identifier>`, amount = the order total) and save it through `RefundRepositoryPort`.

## Business Rules

* Only administrators may approve a return: an order can hold lines of several sellers, so no single seller has authority over the whole return.
* The refund amount is the same total the invoice charged: the sum of `quantity × unitPrice` over the order lines, as a `Money` in the order's currency.
* A return request has exactly one refund, so the refund identifier derives from the request's.

---

# RejectReturnService

## Description

Rejects a return request. Stock and refunds are not affected.

## Parameters

`execute(User actor, ReturnRequest returnRequestReference)`

| Parameter              | Type          | Description |
| ---------------------- | ------------- | ----------- |
| actor                  | User          | Administrator. |
| returnRequestReference | ReturnRequest | Reference to the return request to reject. |

**Returns:** `ReturnRequest`

## Business Rules

* Only administrators may reject a return, for the same reason as approving: an order can hold lines of several sellers.

---

# ProcessRefundService

## Description

Pays a pending refund back to the buyer.

## Parameters

`execute(User actor, Refund refundReference)`

| Parameter       | Type   | Description |
| --------------- | ------ | ----------- |
| actor           | User   | Administrator. |
| refundReference | Refund | Reference to the refund to process. |

**Returns:** `Refund`

## Business Rules

* Only administrators may process refunds.
