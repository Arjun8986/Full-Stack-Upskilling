# 78 Scenario-Based Angular Interview Questions and Answers

**Target level:** Angular developer with approximately 4 years of experience  
**Topics:** Change detection, performance, RxJS, Reactive Forms, routing, HTTP, state management, architecture, testing, and production troubleshooting.

> **Interview tip:** For each scenario, explain (1) the symptom, (2) the root cause, (3) the solution, and (4) any trade-offs. Code examples use TypeScript and modern Angular patterns where appropriate; adapt syntax to the Angular version used by the project.

## 1. Change Detection and Performance

### 1. Your API returns updated data, but the UI does not refresh. How would you debug and fix it?

**Answer:**
First, verify that the API response is correct and that the component property is updated. Then check whether the component uses `OnPush`, whether an object was mutated instead of replaced, and whether the update happened outside Angular's normal change-detection flow.

For an `OnPush` component, replace object or array references when updating state:

```ts
this.user = { ...this.user, name: response.name };
this.items = [...this.items, newItem];
```

If an update comes from an external callback outside Angular's zone in a zone-based application, re-enter Angular's zone or use an appropriate change-detection mechanism. With signals, update the signal using `set()` or `update()`. Use `markForCheck()` when an `OnPush` view needs to be checked. Avoid calling `detectChanges()` as a routine workaround without understanding the cause.

### 2. You use `ChangeDetectionStrategy.OnPush`, but the child component does not update when the parent changes an object. Why?

**Answer:**
`OnPush` can skip checking a component when its input references have not changed and no other trigger marks it for checking. Mutating a property keeps the same object reference:

```ts
// May not trigger an OnPush child through its input reference
this.user.name = 'Priya';

// New reference
this.user = { ...this.user, name: 'Priya' };
```

Prefer immutable updates. Other triggers can include an event in the component subtree, an `AsyncPipe` emission, a signal update read by the template, or explicitly marking the view for checking.

### 3. A component's change detection runs too frequently, making the application slow. How would you optimize it?

**Answer:**
Use Angular DevTools and browser profiling to identify which components and expressions are expensive. Common improvements include:

- Use `OnPush` where appropriate.
- Prefer signals or observable streams with `AsyncPipe` for state updates.
- Avoid expensive function calls in templates.
- Use `trackBy` with `*ngFor` or `track` with `@for`.
- Debounce high-frequency input events.
- Run non-UI work outside Angular's zone when appropriate in zone-based applications.
- Split large components and lazy-load features.

Measure before and after changes; do not assume change detection is the bottleneck without profiling.

### 4. You have 10,000 records in an `*ngFor` list, and scrolling is slow. How would you improve performance?

**Answer:**
Render only what is needed and reduce DOM work.

1. Use Angular CDK virtual scrolling for large lists.
2. Add a stable tracking key:

```html
<div *ngFor="let item of items; trackBy: trackById">
  {{ item.name }}
</div>
```

```ts
trackById(index: number, item: Item): number {
  return item.id;
}
```

3. Use pagination or server-side filtering when suitable.
4. Avoid expensive template expressions and unnecessary nested components.
5. Consider `OnPush` and immutable data updates.

Virtual scrolling is often more effective than simply changing change-detection strategy because it reduces the number of DOM nodes.

### 5. A function called from an Angular template executes repeatedly. Why does this happen, and how would you fix it?

**Answer:**
Angular may evaluate template expressions during change-detection checks. A method such as `getFilteredItems()` can therefore run repeatedly and may allocate new arrays each time.

Instead, calculate the result when its inputs change, use a memoized selector/computed signal, or expose an observable and use `AsyncPipe`.

```ts
readonly filteredItems = computed(() =>
  this.items().filter(item => item.active)
);
```

```html
@for (item of filteredItems(); track item.id) {
  <div>{{ item.name }}</div>
}
```

Do not use a computed signal if its dependencies are not signals; choose a suitable reactive or memoized approach for the data source.

### 6. An event updates a normal class property, but the UI does not reflect the change. What could be the reason?

**Answer:**
Check whether the update occurs in a callback outside Angular's zone, whether the view is detached, or whether the property belongs to a component whose change detection is not being triggered. In an `OnPush` component, also check how the update is initiated.

Possible fixes include using Angular-managed events, `AsyncPipe`, signals, `NgZone.run()` for relevant external callbacks in zone-based applications, or `ChangeDetectorRef.markForCheck()`. Verify that the property is actually updated before changing change-detection code.

### 7. How would you prevent unnecessary child-component re-renders?

**Answer:**
Use `OnPush`, stable input references, immutable state updates, and stable tracking keys in lists. Avoid creating fresh arrays or objects in the parent template on every check. Pass only the data a child needs, and use signals or observables where they simplify reactive updates.

Remember that `OnPush` is a checking strategy, not a guarantee that a component will never be checked. Profile the application to verify the improvement.

### 8. You update an object using `push()` or mutate a property, but an `OnPush` child does not update. How do you fix it?

**Answer:**
Mutation keeps the original array or object reference, so an input reference comparison may not detect a change. Create a new reference:

```ts
this.items = [...this.items, newItem];

this.user = {
  ...this.user,
  address: { ...this.user.address, city: 'Delhi' }
};
```

For nested structures, copy each changed level. If the child also maintains its own copied state, ensure that state is synchronized correctly instead of assuming a new input automatically updates every internal property.

### 9. Your application becomes slow after navigating through multiple pages. How would you investigate memory leaks?

