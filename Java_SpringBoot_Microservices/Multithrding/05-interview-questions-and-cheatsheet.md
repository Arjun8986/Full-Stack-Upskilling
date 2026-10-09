# 05 – Interview Questions, Coding Problems & Cheat-sheet

## Part A – Theory Q&A

### Basics
**1. What is a thread? Process vs thread?**
A thread is the smallest unit of execution; threads in a process share heap/method area but have their own stack and PC register. Processes are isolated, heavier, and communicate via IPC.

**2. Ways to create a thread in Java?**
Extend `Thread`; implement `Runnable`; `Callable` with `FutureTask`/`ExecutorService`; thread pools; `CompletableFuture`; virtual threads (Java 21). Best practice: Runnable/Callable + executor.

**3. Why prefer `Runnable` over extending `Thread`?**
Allows extending another class, decouples task from execution, reusable in pools, better design.

**4. `start()` vs `run()`?**
`start()` creates a new thread and invokes `run()` in it; calling `run()` directly executes on the current thread. `start()` twice → `IllegalThreadStateException`.

**5. `Runnable` vs `Callable`?**
`Callable.call()` returns a value and can throw checked exceptions; `Runnable.run()` returns void and can't throw checked exceptions.

**6. Thread lifecycle states?**
`NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED`.

**7. What is a daemon thread?**
Background service thread; JVM doesn't wait for it before exiting. Set via `setDaemon(true)` before `start()`.

**8. Can we restart a dead thread?** No.

**9. What happens if the main thread finishes while other threads run?**
JVM stays alive until all **non-daemon** threads finish.

**10. What is the thread scheduler? Can we control it?**
OS/JVM component that decides which runnable thread runs. Priorities/yield are hints only.

### Methods
**11. `sleep()` vs `wait()`?**
`sleep` is a static `Thread` method, keeps locks, no `synchronized` needed. `wait` is an `Object` method, releases the monitor, must be in `synchronized`, awakened by `notify`.

**12. Why are `wait/notify/notifyAll` in `Object`?**
Locks are per-object; any object can be a monitor.

**13. `notify()` vs `notifyAll()`?**
One arbitrary waiter vs all waiters. Use `notifyAll` unless you're sure only one kind of waiter exists.

**14. Why call `wait()` in a loop?**
Spurious wakeups and conditions changing before the woken thread re-acquires the lock.

**15. What does `join()` do?**
Current thread waits for the target to terminate (optionally with timeout).

**16. What does `yield()` do?**
Hints the scheduler to let other threads of equal priority run; no guarantee.

**17. How do you stop a thread safely?**
Cooperative cancellation: `interrupt()` + check `isInterrupted()`/handle `InterruptedException`, or a `volatile boolean` flag. Never `stop()` (deprecated).

**18. What happens to the interrupt flag when `InterruptedException` is thrown?**
It is cleared. Rethrow or call `Thread.currentThread().interrupt()` to restore.

**19. `isInterrupted()` vs `Thread.interrupted()`?**
Instance method reads the flag; static method reads **and clears** the flag of the current thread.

**20. How to handle exception thrown in a thread?**
`setUncaughtExceptionHandler` / `setDefaultUncaughtExceptionHandler`; with executors use `Future.get()` → `ExecutionException`.

### Synchronization
**21. What is a race condition? Data race?**
Outcome depends on the timing of threads accessing shared mutable state. A data race is unsynchronized conflicting access (at least one write).

**22. What does `synchronized` guarantee?**
Mutual exclusion, visibility (happens-before on release→acquire), and ordering. It is reentrant.

**23. Object-level vs class-level lock?**
Instance `synchronized` → lock on `this`. `static synchronized` → lock on `Class` object. They are independent.

**24. `synchronized` method vs block?**
Block gives finer granularity, lets you choose the lock object, and holds the lock for less time.

**25. What is `volatile`?**
Guarantees visibility and prevents reordering; **not atomicity** – `count++` on a volatile is still unsafe.

**26. `volatile` vs `synchronized` vs `Atomic`?**
volatile: visibility only. synchronized: atomicity + visibility, blocking. Atomic: lock-free atomic single-variable operations via CAS.

