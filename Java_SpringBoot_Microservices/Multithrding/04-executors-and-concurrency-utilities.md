# 04 – Executor Framework & Concurrency Utilities (`java.util.concurrent`)

## 1. Why thread pools?

Creating a thread per task is expensive (memory ~1 MB stack each, OS scheduling cost) and unbounded thread creation can crash the JVM. A **thread pool** reuses a fixed set of threads and queues extra tasks.

Hierarchy: `Executor` → `ExecutorService` → `ScheduledExecutorService`; helper factory: `Executors`.

| Interface / class | Role |
|---|---|
| `Executor` | `execute(Runnable)` – just run a task |
| `ExecutorService` | Lifecycle (`shutdown`) + `submit` returning `Future` |
| `ScheduledExecutorService` | Delayed / periodic tasks |
| `ThreadPoolExecutor` | The real implementation behind most pools |
| `ForkJoinPool` | Work-stealing pool for divide-and-conquer |

---

## 2. Types of pools from `Executors`

| Factory | Behaviour | Use for | Watch out |
|---|---|---|---|
| `newFixedThreadPool(n)` | n threads, **unbounded** queue | Steady, CPU-ish workloads | Unbounded queue → OOM |
| `newCachedThreadPool()` | Creates threads as needed, reuses idle (60 s) | Many short tasks | Unbounded threads |
| `newSingleThreadExecutor()` | 1 thread, tasks run in order | Sequential processing | Single point of slowness |
| `newScheduledThreadPool(n)` | Delay / periodic | Timers, polling | – |
| `newWorkStealingPool()` | `ForkJoinPool`, parallelism = cores | Recursive/parallel tasks | – |
| `newVirtualThreadPerTaskExecutor()` | Virtual thread per task (Java 21) | Blocking I/O | Not for CPU-bound |

```java
// File: PoolTypesDemo.java
import java.util.concurrent.*;

public class PoolTypesDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService fixed = Executors.newFixedThreadPool(2);
        for (int i = 1; i <= 4; i++) {
            int id = i;
            fixed.submit(() -> System.out.println("fixed task " + id + " on "
                + Thread.currentThread().getName()));
        }
        fixed.shutdown();
        fixed.awaitTermination(2, TimeUnit.SECONDS);

        ExecutorService single = Executors.newSingleThreadExecutor();
        single.submit(() -> System.out.println("A"));
        single.submit(() -> System.out.println("B"));      // always after A
        single.shutdown();
    }
}
```

### Sizing a pool (interview favourite)

* **CPU-bound:** `threads ≈ number of cores (+1)`.
* **I/O-bound:** `threads ≈ cores × (1 + wait_time / compute_time)`.
* `Runtime.getRuntime().availableProcessors()` gives core count.

---

## 3. `ThreadPoolExecutor` – the 7 constructor parameters

```java
new ThreadPoolExecutor(
    corePoolSize,        // threads kept alive even when idle
    maximumPoolSize,     // upper limit of threads
    keepAliveTime, unit, // idle time before extra (non-core) threads die
    workQueue,           // holds tasks waiting to run
    threadFactory,       // how threads are created/named
    rejectedHandler);    // what to do when saturated
```

**Task submission flow** (very common question):
1. Fewer than `corePoolSize` threads → create a new thread.
2. Otherwise → put task in the **queue**.
3. Queue full and threads < `maximumPoolSize` → create extra thread.
4. Queue full and threads == max → **reject** (rejection policy).