**Answer:**
Use browser DevTools' Memory panel to take heap snapshots before and after repeated navigation. Look for detached DOM nodes, retained components, growing arrays, and subscriptions or event listeners that keep objects reachable.

Common fixes:

- Use `AsyncPipe` for template subscriptions.
- Use `takeUntilDestroyed()` where supported by the Angular version.
- Unsubscribe from long-lived streams when needed.
- Remove manually registered event listeners.
- Clear timers and intervals.
- Ensure services do not retain obsolete component references.

A subscription to a finite HTTP observable normally completes; focus especially on long-lived streams and retained references.

### 10. When would you use `ChangeDetectorRef.markForCheck()`, `detectChanges()`, and `detach()`?

**Answer:**

- **`markForCheck()`**: Marks an `OnPush` view so it can be checked in a future change-detection cycle.
- **`detectChanges()`**: Immediately checks the view and its children. Use carefully when a local, immediate check is genuinely needed.
- **`detach()`**: Removes a view from the normal change-detection tree. You must arrange checks yourself, often with `detectChanges()`, if the view still needs updates.

Prefer normal Angular reactivity, signals, and `AsyncPipe` when they solve the problem cleanly. Detached views can become stale if manual checking is forgotten.

## 2. RxJS and API Handling

### 11. A user types into a search box, triggering an API request for every character. How would you reduce API calls?

**Answer:**
Use `debounceTime()` to wait for typing to pause, `distinctUntilChanged()` to skip identical terms, and `switchMap()` to switch to the latest request.

```ts
this.searchControl.valueChanges.pipe(
  map(value => value?.trim() ?? ''),
  debounceTime(300),
  distinctUntilChanged(),
  switchMap(term =>
    term ? this.api.search(term) : of([])
  ),
  takeUntilDestroyed()
).subscribe(results => {
  this.results = results;
});
```

Import the required RxJS operators and `of`. In Angular versions without `takeUntilDestroyed()`, use an appropriate cleanup pattern. `switchMap` unsubscribes from the previous inner observable; with Angular `HttpClient`, that generally aborts the in-flight request.

### 12. Multiple API requests must run in parallel, and you need all results before updating the UI. Which RxJS operator would you use?

**Answer:**
Use `forkJoin()` when each source completes and you need its final emitted value.

```ts
forkJoin({
  profile: this.api.getProfile(),
  permissions: this.api.getPermissions(),
  settings: this.api.getSettings()
}).subscribe({
  next: result => {
    this.profile = result.profile;
    this.permissions = result.permissions;
    this.settings = result.settings;
  },
  error: error => this.handleError(error)
});
```

`HttpClient` requests normally complete after responding, making them a good fit. If any source errors, `forkJoin` errors unless that source's error is handled. It is not suitable for sources that never complete unless they are bounded.

### 13. You need to call API B only after API A succeeds. How would you implement it?

**Answer:**
Use `switchMap()` when the flow should switch to the latest outer emission, or `concatMap()` when each outer emission must be processed sequentially.

```ts
this.api.getUser(userId).pipe(
  switchMap(user => this.api.getOrders(user.id))
).subscribe({
  next: orders => this.orders = orders,
  error: error => this.handleError(error)
});
```

This is preferable to nested subscriptions because it composes the asynchronous flow and centralizes error handling. If every incoming operation must run in order, choose `concatMap()` instead.

### 14. A user clicks Submit multiple times, causing duplicate API requests. How would you prevent this?

**Answer:**
Disable the button while the request is running, and use an RxJS strategy that matches the desired behavior. `exhaustMap()` ignores new submit events while the current request is active.

```ts
this.submitClicks$.pipe(
  exhaustMap(() => this.api.save(this.form.getRawValue())),
  takeUntilDestroyed()
).subscribe({
  next: () => this.form.markAsPristine(),
  error: error => this.handleError(error)
});
```

Also consider server-side idempotency for operations such as payments or record creation. Disabling a button alone is not a complete protection against duplicate requests.

### 15. You need to cancel the previous search API request when a new search term arrives. Which operator would you use?

**Answer:**
Use `switchMap()`. It unsubscribes from the previous inner observable when a new value arrives. Angular `HttpClient` usually cancels the previous in-flight request when the subscription is cancelled.

```ts
this.searchControl.valueChanges.pipe(
  debounceTime(250),
  distinctUntilChanged(),
  switchMap(term => this.api.search(term ?? ''))
).subscribe(results => this.results = results);
```

Use this for latest-value workflows such as autocomplete. Do not use it for operations where every request must finish, such as processing each queued write.

### 16. You need to process every request in order, without running them concurrently. Which operator fits best?

**Answer:**
Use `concatMap()`. It subscribes to each inner observable in sequence and waits for one to complete before starting the next.

```ts
from(recordIds).pipe(
  concatMap(id => this.api.processRecord(id))
).subscribe({
  next: result => this.handleResult(result),
  error: error => this.handleError(error)
});
```

This preserves sequential processing, but a slow request delays the queue. For a long or unbounded source, the queue can grow, so consider backpressure or batching.

### 17. One API in a group of requests fails, but you still need successful responses from the others. How would you handle it?

**Answer:**
Handle errors at the individual request level before combining them. That allows other requests to complete.

```ts
forkJoin({
  profile: this.api.getProfile().pipe(
    catchError(error => of(null))
  ),
  settings: this.api.getSettings().pipe(
    catchError(error => of(null))
  )
}).subscribe(result => {
  this.profile = result.profile;
  this.settings = result.settings;
});
```