**27. What is CAS?**
Compare-And-Swap: atomically set a value only if it equals the expected value (CPU instruction). Basis of `Atomic*`, may need retries; suffers from ABA.

**28. `synchronized` vs `ReentrantLock`?**
ReentrantLock adds `tryLock`, timeouts, interruptible locking, fairness, multiple `Condition`s, but you must `unlock()` in `finally`.

**29. What is a deadlock? How do you prevent/detect it?**
Circular wait among threads holding locks. Prevent: consistent lock ordering, `tryLock` with timeout, fewer/shorter locks. Detect: `jstack`, VisualVM, `ThreadMXBean`.

**30. Livelock vs starvation?**
Livelock: active but no progress. Starvation: thread never scheduled / never gets lock.

**31. What is `ThreadLocal`? Pitfalls?**
Per-thread variable copy. In pools, call `remove()` to avoid leaks/stale data.

**32. What is the Java Memory Model / happens-before?**
Rules defining when one thread's writes are visible to another (monitor unlock→lock, volatile write→read, `start()`, `join()`, etc.).

**33. Is `i++` atomic? Are reads/writes of `long`/`double` atomic?**
`i++` is not atomic. Plain `long`/`double` writes may be non-atomic on 32-bit JVMs (word tearing) unless declared `volatile`.

**34. What is the double-checked locking pattern and why `volatile`?**
Lazy singleton init with a 2-level check; `volatile` prevents another thread seeing a partially constructed object due to reordering.

**35. How can you make a class immutable/thread-safe?**
`final` class, `final` private fields, no setters, defensive copies of mutable inputs/outputs, safe construction (no `this` escape).

### Executors and utilities
**36. Why use thread pools?**
Reuse threads, bound resource usage, queue tasks, simpler lifecycle management.

**37. Explain `ThreadPoolExecutor` parameters and task flow.**
core → queue → max → reject (see file 04).

**38. `submit()` vs `execute()`?**
`submit` returns `Future` and captures exceptions; `execute` returns void.

**39. `shutdown()` vs `shutdownNow()`?**
`shutdown` lets queued tasks finish; `shutdownNow` interrupts workers and returns pending tasks.

**40. Why avoid `Executors.newFixedThreadPool/newCachedThreadPool` in production?**
Unbounded queue / unbounded threads can cause OOM; create `ThreadPoolExecutor` with bounded queue and explicit policy.

**41. `Future` vs `CompletableFuture`?**
`Future`: blocking `get`, no chaining. `CompletableFuture`: non-blocking callbacks, chaining, combining, error handling.

**42. `CountDownLatch` vs `CyclicBarrier` vs `Semaphore`?**
Latch: one-shot wait for events. Barrier: reusable meet-up of N threads. Semaphore: limit concurrent access with permits.

**43. How does `ConcurrentHashMap` work and how is it different from `Hashtable`?**
Fine-grained locking (per bin with CAS/synchronized in Java 8+), lock-free reads, no null keys/values, atomic compound ops (`merge`, `computeIfAbsent`). `Hashtable` locks the entire map.

**44. `CopyOnWriteArrayList` – when to use?**
Reads far outnumber writes; each write copies the array; snapshot iterators never throw `ConcurrentModificationException`.

**45. What is `BlockingQueue`?**
A queue whose `put` blocks when full and `take` blocks when empty – base for producer-consumer.

**46. What is the Fork/Join framework?**
Recursive divide-and-conquer on a work-stealing pool (`RecursiveTask`, `RecursiveAction`); used by parallel streams.

**47. What are virtual threads?**
Lightweight JVM-managed threads (Java 21) letting thread-per-request scale for blocking I/O. Avoid pooling and long pinned `synchronized` sections.

**48. How would you size a thread pool?**
CPU-bound ≈ cores; I/O-bound ≈ cores × (1 + wait/compute); then measure.

**49. What is thread safety? How do you achieve it?**
Correct behaviour under concurrent access. Achieve by confinement (no sharing, `ThreadLocal`), immutability, synchronization/locks, atomics, concurrent collections.