| Rejection policy | Behaviour |
|---|---|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` |
| `CallerRunsPolicy` | Caller thread runs the task (natural back-pressure) |
| `DiscardPolicy` | Silently drops the task |
| `DiscardOldestPolicy` | Drops oldest queued task, retries |

```java
// File: CustomPoolDemo.java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomPoolDemo {
    public static void main(String[] args) throws Exception {
        AtomicInteger n = new AtomicInteger();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "my-pool-" + n.incrementAndGet());
            t.setDaemon(false);
            return t;
        };

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
            2, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2),                  // BOUNDED queue
            tf,
            new ThreadPoolExecutor.CallerRunsPolicy());

        for (int i = 1; i <= 10; i++) {
            int id = i;
            pool.execute(() -> {
                System.out.println("task " + id + " -> " + Thread.currentThread().getName());
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("largest pool size = " + pool.getLargestPoolSize());
    }
}
```

> Production tip: prefer constructing `ThreadPoolExecutor` directly with a **bounded queue** and explicit rejection policy rather than `Executors.newFixedThreadPool`/`newCachedThreadPool` (unbounded risk – flagged by Alibaba / Sonar rules).

---

## 4. `execute()` vs `submit()`, `Future`

| | `execute(Runnable)` | `submit(Runnable/Callable)` |
|---|---|---|
| Returns | `void` | `Future<T>` |
| Exceptions | Go to thread's uncaught handler | **Captured in Future** (thrown on `get()`) |
| Defined in | `Executor` | `ExecutorService` |

```java
// File: FutureDemo.java
import java.util.*;
import java.util.concurrent.*;

public class FutureDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(3);

        // Callable returns a value
        Future<Integer> f = ex.submit(() -> { Thread.sleep(300); return 42; });
        System.out.println("done yet? " + f.isDone());
        System.out.println("result = " + f.get());                    // blocks
        System.out.println("done yet? " + f.isDone());

        // Exception handling
        Future<Integer> bad = ex.submit(() -> { throw new IllegalStateException("oops"); });
        try { bad.get(); }
        catch (ExecutionException e) { System.out.println("cause: " + e.getCause()); }

        // Timeout & cancel
        Future<?> slow = ex.submit(() -> { try { Thread.sleep(5000); } catch (InterruptedException e) { System.out.println("cancelled"); } });
        try { slow.get(200, TimeUnit.MILLISECONDS); }
        catch (TimeoutException e) { slow.cancel(true); }             // true => interrupt

        // invokeAll: run many, wait for all
        List<Callable<String>> jobs = List.of(() -> "A", () -> "B", () -> "C");
        for (Future<String> r : ex.invokeAll(jobs)) System.out.println(r.get());

        // invokeAny: first successful result
        System.out.println("fastest = " + ex.invokeAny(jobs));

        ex.shutdown();
    }
}
```

`Future` methods: `get()`, `get(timeout, unit)`, `isDone()`, `isCancelled()`, `cancel(boolean)`.
**Limitation:** `get()` is blocking and `Future` can't be chained → use `CompletableFuture`.

### Shutting down correctly

| Method | Effect |
|---|---|
| `shutdown()` | No new tasks; already submitted tasks complete |
| `shutdownNow()` | Tries to stop running tasks (interrupts), returns list of queued tasks never run |
| `awaitTermination(t, unit)` | Block until all finished or timeout |
| `isShutdown()` / `isTerminated()` | Status checks |

```java
// File: GracefulShutdown.java
import java.util.concurrent.*;

public class GracefulShutdown {
    static void shutdownAndAwait(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> System.out.println("work"));
        shutdownAndAwait(pool);
    }
}
```

> A forgotten non-daemon pool keeps the JVM from exiting. Since Java 19 `ExecutorService` is `AutoCloseable` (use try-with-resources).

---

## 5. `ScheduledExecutorService`

```java
// File: ScheduledDemo.java
import java.util.concurrent.*;

public class ScheduledDemo {
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(1);

        s.schedule(() -> System.out.println("once after 500ms"), 500, TimeUnit.MILLISECONDS);

        // fixed RATE: start-to-start every 400ms
        ScheduledFuture<?> rate = s.scheduleAtFixedRate(
            () -> System.out.println("rate  tick"), 0, 400, TimeUnit.MILLISECONDS);

        // fixed DELAY: end-to-start 400ms
        s.scheduleWithFixedDelay(
            () -> System.out.println("delay tick"), 0, 400, TimeUnit.MILLISECONDS);

        Thread.sleep(1300);
        rate.cancel(false);
        s.shutdownNow();
    }
}
```

`scheduleAtFixedRate` → fixed period between **starts**; `scheduleWithFixedDelay` → fixed gap between **end of one and start of next**. If a task throws, subsequent runs are suppressed – catch exceptions inside.

---

## 6. `CompletableFuture` – async pipelines

```java
// File: CompletableFutureDemo.java
import java.util.concurrent.*;

public class CompletableFutureDemo {
    static String fetchUser()  { sleep(300); return "alice"; }
    static String fetchOrders(){ sleep(400); return "3 orders"; }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // run two calls in PARALLEL and combine
        CompletableFuture<String> user   = CompletableFuture.supplyAsync(CompletableFutureDemo::fetchUser, pool);
        CompletableFuture<String> orders = CompletableFuture.supplyAsync(CompletableFutureDemo::fetchOrders, pool);
        System.out.println(user.thenCombine(orders, (u, o) -> u + " has " + o).get());