Use an explicit result type or status object if `null` could be a valid value. This approach distinguishes partial failure from total failure and avoids hiding errors without logging or reporting them.

### 18. An API fails temporarily. How would you implement retry logic without creating an infinite loop?

**Answer:**
Use `retry()` with a finite count and, if appropriate, delay retries with backoff. Retry only errors that are likely transient.

```ts
this.api.getData().pipe(
  retry({
    count: 2,
    delay: (error, retryCount) =>
      timer(retryCount * 1000)
  }),
  catchError(error => {
    this.errorMessage = 'Unable to load data.';
    return of(null);
  })
);
```

This example retries up to two times with increasing delays. Check the RxJS version because the configuration-object form depends on the version. Avoid blindly retrying validation errors or non-idempotent writes.

### 19. The same API is called by multiple components. How would you avoid duplicate HTTP requests?

**Answer:**
Centralize the request in a service and share the observable when the use case permits it.

```ts
private readonly config$ = this.http
  .get<Config>('/api/config')
  .pipe(shareReplay({ bufferSize: 1, refCount: true }));

getConfig(): Observable<Config> {
  return this.config$;
}
```

Consider cache lifetime, refresh behavior, error handling, and whether the data is user-specific. `shareReplay` is not a complete cache policy by itself; a long-lived service may need explicit invalidation or a refresh mechanism.

### 20. You subscribe to an observable in a component, then navigate away. How do you prevent memory leaks?

**Answer:**
Prefer `AsyncPipe` for template-only subscriptions. For imperative subscriptions, use `takeUntilDestroyed()` in supported Angular versions.

```ts
private readonly destroyRef = inject(DestroyRef);

ngOnInit(): void {
  this.dataService.data$.pipe(
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(data => {
    this.data = data;
  });
}
```

Import `DestroyRef` and `takeUntilDestroyed` from their Angular packages. For older Angular versions, use a `Subject<void>` with `takeUntil()` and complete it in `ngOnDestroy()`. Finite HTTP observables generally complete automatically.

### 21. How would you show a global loader for multiple concurrent HTTP requests and hide it only when all requests finish?

**Answer:**
Use an HTTP interceptor and a shared loading service that tracks the number of active requests. Increment before forwarding the request and decrement in `finalize()`, which runs on completion, error, or unsubscription.

```ts
return next(req).pipe(
  finalize(() => loadingService.decrement())
);
```

Only show the loader when the active count changes from zero to one, and hide it when the count returns to zero. Ensure every increment has a corresponding decrement, and consider excluding background or silent requests. A boolean alone can hide the loader too early when requests overlap.

### 22. What is the difference between `switchMap`, `mergeMap`, `concatMap`, and `exhaustMap`? Give real project scenarios.

**Answer:**

- **`switchMap`**: Keeps the latest inner observable; use for autocomplete and route-driven reads.
- **`mergeMap`**: Runs inner observables concurrently; use when independent operations can run in parallel.
- **`concatMap`**: Queues inner observables sequentially; use for ordered saves or processing.
- **`exhaustMap`**: Ignores new source emissions while an inner observable is active; use for preventing repeated login or submit actions.

Choose based on the business requirement. These operators differ in concurrency and cancellation behavior, not just syntax.

## 3. Reactive Forms and Validation

### 23. You have a Reactive Form with 100+ fields. How would you organize the form efficiently?

**Answer:**
Split the form into logical sections, use nested `FormGroup`s and `FormArray`s where appropriate, and extract reusable controls into child components. Keep validation rules close to the relevant form model and use typed forms if the Angular version supports them.

```ts
this.form = this.fb.group({
  personal: this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required]
  }),
  contacts: this.fb.array([]),
  medical: this.fb.group({
    notes: ['']
  })
});
```

Load data carefully with `patchValue()`, avoid rebuilding the entire form unnecessarily, and consider lazy or step-based sections for very large workflows.

### 24. A field is required only when another dropdown has a specific value. How would you implement conditional validation?

**Answer:**
Subscribe to the controlling field and update the dependent validator. Recalculate validity after changing validators.

```ts
const type = this.form.get('type');
const details = this.form.get('details');

type?.valueChanges.pipe(
  startWith(type.value),
  takeUntilDestroyed()
).subscribe(value => {
  if (value === 'Other') {
    details?.setValidators([Validators.required]);
  } else {
    details?.clearValidators();
  }

  details?.updateValueAndValidity();
});
```

Preserve any existing validators when updating them. A group-level custom validator may be cleaner when the rule depends on multiple fields and should be evaluated together.

### 25. How would you dynamically add and remove controls using `FormArray`?

**Answer:**
Use a `FormArray` for a variable number of similar controls or groups.

```ts
get contacts(): FormArray {
  return this.form.get('contacts') as FormArray;
}

addContact(): void {
  this.contacts.push(this.fb.group({
    type: [''],
    value: ['', Validators.required]
  }));
}

removeContact(index: number): void {
  this.contacts.removeAt(index);
}
```

Bind the array in the template using `formArrayName` and `formGroupName`. Validate indexes and preserve any business rules about minimum or maximum entries.

### 26. An API returns existing user data. How do you populate a nested form without losing existing controls?

**Answer:**
Build the form structure first, then use `patchValue()` for fields that exist in the form.

```ts
this.form.patchValue(apiResponse);
```

`patchValue()` does not automatically create missing controls or `FormArray` entries. For arrays, create the appropriate groups first, then patch the values. Use `setValue()` when you want Angular to enforce an exact match between the supplied object and the form structure.

