---
name: Testing Conventions
description: MockK, JUnit4, Turbine, coroutines-test patterns established in the quoteAnime test suite
type: feedback
---

Use **MockK** (not Mockito) for all mocking. Observed in the first test session and established as the project standard.

Use **JUnit 4** (`@Test`, `@Before`, `@Rule`) — not JUnit 5. The project uses `junit:junit:4.13.2`.

Use **Turbine** for Flow testing. Preferred over `toList()` because it handles infinite/hot flows gracefully.

Use `coJustRun { }` for suspend functions that return Unit. Use `coEvery { } returns value` for suspend functions that return a value.

Use `every { }` (not `coEvery`) for non-suspend functions including those that return a Flow.

For ViewModel tests with `StandardTestDispatcher`, always pair `advanceUntilIdle()` with coroutine launches. The dispatcher does not execute coroutines eagerly — you must advance the clock explicitly.

Test method naming convention: backtick-quoted descriptive sentences in the format `given X, when Y, then Z` or `when Y, then Z`.

**Why:** Established when writing the first comprehensive test suite for this project (2026-04-07).

**How to apply:** Every new test file written for this project should follow these conventions without needing reminders.