        // chain + error handling
        CompletableFuture<Integer> cf = CompletableFuture
            .supplyAsync(() -> 10 / 0, pool)
            .exceptionally(ex -> { System.out.println("recovered from " + ex.getClass().getSimpleName()); return -1; });
        System.out.println(cf.get());

        // wait for all / any
        CompletableFuture<Void> all = CompletableFuture.allOf(user, orders);
        all.join();
        System.out.println("all done");

        // thenCompose = flatMap (dependent async calls)
        CompletableFuture<String> composed = CompletableFuture
            .supplyAsync(() -> "id-7", pool)
            .thenCompose(id -> CompletableFuture.supplyAsync(() -> "details of " + id, pool));
        System.out.println(composed.join());

        pool.shutdown();
    }
}
```

| Method | Purpose |
|---|---|
| `supplyAsync` / `runAsync` | Start async task (with / without result) |
| `thenApply` | Transform result (map) |
| `thenAccept` / `thenRun` | Consume result / just run |
| `thenCompose` | Chain another async call (flatMap) |
| `thenCombine` | Combine two independent futures |
| `exceptionally` / `handle` / `whenComplete` | Error handling |
| `allOf` / `anyOf` | Wait for all / first |
| `join()` vs `get()` | `join` throws unchecked `CompletionException` |

Suffix `Async` (e.g., `thenApplyAsync`) runs the step on another pool thread. Without an executor argument, default is `ForkJoinPool.commonPool()` – pass your own pool for blocking work.

---

## 7. Synchronizers (coordination utilities)

### 7.1 `CountDownLatch` – wait until N events happened (one-shot)

```java
// File: CountDownLatchDemo.java
import java.util.concurrent.*;

public class CountDownLatchDemo {
    public static void main(String[] args) throws Exception {
        int workers = 3;
        CountDownLatch done = new CountDownLatch(workers);
        for (int i = 1; i <= workers; i++) {
            int id = i;
            new Thread(() -> {
                System.out.println("worker " + id + " finished");
                done.countDown();
            }).start();
        }
        done.await();                          // main waits for all 3
        System.out.println("all workers done, main proceeds");
    }
}
```

### 7.2 `CyclicBarrier` – N threads wait for each other (reusable)

```java
// File: CyclicBarrierDemo.java
import java.util.concurrent.*;

public class CyclicBarrierDemo {
    public static void main(String[] args) {
        CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("--- all arrived, go! ---"));
        for (int i = 1; i <= 3; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep(id * 200L);
                    System.out.println("thread " + id + " waiting at barrier");
                    barrier.await();
                    System.out.println("thread " + id + " crossed");
                } catch (Exception ignored) {}
            }).start();
        }
    }
}
```

| | `CountDownLatch` | `CyclicBarrier` |
|---|---|---|
| Reusable | No | **Yes** |
| Who waits | Usually one thread waits for others | All parties wait for each other |
| Counting | `countDown()` by anyone | `await()` by each party |

### 7.3 `Semaphore` – limit concurrent access to a resource

```java
// File: SemaphoreDemo.java
import java.util.concurrent.*;

public class SemaphoreDemo {
    public static void main(String[] args) {
        Semaphore permits = new Semaphore(2);        // max 2 at a time (e.g., DB connections)
        for (int i = 1; i <= 5; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    permits.acquire();
                    System.out.println("T" + id + " entered");
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                } finally {
                    System.out.println("T" + id + " leaving");
                    permits.release();
                }
            }).start();
        }
    }
}
```

`Semaphore(1)` behaves like a (non-reentrant) mutex. Others: `Phaser` (flexible multi-phase barrier), `Exchanger` (two threads swap objects).

---

## 8. Concurrent collections

| Collection | Notes |
|---|---|
| `ConcurrentHashMap` | Segment/bucket-level locking + CAS; no `null` keys/values; atomic `putIfAbsent`, `compute`, `merge` |
| `CopyOnWriteArrayList` / `Set` | Copies array on every write – great for read-heavy, rarely-changing lists; iterators never throw CME |
| `ConcurrentLinkedQueue` / `Deque` | Lock-free non-blocking queues |
| `BlockingQueue`: `ArrayBlockingQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue`, `SynchronousQueue` | Producer-consumer building block |
| `ConcurrentSkipListMap` | Sorted concurrent map |

```java
// File: ConcurrentMapDemo.java
import java.util.concurrent.*;