### 27. How would you implement cross-field validation, such as confirming that two password fields match?

**Answer:**
Attach a custom validator to the containing `FormGroup`, because the rule depends on two controls.

```ts
const passwordsMatch: ValidatorFn = control => {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;

  return password === confirm ? null : { passwordMismatch: true };
};

this.form = this.fb.group({
  password: ['', Validators.required],
  confirmPassword: ['', Validators.required]
}, { validators: passwordsMatch });
```

Display the error when appropriate, such as after the user has touched the confirmation field. Avoid manually setting errors in a way that overwrites unrelated validation errors.

### 28. You disable a form control, but its value is missing from `form.value`. How do you retrieve all form values?

**Answer:**
Disabled controls are excluded from a form group's normal `value`. Use `getRawValue()` when you need values from both enabled and disabled controls.

```ts
const payload = this.form.getRawValue();
```

Do not assume disabled controls should always be sent to the backend. Decide which fields belong in the API payload and enforce authorization and validation on the server.

### 29. How do you prevent the user from submitting a form until all asynchronous validations finish?

**Answer:**
Angular marks a form as `PENDING` while asynchronous validation is running. Disable submission when the form is invalid or pending, and prevent repeated submits.

```html
<button
  type="submit"
  [disabled]="form.invalid || form.pending || saving">
  Save
</button>
```

In the submit handler, check the same conditions before calling the API. Async validators should return observables or promises that complete with a validation result; avoid validators that never finish.

### 30. A form is valid, but the Submit button remains disabled. How would you debug it?

**Answer:**
Inspect the exact disabled expression and all state values it depends on. A button may depend on `form.invalid`, `form.pending`, a `saving` flag, a permission check, or a custom condition.

Check the form status (`VALID`, `INVALID`, `PENDING`, or `DISABLED`), individual control errors, and whether the button is disabled by CSS or another attribute. If the button is disabled by a stale application flag, trace where that flag is set and reset.

### 31. How would you display server-side validation errors against the corresponding form controls?

**Answer:**
Map server error keys to known controls, set errors, and display messages beside the relevant fields.

```ts
const control = this.form.get('email');
control?.setErrors({
  ...(control.errors ?? {}),
  server: 'This email is already registered.'
});
```

Be careful: later validation runs can replace manually set errors. A more robust approach may store server errors separately or reapply them appropriately. Clear server errors when the user edits the field, and never trust client-side validation as the only validation layer.

### 32. When would you use `setValue()`, `patchValue()`, and `reset()`?

**Answer:**

- **`setValue()`**: Sets all controls and requires the supplied structure to match the form.
- **`patchValue()`**: Updates only the supplied controls, useful for partial API responses.
- **`reset()`**: Resets controls to initial or supplied values and generally resets their pristine/untouched state.

Use `reset()` after a successful save only if that matches the desired user experience. Consider whether the form should reset to defaults or retain the saved values.

## 4. Routing, Lazy Loading, and Authentication

### 33. A user directly opens a nested route, but the application displays a 404 error. How would you fix it?

**Answer:**
Check whether the Angular route is configured correctly and whether the web server is configured to serve the SPA's `index.html` for application routes. A server that only looks for physical files can return 404 for routes such as `/users/123`.

Also verify route parameters, base-href configuration, deployment path, and any reverse-proxy rules. Do not redirect every unknown URL to the application if the server must still return real 404s for missing assets or APIs.

### 34. How would you protect routes so that only authenticated users can access them?

**Answer:**
Use a route guard that checks the application's authentication state and redirects unauthenticated users to login, ideally preserving the requested URL.

```ts
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated()
    ? true
    : router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
      });
};
```

The example assumes `isAuthenticated()` is synchronous. If authentication must be refreshed asynchronously, return an observable or promise. Route guards improve navigation behavior but are not a security boundary; the backend must authorize every protected operation.

### 35. A user logs in successfully but is redirected to the wrong page. How would you debug the routing logic?

**Answer:**
Trace the login response, stored authentication state, return URL, guard decisions, and any default route or wildcard route. Check for stale return URLs, incorrect query-parameter handling, relative navigation, and competing redirects.

Validate the return URL to prevent open redirects. Log navigation events during debugging and use Angular Router diagnostics where appropriate. Keep post-login redirection logic in one place rather than duplicating it across components and services.

### 36. How would you preserve the originally requested URL and redirect the user there after login?

**Answer:**
When a guard blocks navigation, pass `state.url` as a `returnUrl` query parameter. After login, read and validate that value, then navigate to it; otherwise navigate to a safe default page.

```ts
const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
const destination = this.isSafeInternalUrl(returnUrl)
  ? returnUrl!
  : '/dashboard';

this.router.navigateByUrl(destination);
```

Implement `isSafeInternalUrl()` to reject external or malformed destinations. The exact validation should fit the app's routing rules.

### 37. You have multiple feature modules. How would you implement lazy loading and reduce the initial bundle size?

**Answer:**
Lazy-load feature routes so their code is downloaded when the user visits the feature. With standalone routing, use `loadChildren` for route collections or `loadComponent` for a standalone component.

```ts
{
  path: 'reports',
  loadChildren: () =>
    import('./reports/reports.routes')
      .then(m => m.REPORT_ROUTES)
}
```

Also remove unused dependencies, inspect bundle statistics, avoid importing large libraries into the root bundle, and use production builds. Lazy loading improves initial load time but can make first navigation to a feature slower; preloading can help when justified.

### 38. How would you implement role-based access for Admin, Manager, and User routes?