**50. Which is faster: `synchronized` or `ReentrantLock`?**
Modern JVMs optimise `synchronized` heavily (biased/thin locks, lock elision); differences are small. Pick by features needed, not by speed.

---

## Part B – Classic Coding Problems

### B1. Print odd and even numbers alternately with two threads

```java
// File: OddEvenPrinter.java
public class OddEvenPrinter {
    private final Object lock = new Object();
    private int current = 1;
    private final int max = 10;

    void print(boolean wantOdd) {
        synchronized (lock) {
            while (current <= max) {
                if ((current % 2 == 1) == wantOdd) {
                    System.out.println(Thread.currentThread().getName() + ": " + current++);
                    lock.notifyAll();
                } else {
                    try { lock.wait(); } catch (InterruptedException e) { return; }
                }
            }
            lock.notifyAll();
        }
    }

    public static void main(String[] args) throws Exception {
        OddEvenPrinter p = new OddEvenPrinter();
        Thread odd  = new Thread(() -> p.print(true),  "odd ");
        Thread even = new Thread(() -> p.print(false), "even");
        odd.start(); even.start(); odd.join(); even.join();
    }
}
```

### B2. Three threads printing A B C in order, repeated (using Semaphores)

```java
// File: PrintABC.java
import java.util.concurrent.Semaphore;

public class PrintABC {
    public static void main(String[] args) throws Exception {
        Semaphore a = new Semaphore(1), b = new Semaphore(0), c = new Semaphore(0);
        int rounds = 3;
        Thread ta = new Thread(() -> loop(a, b, "A", rounds));
        Thread tb = new Thread(() -> loop(b, c, "B", rounds));
        Thread tc = new Thread(() -> loop(c, a, "C", rounds));
        ta.start(); tb.start(); tc.start();
        ta.join(); tb.join(); tc.join();
        System.out.println();
    }
    static void loop(Semaphore mine, Semaphore next, String s, int rounds) {
        try {
            for (int i = 0; i < rounds; i++) {
                mine.acquire();
                System.out.print(s + " ");
                next.release();
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
```

### B3. Run T1, then T2, then T3 in sequence (using `join`)

```java
// File: SequentialThreads.java
public class SequentialThreads {
    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> System.out.println("T1"));
        Thread t2 = new Thread(() -> System.out.println("T2"));
        Thread t3 = new Thread(() -> System.out.println("T3"));
        t1.start(); t1.join();
        t2.start(); t2.join();
        t3.start(); t3.join();
    }
}
```

### B4. Thread-safe Singleton using the holder idiom (no locks, lazy)

```java
// File: SingletonHolder.java
public class SingletonHolder {
    private SingletonHolder() {}
    private static class Holder { static final SingletonHolder INSTANCE = new SingletonHolder(); }
    public static SingletonHolder getInstance() { return Holder.INSTANCE; }  // class-loading is thread-safe

    public static void main(String[] args) {
        System.out.println(getInstance() == getInstance());
    }
}
```

### B5. Simple custom thread pool (shows how a pool works inside)

```java
// File: MySimpleThreadPool.java
import java.util.concurrent.*;

public class MySimpleThreadPool {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final Thread[] workers;
    private volatile boolean shutdown = false;

    MySimpleThreadPool(int n) {
        workers = new Thread[n];
        for (int i = 0; i < n; i++) {
            workers[i] = new Thread(() -> {
                while (!shutdown || !queue.isEmpty()) {
                    try {
                        Runnable task = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (task != null) task.run();
                    } catch (InterruptedException e) { return; }
                }
            }, "worker-" + i);
            workers[i].start();
        }
    }
    void submit(Runnable r) {
        if (shutdown) throw new IllegalStateException("pool is shut down");
        queue.add(r);
    }
    void shutdown() throws InterruptedException {
        shutdown = true;
        for (Thread w : workers) w.join();
    }

    public static void main(String[] args) throws Exception {
        MySimpleThreadPool pool = new MySimpleThreadPool(2);
        for (int i = 1; i <= 5; i++) {
            int id = i;
            pool.submit(() -> System.out.println("job " + id + " by " + Thread.currentThread().getName()));
        }
        pool.shutdown();
    }
}
```

