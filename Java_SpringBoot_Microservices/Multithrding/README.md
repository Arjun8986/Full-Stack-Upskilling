# Java Multithreading – Interview Notes

A step-by-step study path with runnable code (Java 8+; some examples need **Java 21** and are marked).

| # | File | What you learn |
|---|---|---|
| 1 | [`01-basics-and-ways-to-create-threads.md`](01-basics-and-ways-to-create-threads.md) | Process vs thread, **all ways to create threads**, `start()` vs `run()` |
| 2 | [`02-thread-lifecycle-and-methods.md`](02-thread-lifecycle-and-methods.md) | Lifecycle states, **all Thread methods**, `wait/notify`, interrupt, daemon |
| 3 | [`03-synchronization-and-locks.md`](03-synchronization-and-locks.md) | Race conditions, `synchronized`, `volatile`, atomics, locks, deadlock, `ThreadLocal`, JMM |
| 4 | [`04-executors-and-concurrency-utilities.md`](04-executors-and-concurrency-utilities.md) | Thread pools, `Future`, `CompletableFuture`, latches/barriers/semaphores, concurrent collections, Fork/Join, virtual threads |
| 5 | [`05-interview-questions-and-cheatsheet.md`](05-interview-questions-and-cheatsheet.md) | 50 Q&A, classic coding problems, cheat-sheet |

## How to run any example

Each runnable snippet starts with a `// File: Name.java` comment. Save it under that name and run:

```bash
javac Name.java
java Name
```

(or on Java 11+: `java Name.java`)

## Suggested study order

1. Read 01 → type out the three ways of creating threads.
2. Read 02 → run the lifecycle and interrupt demos.
3. Read 03 → reproduce the race condition, then fix it three ways (synchronized, atomic, lock).
4. Read 04 → rewrite your thread code using an `ExecutorService`.
5. Use 05 for revision the day before the interview and practise the coding problems without looking.