**Answer:**
Represent route requirements in route data and have a guard check the current user's roles or permissions.

```ts
{
  path: 'admin',
  component: AdminComponent,
  canActivate: [roleGuard],
  data: { roles: ['Admin'] }
}
```

The guard should deny access when the user lacks the required permission and redirect or show an access-denied page. Prefer permission-based checks for complex systems. The backend must independently enforce the same authorization policy.

### 39. A route resolver makes navigation slow because it waits for multiple API calls. How would you optimize it?

**Answer:**
Determine whether every request is truly required before the page can render. Combine independent requests with `forkJoin()`, cache stable data, remove redundant calls, and keep the resolver focused on essential route data.

For nonessential data, load it after navigation and show a loading state. Add error handling so a failed request does not leave navigation in an unexplained state. Measure network latency and resolver duration before deciding whether to remove it.

### 40. What is the difference between `canActivate`, `canActivateChild`, `canDeactivate`, and `canMatch`?

**Answer:**

- **`canActivate`**: Controls whether a route can be activated.
- **`canActivateChild`**: Controls activation of child routes.
- **`canDeactivate`**: Controls whether the user can leave a route, often for unsaved-change warnings.
- **`canMatch`**: Controls whether a route configuration matches during route selection; useful for conditional route selection and some lazy-loading scenarios.

Return `true`, `false`, or a `UrlTree`/supported asynchronous equivalent as appropriate. Guards are client-side controls and cannot replace backend security.

### 41. How would you warn a user about unsaved changes when navigating away from a form?

**Answer:**
Use a `CanDeactivate` guard and track whether the form has unsaved changes, often using `form.dirty` plus any application-specific save state.

```ts
export interface CanLeaveForm {
  canLeave: () => boolean | Observable<boolean>;
}
```

The guard can call `canLeave()` and show a confirmation dialog when needed. Reset the dirty state after a successful save. Consider browser refresh or tab closing separately with `beforeunload`, noting browser limitations.

### 42. How would you handle expired JWT tokens and redirect users to login without causing an interceptor loop?

**Answer:**
Use an interceptor to detect an unauthorized response, coordinate token refresh if supported, and redirect to login when refresh fails. Exclude login and refresh endpoints from the refresh logic. Ensure concurrent 401 responses do not trigger many refresh requests.

Clear sensitive client state on logout and preserve a safe internal return URL. Never assume a JWT's client-side presence proves it is valid. The backend must validate signature, expiration, issuer, audience, and authorization on every protected request.

## 5. HTTP Interceptors and API Integration

### 43. How would you attach an authentication token to every API request?

**Answer:**
Use an HTTP interceptor to clone outgoing requests and add the authorization header to trusted API requests.

```ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getAccessToken();

  if (!token || !req.url.startsWith('/api/')) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }));
};
```

Use a trusted API-origin check rather than a loose string match in production, especially when absolute URLs are possible. Avoid attaching credentials to third-party requests. Protect token storage appropriately and use HTTPS.

### 44. How would you handle a global HTTP 401 or 403 response?

**Answer:**
Use an interceptor or centralized error handler for consistent behavior. A 401 usually means authentication is missing or invalid and may require token refresh or login. A 403 generally means the request is authenticated but not permitted; show an access-denied message rather than blindly logging the user out.

Avoid recursive handling of authentication endpoints and coordinate concurrent refresh attempts. Keep detailed technical errors in safe logs, not in user-facing messages.

### 45. Your application sends duplicate HTTP requests. How would you identify and eliminate the cause?

**Answer:**
Inspect the browser Network panel and trace subscriptions and lifecycle hooks. Common causes include subscribing more than once, using the same cold HTTP observable in multiple places, invoking API methods from template expressions, or triggering duplicate events.

Use `AsyncPipe`, share requests when appropriate, move data loading to a clear owner, and prevent repeated submits. Be cautious with `shareReplay`: it requires an intentional caching and invalidation policy.

### 46. How would you configure different API base URLs for development, testing, and production?

**Answer:**
Use environment-specific build configuration or deployment-time configuration, depending on the deployment model.

```ts
export const environment = {
  production: false,
  apiBaseUrl: '/api'
};
```

Build or deploy with the appropriate configuration and keep secrets out of frontend environment files because client bundles are visible to users. For one artifact deployed to multiple environments, fetch public runtime configuration before bootstrapping the application.

### 47. An API takes 15 seconds to respond. How would you implement timeout handling and user-friendly errors?

**Answer:**
Set an appropriate timeout based on the endpoint's expected latency, show a loading state, and offer a retry or cancellation path where useful.

```ts
this.http.get<Data>('/api/data').pipe(
  timeout(10000),
  catchError(error => {
    this.errorMessage =
      error.name === 'TimeoutError'
        ? 'The request took too long. Please try again.'
        : 'Unable to load data.';
    return EMPTY;
  })
);
```

Choose a timeout appropriate to the business operation. A client timeout does not necessarily mean the server stopped processing the request.

### 48. How would you implement a global loading indicator using an HTTP interceptor?

**Answer:**
Use an interceptor to increment a shared active-request counter and decrement it in `finalize()`. Expose loading state through a signal or observable so the app shell can display the indicator.

Exclude requests that should not affect the global loader, such as polling or background refreshes. Ensure cancellation and errors also decrement the count. If the application already has a counter-based loading service, reuse it instead of introducing another competing state source.

### 49. A GET request can be cached, but POST requests should not be cached. How would you design the caching logic?