public class ConcurrentMapDemo {
    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
        Runnable r = () -> {
            for (int i = 0; i < 10_000; i++)
                wordCount.merge("java", 1, Integer::sum);     // atomic read-modify-write
        };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println(wordCount.get("java"));            // 20000

        // WRONG (check-then-act is not atomic even on ConcurrentHashMap):
        // if (!map.containsKey(k)) map.put(k, v);   -> use putIfAbsent / computeIfAbsent
        wordCount.computeIfAbsent("spring", k -> 1);
    }
}
```

### `HashMap` vs `Hashtable` vs `ConcurrentHashMap` vs `synchronizedMap`

| | Thread-safe | Locking | Null key/value | Performance |
|---|---|---|---|---|
| `HashMap` | No | – | 1 null key, null values | Fastest (single thread) |
| `Hashtable` | Yes | Whole-map lock | No nulls | Poor |
| `Collections.synchronizedMap` | Yes | Whole-map lock | Depends | Poor; must lock manually while iterating |
| `ConcurrentHashMap` | Yes | Fine-grained (bucket CAS/synchronized) | **No nulls** | Best for concurrency |

---

## 9. Fork/Join & parallel streams

Divide a task recursively, run pieces on a **work-stealing** pool (idle threads steal tasks from busy threads' queues).

```java
// File: ForkJoinSumDemo.java
import java.util.concurrent.*;
import java.util.stream.LongStream;

public class ForkJoinSumDemo {
    static class SumTask extends RecursiveTask<Long> {
        final long[] arr; final int lo, hi;
        SumTask(long[] arr, int lo, int hi) { this.arr = arr; this.lo = lo; this.hi = hi; }

        @Override protected Long compute() {
            if (hi - lo <= 10_000) {                       // small enough: compute directly
                long s = 0; for (int i = lo; i < hi; i++) s += arr[i]; return s;
            }
            int mid = (lo + hi) / 2;
            SumTask left = new SumTask(arr, lo, mid), right = new SumTask(arr, mid, hi);
            left.fork();                                   // async
            long r = right.compute();                      // reuse this thread
            return left.join() + r;
        }
    }

    public static void main(String[] args) {
        long[] data = LongStream.rangeClosed(1, 1_000_000).toArray();
        long sum = ForkJoinPool.commonPool().invoke(new SumTask(data, 0, data.length));
        System.out.println(sum);                           // 500000500000

        // Parallel stream does the same for you
        System.out.println(LongStream.rangeClosed(1, 1_000_000).parallel().sum());
    }
}
```

Parallel streams use the **common ForkJoinPool** – avoid blocking calls or shared mutable state inside them.

---

## 10. Virtual threads (Java 21) – when and why

* Platform thread = 1:1 with an OS thread (heavy, thousands max).
* **Virtual thread** = JVM-scheduled, mounted on a few carrier threads; blocking I/O **unmounts** it, so you can run **100 000s** of them.
* Programming model unchanged – write simple blocking code.
* Don't pool them; create one per task.
* Caution: long `synchronized` blocks around blocking calls can **pin** the carrier (prefer `ReentrantLock`); virtual threads give no speed-up for CPU-bound work.

```java
// File: VirtualThreadsMany.java
import java.time.Duration;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class VirtualThreadsMany {
    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 10_000).forEach(i ->
                ex.submit(() -> { Thread.sleep(Duration.ofMillis(200)); return i; }));
        }
        System.out.println("10,000 blocking tasks in ~" + (System.currentTimeMillis() - t0) + " ms");
    }
}
```

---

## 11. Key takeaways

* Use **pools**, size them deliberately, prefer **bounded queues** + a rejection policy.
* `submit()` hides exceptions inside the `Future` – always call `get()` (or use `CompletableFuture.exceptionally`).
* Always **shut down** executors.
* Choose the right synchronizer: **Latch** (one-shot wait), **Barrier** (meet-up), **Semaphore** (limit access).
* Prefer `ConcurrentHashMap` + atomic methods (`merge`, `computeIfAbsent`) to manual locking.

➡ Next: [`05-interview-questions-and-cheatsheet.md`](05-interview-questions-and-cheatsheet.md)