### B6. Parallel sum with threads (split-and-join)

```java
// File: ParallelSum.java
import java.util.*;
import java.util.concurrent.*;

public class ParallelSum {
    public static void main(String[] args) throws Exception {
        int[] data = new int[1_000_000];
        Arrays.fill(data, 1);
        int parts = 4, chunk = data.length / parts;

        ExecutorService ex = Executors.newFixedThreadPool(parts);
        List<Future<Long>> futures = new ArrayList<>();
        for (int p = 0; p < parts; p++) {
            int from = p * chunk, to = (p == parts - 1) ? data.length : from + chunk;
            futures.add(ex.submit(() -> {
                long s = 0; for (int i = from; i < to; i++) s += data[i]; return s;
            }));
        }
        long total = 0;
        for (Future<Long> f : futures) total += f.get();
        ex.shutdown();
        System.out.println("total = " + total);        // 1000000
    }
}
```

### B7. Common "find the bug" snippets

```java
// BUG 1: check-then-act race even though ConcurrentHashMap is thread-safe
if (!map.containsKey(k)) { map.put(k, v); }          // use map.putIfAbsent(k, v)

// BUG 2: lock not released on exception
lock.lock();
doWork();            // may throw
lock.unlock();       // never reached -> use try/finally

// BUG 3: if instead of while around wait()
synchronized (lock) { if (!ready) lock.wait(); }     // use while

// BUG 4: swallowing InterruptedException
catch (InterruptedException e) { }                    // restore: Thread.currentThread().interrupt()

// BUG 5: synchronizing on a changing/boxed/String-literal object
synchronized (count) { count++; }   // Integer count -> new object each time, no mutual exclusion

// BUG 6: calling run() instead of start()
new Thread(task).run();

// BUG 7: volatile counter
private volatile int count; void inc() { count++; } // still a race -> AtomicInteger
```

---

## Part C – Cheat-sheet

### Which tool when?

| Need | Use |
|---|---|
| Run a task in background | `ExecutorService.submit` |
| Task returns a value | `Callable` + `Future` / `CompletableFuture` |
| Mutual exclusion on a few lines | `synchronized` block |
| Timeout / interruptible / fair lock | `ReentrantLock` |
| Many readers, few writers | `ReentrantReadWriteLock` / `StampedLock` |
| Simple flag visible to all threads | `volatile` |
| Shared counter | `AtomicInteger` / `LongAdder` |
| Hand-off between producer & consumer | `BlockingQueue` |
| Wait for N tasks to finish | `CountDownLatch` / `invokeAll` / `CompletableFuture.allOf` |
| Threads meet at a point repeatedly | `CyclicBarrier` |
| Limit concurrency (pool of resources) | `Semaphore` |
| Concurrent map / list | `ConcurrentHashMap` / `CopyOnWriteArrayList` |
| Per-thread state | `ThreadLocal` |
| Divide & conquer CPU work | `ForkJoinPool` / parallel streams |
| Massive blocking I/O concurrency | Virtual threads (Java 21) |

### Methods at a glance

```
Thread     : start, run, sleep, join, yield, interrupt, isInterrupted, interrupted,
             isAlive, setDaemon, setPriority, setName, currentThread, getState
Object     : wait, wait(ms), notify, notifyAll          (inside synchronized only)
Lock       : lock, unlock, tryLock, lockInterruptibly, newCondition
Condition  : await, signal, signalAll
Executor   : execute, submit, invokeAll, invokeAny, shutdown, shutdownNow, awaitTermination
Future     : get, get(timeout), cancel, isDone, isCancelled
```

### Golden rules

1. Prefer **not sharing** state, then **immutability**, then synchronization.
2. Keep critical sections **small**; never call unknown/blocking code while holding a lock.
3. Acquire multiple locks in a **fixed order**.
4. Always `unlock()` in `finally`; always `wait()` in a `while`.
5. Never swallow `InterruptedException`.
6. Name your threads, bound your queues, shut down your pools.
7. Test with many iterations/threads – concurrency bugs are timing-dependent.

Good luck with your interviews! 🎯