**Answer:**
Cache only eligible GET requests, using a cache key that includes the relevant URL and query parameters and, where appropriate, user or tenant context. Set expiration and invalidation rules, and clear user-specific cache on logout.

Do not cache sensitive data indiscriminately. HTTP caching headers and browser caches may also participate. POST requests can be retried or made idempotent in some designs, but they should not be placed into a generic GET response cache.

### 50. How would you upload a large file and show upload progress?

**Answer:**
Use `HttpClient` with progress reporting enabled and send a `FormData` payload.

```ts
const formData = new FormData();
formData.append('file', file);

this.http.post('/api/upload', formData, {
  observe: 'events',
  reportProgress: true
}).subscribe(event => {
  if (event.type === HttpEventType.UploadProgress && event.total) {
    this.progress = Math.round(event.loaded / event.total * 100);
  }
});
```

Import `HttpEventType`. Progress reporting depends on the configured HTTP backend; verify support for the app's Angular version and backend. For very large files, consider chunking, resumable uploads, size limits, and server-side validation.

## 6. Component Communication and State Management

### 51. How would you pass data from a parent component to a child component?

**Answer:**
Use an input. Traditional components can use `@Input()`, while supported Angular versions also provide signal-based `input()`.

```ts
@Input() user!: User;
```

```html
<app-user-card [user]="selectedUser"></app-user-card>
```

Treat input data as parent-owned state. If the child needs to request a change, emit an output event or use an explicitly designed two-way binding API instead of mutating parent state unexpectedly.

### 52. Two unrelated components need to share data. What approaches would you consider?

**Answer:**
Use a shared service when the components need to communicate across the component tree. The service can expose an observable, a `BehaviorSubject`, or a signal, depending on the state and Angular version.

For complex cross-feature state, consider a structured state-management library. For data that is already represented in the URL, use route parameters or query parameters. Choose the smallest approach that supports ownership, updates, testing, and lifecycle needs.

### 53. A shared service uses a `Subject`, but a newly opened component does not receive the previous value. Why?

**Answer:**
A plain `Subject` multicasts only to current subscribers; it does not replay prior emissions to late subscribers.

Use a `BehaviorSubject` if a current value is required, a `ReplaySubject` if a configured number of prior values should be replayed, or a signal for synchronous reactive state.

```ts
private readonly selectedUserSubject = new BehaviorSubject<User | null>(null);
readonly selectedUser$ = this.selectedUserSubject.asObservable();
```

Define a sensible initial state and avoid exposing the subject itself for arbitrary mutation.

### 54. When would you use `Subject`, `BehaviorSubject`, `ReplaySubject`, and Angular signals?

**Answer:**

- **`Subject`**: Event stream for current subscribers; no stored current value.
- **`BehaviorSubject`**: Holds a current value and emits it immediately to new subscribers.
- **`ReplaySubject`**: Replays a configured history of emissions to new subscribers.
- **Signal**: Holds a synchronous value and notifies dependent reactive computations and templates when updated.

Use RxJS for asynchronous streams and rich stream composition; use signals for local or shared synchronous reactive state when suitable. They can work together, but conversion should be deliberate.

### 55. Several components depend on the same API data. How would you maintain a consistent state?

**Answer:**
Create a service that owns the data-loading and update rules. Expose read-only observable or signal state to components and centralize refresh, caching, and error behavior.

For simple state, a service may be sufficient. For complex state with many actions, effects, and derived views, a state-management library can provide clearer structure. Avoid each component independently fetching and mutating its own copy unless independent state is intentional.

### 56. A child component emits an event, but the parent does not receive it. How would you debug it?

**Answer:**
Verify that the child event is emitted on the expected code path, the parent template binds to the correct output name, and the child instance is the one visible in the template. Check spelling, conditional rendering, and whether an event is emitted before the parent establishes the relevant state.

For traditional output:

```ts
@Output() saved = new EventEmitter<User>();

save(user: User): void {
  this.saved.emit(user);
}
```

```html
<app-user-form (saved)="handleSaved($event)"></app-user-form>
```

For supported Angular versions, signal-based outputs are also available.

### 57. When should you use a shared service instead of passing data through multiple levels of `@Input()` and `@Output()`?

**Answer:**
Use input/output bindings when the communication follows a clear parent-child relationship. A shared service may be cleaner when unrelated components need the same state or when passing events through many intermediate components creates excessive wiring.

Keep state ownership explicit. A service should not become an unstructured global event bus; define typed APIs, observable or signal state, and clear update rules.

### 58. How would you prevent stale data from being displayed after switching between different users or records?

**Answer:**
Derive data loading from the selected identifier and use `switchMap()` so a newer selection supersedes the prior request.

```ts
this.route.paramMap.pipe(
  map(params => params.get('id')),
  filter((id): id is string => id !== null),
  distinctUntilChanged(),
  switchMap(id => this.api.getRecord(id))
).subscribe(record => this.record = record);
```

Clear or mark the old view as loading when the identifier changes, handle errors for each request, and ensure cached data is keyed by the correct user or record ID.

## 7. Angular Architecture and Advanced Concepts

### 59. How would you migrate an Angular application from NgModules to standalone components incrementally?

**Answer:**
Start with a small feature or route. Convert eligible components, directives, and pipes to standalone declarations, list their imports explicitly, and update route configuration to use standalone components or lazy-loaded route arrays.

Angular supports incremental migration; a full rewrite is not required. Run unit tests and production builds after each step, check shared dependencies, and keep module-based features working during the transition. Follow the migration guidance for the target Angular version.

### 60. Your application has a large shared module imported into every feature module. What problems could this cause?

