---
name: WhileSubscribed stateIn Pattern
description: Critical pattern for testing ViewModels whose uiState uses stateIn(WhileSubscribed). Must subscribe before advancing.
type: feedback
---

`CatalogViewModel.uiState` uses `stateIn(SharingStarted.WhileSubscribed(5_000))`. This means the upstream `combine()` only starts collecting when there is an active subscriber.

**The problem:** Calling `advanceUntilIdle()` before subscribing (before entering `.test {}`) has NO effect because no subscriber exists yet. The state stays at the `initialValue = CatalogUiState()` which has `isLoading=true` and empty lists.

**The fix:** Subscribe first by entering the Turbine `.test {}` block, THEN call `advanceUntilIdle()` inside it:

```kotlin
viewModel.uiState.test {
    advanceUntilIdle()           // now the upstream is active because we subscribed
    val state = expectMostRecentItem()
    // assert on state
    cancelAndIgnoreRemainingEvents()
}
```

Use `expectMostRecentItem()` (not `awaitItem()`) after `advanceUntilIdle()` because multiple emissions may have happened (initialValue + real value) and you want the latest settled state.

For ViewModels that do NOT use `stateIn` (e.g., `HomeViewModel` uses `MutableStateFlow` directly), the pattern of `advanceUntilIdle()` before asserting on `.value` works fine.

**Why:** Discovered when 8 CatalogViewModelTest tests failed with `expected:<3> but was:<0>` because `advanceUntilIdle()` was called before subscribing. Fixed by moving the advance inside the Turbine block (2026-04-07).

**How to apply:** Any ViewModel test that involves a `stateIn(WhileSubscribed(...))` StateFlow must follow the subscribe-then-advance pattern.