**Answer:**
A shared module with many unrelated declarations and dependencies can make dependencies hard to understand and may increase bundle size if it causes large libraries to be included in eagerly loaded code. It can also make ownership and testing unclear.

Split it into focused shared UI, utility, and feature-specific areas. Import only what each feature needs and inspect the actual production bundle before claiming a size problem; tree shaking may remove unused code.

### 61. How would you implement a reusable table component that supports different data models?

**Answer:**
Design a generic table around typed input data, column definitions, sorting, paging, and row events. Keep domain-specific formatting outside the generic table where possible.

```ts
interface TableColumn<T> {
  key: keyof T;
  label: string;
}

@Input() rows: readonly T[] = [];
@Input() columns: readonly TableColumn<T>[] = [];
@Output() rowSelected = new EventEmitter<T>();
```

The snippet illustrates the idea; a generic component class needs a suitable generic type declaration in the actual implementation. Use stable row keys, accessible table markup, and test sorting, empty states, and large datasets.

### 62. A service is unexpectedly instantiated more than once. How would you investigate Angular dependency injection scopes?

**Answer:**
Check whether the service uses `providedIn: 'root'`, is listed in multiple module/component `providers` arrays, or is provided in a lazy-loaded or route-level injector. Component-level providers create instances scoped to that component subtree.

If the service should be application-wide, use a root provider and avoid redeclaring it in lower scopes. Multiple instances may be intentional for isolated state, so confirm the required lifetime before changing providers.

### 63. What happens if a component uses `@ViewChild()` to access an element inside an `*ngIf` block?

**Answer:**
The queried element does not exist while the condition is false. With a dynamic query (`static: false`, the usual default), Angular updates the query as the view changes; access it after the view is initialized and check for `undefined` when the element is conditional.

```ts
@ViewChild('inputElement') inputElement?: ElementRef<HTMLInputElement>;

focusInput(): void {
  this.inputElement?.nativeElement.focus();
}
```

Avoid accessing the element before it has been created. Prefer template-driven behavior where direct DOM access is not needed.

### 64. How would you build a custom structural or attribute directive?

**Answer:**
An attribute directive changes the behavior or appearance of an existing element; a structural directive controls whether or how template content is rendered.

For an attribute directive, use a directive class and dependency injection for the host element or `ElementRef`, and use `Renderer2` when appropriate for DOM changes. For structural directives, use `TemplateRef` and `ViewContainerRef` to create or clear embedded views. Make directives standalone where supported and test lifecycle behavior, accessibility, and cleanup.

### 65. How would you create a custom form control that integrates with Angular Reactive Forms using `ControlValueAccessor`?

**Answer:**
Implement `ControlValueAccessor` so Angular can write values to the component and receive user changes.

```ts
writeValue(value: string | null): void {
  this.value = value ?? '';
}

registerOnChange(fn: (value: string) => void): void {
  this.onChange = fn;
}

registerOnTouched(fn: () => void): void {
  this.onTouched = fn;
}

setDisabledState(disabled: boolean): void {
  this.disabled = disabled;
}
```

Also implement the `ControlValueAccessor` interface's required methods, register the component as an `NG_VALUE_ACCESSOR` provider, and invoke `onChange` and `onTouched` at the appropriate times. Test disabled state, touched state, reset behavior, and validation integration.

### 66. How would you manage environment-specific configuration and feature flags?

**Answer:**
Use build-time environment configuration when separate builds are acceptable, or load public runtime configuration when the same artifact must run in different environments. Keep feature flags in a typed service and make their loading/fallback behavior explicit.

Do not place secrets in Angular bundles. For flags that control access to sensitive functionality, enforce the underlying permission on the backend; hiding a button is not authorization.

### 67. What is the difference between Angular signals and RxJS observables, and when would you choose each?

**Answer:**
Signals provide synchronous reactive state: consumers read the current value, and Angular can track dependencies. Observables represent streams over time and provide operators for asynchronous workflows, cancellation, combination, and event processing.

Use signals for component state and derived synchronous values. Use RxJS for HTTP composition, user event streams, and timing or concurrency logic. Convert between them when useful, and be careful to manage subscriptions when bridging long-lived streams.

### 68. How would you handle server-side rendering and hydration for an Angular application?

**Answer:**
Use Angular's SSR and hydration support when initial HTML rendering, search indexing, or perceived loading speed benefits the app. Ensure server-rendered and client-rendered markup is consistent and avoid accessing browser-only APIs during server rendering without guards.

Review authentication, caching, and per-user data carefully so server responses cannot leak between users. Test routes and interactions after hydration, and measure the benefit for the app's actual use case.

## 8. Debugging, Testing, and Production Issues

### 69. An application works locally but fails after deployment. What steps would you follow to troubleshoot it?

**Answer:**
Reproduce the issue using the production build and inspect:

1. Browser console and Network panel.
2. API URLs, CORS, status codes, and response payloads.
3. Environment/runtime configuration and base href.
4. Server SPA fallback and reverse-proxy rules.
5. Asset paths, case-sensitive filenames, and content security policy.
6. Build output, source maps where safely available, and deployment logs.

Compare local and production configuration systematically. Avoid exposing tokens or sensitive response data in logs.

### 70. How would you write unit tests for a component that depends on an HTTP service?

**Answer:**
Test the component's behavior while mocking or providing a controlled service dependency. If testing the actual HTTP service, use Angular's HTTP testing utilities.

```ts
TestBed.configureTestingModule({
  providers: [
    provideHttpClient(),
    provideHttpClientTesting()
  ]
});

const httpMock = TestBed.inject(HttpTestingController);
```

Call the service, use `expectOne()` to match the request, flush a success or error response, and call `httpMock.verify()` during cleanup. Check the imports and provider APIs for the Angular version in use.

### 71. How would you test an HTTP interceptor?

**Answer:**
Configure the HTTP client with the interceptor and testing backend, issue a request, then inspect the outgoing request to verify headers or other transformations.

```ts
provideHttpClient(withInterceptors([authInterceptor])),
provideHttpClientTesting()
```

Use `HttpTestingController` to match and flush the request. Add tests for missing tokens, excluded URLs, error handling, and refresh behavior. Provider order matters: configure `provideHttpClient()` before `provideHttpClientTesting()`.

### 72. A component has several subscriptions. How would you test that they are cleaned up correctly?

**Answer:**
Test observable behavior and component destruction. Use a controllable long-lived `Subject`, create the component, subscribe through its normal lifecycle, destroy the fixture, and verify that later emissions no longer cause component effects.

Where practical, prefer `AsyncPipe` or `takeUntilDestroyed()` to make cleanup automatic. Avoid tests that depend on implementation details when a behavior-level assertion is sufficient.

### 73. How would you mock API responses in Angular unit tests?

**Answer:**
For service tests, use `HttpTestingController` to match requests and flush controlled responses. For component tests, provide a spy or mock service returning `of(mockData)` or `throwError(() => error)`.

Test success, empty results, errors, and loading states. Keep mock objects aligned with the real response contract so tests do not pass with unrealistic data shapes.

### 74. How would you investigate a memory leak caused by event listeners, timers, or subscriptions?

**Answer:**
Use heap snapshots and allocation timelines to identify retained objects after repeated navigation or interaction. Inspect manual `addEventListener()` calls, `setInterval()`, long-lived observables, and closures that retain components.

Remove event listeners, clear timers, and unsubscribe or use lifecycle-aware cleanup. For RxJS streams, ensure the cleanup operator is placed appropriately in the pipeline, especially when higher-order operators create inner subscriptions.

### 75. How would you analyze and reduce the production bundle size?

**Answer:**
Build with production settings, inspect bundle statistics, and identify large dependencies or eagerly loaded features. Use lazy loading, remove unused packages and imports, avoid unnecessarily large date or utility libraries, and ensure tree-shaking-friendly imports.

Check for duplicated dependencies and large assets. Establish a bundle budget in the Angular build configuration and compare measured output after changes rather than relying on assumptions.

### 76. A PWA continues showing old application files after a deployment. How would you troubleshoot service worker updates?

**Answer:**
Check whether the new service-worker version was deployed completely, whether the browser detects an update, and whether cached assets or deployment paths are inconsistent. In Angular, listen to `SwUpdate.versionUpdates` and handle `VERSION_READY` according to the app's update policy.

Offer a safe refresh prompt when a new version is ready, and avoid automatically reloading during a critical unsaved workflow. Test production builds over HTTPS or the appropriate local development setup; service-worker behavior differs from ordinary development serving.

### 77. How would you handle an unexpected API response without crashing the entire page?

**Answer:**
Validate and normalize data at the service boundary, handle errors explicitly, and let the UI represent loading, success, empty, and error states.

```ts
this.api.getItems().pipe(
  catchError(error => {
    this.logger.error('Failed to load items', error);
    this.errorMessage = 'Items could not be loaded.';
    return of([]);
  })
).subscribe(items => {
  this.items = items;
});
```

Use runtime validation when the API contract is uncertain or important. Do not silently turn every error into an empty success if that would mislead users; preserve enough information to distinguish failure from no data.

### 78. How would you identify and fix circular dependencies between Angular services?

**Answer:**
Look for dependency-injection errors and inspect the service dependency graph. For example, `UserService` depends on `AuthService`, while `AuthService` depends on `UserService`, creating a cycle.

Break the cycle by separating shared responsibilities into a third service, moving orchestration to a higher-level layer, or replacing a direct dependency with a clearer event or interface boundary. Avoid using `Injector` or lazy lookup simply to hide a design problem unless there is a justified architectural reason.

---

## Quick Revision: Important RxJS Operator Selection

| Requirement | Operator / approach | Why |
|---|---|---|
| Latest search result only | `switchMap` | Switches away from previous inner stream |
| Run independent requests concurrently | `mergeMap` | Supports concurrent inner subscriptions |
| Process every operation in order | `concatMap` | Queues inner observables |
| Ignore repeated submit events while busy | `exhaustMap` | Ignores new source emissions while active |
| Wait for several finite requests | `forkJoin` | Emits their final values when all complete |
| Wait until typing pauses | `debounceTime` | Reduces high-frequency emissions |
| Ignore repeated consecutive values | `distinctUntilChanged` | Emits only when the value changes |
| Recover from an error | `catchError` | Replaces or rethrows the failed stream |
| Clean up when a component is destroyed | `takeUntilDestroyed` or `AsyncPipe` | Ties subscription lifetime to the view/component |

## Suggested Preparation Plan

1. **First pass:** Questions 1–22 — change detection, performance, and RxJS.
2. **Second pass:** Questions 23–50 — Reactive Forms, routing, and HTTP.
3. **Third pass:** Questions 51–68 — communication, state, and architecture.
4. **Final pass:** Questions 69–78 — testing, production debugging, and service-worker issues.

For each question, practise explaining the solution aloud, writing a small code example, and discussing why you chose that approach over the alternatives.
