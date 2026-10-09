# Java Multithreading — Interview-Oriented Study Notes

These notes cover Java multithreading from the basics to interview-level concepts, with runnable code examples. Examples use Java 8-compatible syntax unless noted otherwise.

## 1. What is multithreading?

**Multithreading** means executing multiple threads concurrently within a single process. A thread is a lightweight unit of execution. Threads share process memory, while each thread has its own execution stack and program counter.

### Real-world example

A healthcare application processes patient records. Multiple workers can process separate records concurrently, potentially improving throughput when the workload can safely run in parallel. Actual performance depends on CPU cores, workload type, and synchronization overhead.

### Process vs thread

| Process | Thread |
|---|---|
| Independent executing program | Execution unit within a process |
| Has its own address space | Shares process memory with other threads |
| Generally more expensive to create | Generally cheaper to create |
| Communication often uses IPC | Communication can use shared objects |
| Failure is often more isolated | An uncaught exception can terminate its thread |

## 2. Ways to define and execute concurrent tasks

### Method 1: Extend `Thread`

```java
class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(
                Thread.currentThread().getName() + " : " + i
            );
        }
    }
}

public class Main {
    public static void main(String[] args) {
        MyThread t1 = new MyThread();
        t1.start();
    }
}
```

Call `start()` to start a new thread. Calling `run()` directly executes the method on the current thread.

### Method 2: Implement `Runnable`

Usually preferred because the task is separate from the thread and the class can still extend another class.

```java
class PatientTask implements Runnable {
    @Override
    public void run() {
        System.out.println(
            "Processing patient on: " +
            Thread.currentThread().getName()
        );
    }
}

public class Main {
    public static void main(String[] args) {
        Runnable task = new PatientTask();

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();
    }
}
```

Both threads run the same task object. Protect mutable shared state if the task uses it.

### Method 3: Lambda with `Runnable`

```java
public class Main {
    public static void main(String[] args) {
        Thread t1 = new Thread(() ->
            System.out.println("Task running")
        );

        t1.start();
    }
}
```

### Method 4: `Callable`, `Future`, and `ExecutorService`

`Callable<V>` can return a result and throw checked exceptions. It is submitted to an executor rather than passed directly to a `Thread`.

```java
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        ExecutorService executor =
            Executors.newSingleThreadExecutor();

        try {
            Callable<Integer> task = () -> 10 + 20;
            Future<Integer> future = executor.submit(task);

            System.out.println(future.get()); // 30
        } finally {
            executor.shutdown();
        }
    }
}
```

| Approach | Returns a result? | Typical use |
|---|---|---|
| Extend `Thread` | Not directly | Simple demos or specialized thread behavior |
| `Runnable` | No, not directly | Define a task independently of a thread |
| `Callable<V>` | Yes | Result-producing tasks |
| `ExecutorService` | Through `Future` or related APIs | Manage and reuse threads |

Modern Java applications commonly use executors, `Callable`, `CompletableFuture`, and concurrency utilities.

## 3. Thread lifecycle

The states in `Thread.State` are:

- **NEW** — created, but `start()` has not been called.
- **RUNNABLE** — ready to run or running on the CPU.
- **BLOCKED** — waiting to acquire a monitor lock.
- **WAITING** — waiting indefinitely for another thread or action.
- **TIMED_WAITING** — waiting for a specified duration.
- **TERMINATED** — execution has completed.

```java
Thread t1 = new Thread(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

System.out.println(t1.getState()); // NEW
t1.start();
System.out.println(t1.getState()); // Timing-dependent
t1.join();
System.out.println(t1.getState()); // TERMINATED
```

Do not assume the state immediately after `start()` will always be `RUNNABLE`; it depends on scheduling.

## 4. Important `Thread` methods

| Method | Purpose |
|---|---|
| `start()` | Starts a new thread of execution |
| `run()` | Contains the task logic |
| `sleep(ms)` | Pauses the current thread |
| `join()` | Waits for another thread to finish |
| `yield()` | Hints that the scheduler may run another thread |
| `interrupt()` | Requests interruption/cancellation |
| `isInterrupted()` | Checks interrupt status without clearing it |
| `interrupted()` | Checks and clears the current thread's interrupt status |
| `currentThread()` | Returns the currently executing thread |
| `getName()` / `setName()` | Gets or sets a thread's name |
| `getState()` | Returns the thread's state |
| `isAlive()` | Checks whether a thread has started and not terminated |
| `setDaemon(true)` | Marks a thread as daemon before it starts |

### `start()` vs `run()`

```java
Thread t = new Thread(() ->
    System.out.println(Thread.currentThread().getName())
);

t.run();   // Executes on the current thread, e.g. main
t.start(); // Starts a separate thread
```

Calling `start()` more than once on the same `Thread` instance throws `IllegalThreadStateException`.

### `sleep()` vs `join()`

```java
Thread t1 = new Thread(() -> {
    try {
        Thread.sleep(2000);
        System.out.println("Task completed");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

t1.start();
t1.join(); // main waits for t1 to terminate
System.out.println("Main continues");
```

- `sleep()` pauses the current thread and does **not** release monitor locks it holds.
- `join()` waits for the target thread to terminate.
- Both can throw `InterruptedException`.

### `interrupt()` — cooperative cancellation

`interrupt()` does not forcibly kill a thread. It sets an interrupt status or causes certain blocking operations to throw `InterruptedException`.

```java
public class Main {
    public static void main(String[] args)
            throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Working...");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                // sleep() cleared the interrupt status
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker stopping");
        });

        worker.start();
        Thread.sleep(1500);
        worker.interrupt();
        worker.join();
    }
}
```

Avoid deprecated `stop()`, `suspend()`, and `resume()`. Use cooperative cancellation.

## 5. Synchronization and race conditions

A race condition occurs when concurrent access to shared mutable state causes results to depend on timing. `count++` is a read-modify-write operation, not one atomic operation.

### Fix using `synchronized`

```java
class Counter {
    private int count = 0;

    synchronized void increment() {
        count++;
    }

    int getCount() {
        return count;
    }
}

public class Main {
    public static void main(String[] args)
            throws InterruptedException {
        Counter counter = new Counter();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter.increment();
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter.increment();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println(counter.getCount()); // 2000
    }
}
```

A synchronized instance method locks the current object. A static synchronized method locks the class monitor. A synchronized block lets you choose a lock object.

```java
class Example {
    void methodLevel() {
        synchronized (this) {
            System.out.println("Object-level lock");
        }
    }

    synchronized void instanceMethod() {
        System.out.println("Locks this object");
    }

    static synchronized void staticMethod() {
        System.out.println("Locks Example.class");
    }
}
```

Different instances have different object locks.

## 6. `volatile` vs `synchronized` vs `AtomicInteger`

| Feature | `volatile` | `synchronized` | `AtomicInteger` |
|---|---|---|---|
| Visibility | Yes | Yes, through monitor rules | Yes |
| Mutual exclusion | No | Yes | No general lock |
| Atomic increment | No | Yes, if consistently protected | Yes |
| Typical use | Status flags | Critical sections | Counters and atomic updates |

### `volatile` example

```java
class Worker {
    private volatile boolean running = true;

    void work() {
        while (running) {
            // Perform work
        }
    }

    void stop() {
        running = false;
    }
}
```

`volatile` provides visibility for the flag, but does not make compound operations such as `count++` atomic.

### `AtomicInteger` example

```java
import java.util.concurrent.atomic.AtomicInteger;

class Counter {
    private final AtomicInteger count = new AtomicInteger();

    void increment() {
        count.incrementAndGet();
    }

    int getCount() {
        return count.get();
    }
}
```

Use atomic classes for simple atomic updates. For related variables that must change together, use an appropriate locking or synchronization strategy.

## 7. `wait()`, `notify()`, and `notifyAll()`

These methods belong to `Object`, not `Thread`. Call them while holding the same object's monitor, normally inside a `synchronized` method or block.

- `wait()` releases that object's monitor and waits until notified, interrupted, or timed out.
- `notify()` wakes one thread waiting on that object's monitor.
- `notifyAll()` wakes all threads waiting on that object's monitor; they must compete to reacquire the lock.

### Producer-consumer example

```java
class SharedData {
    private boolean available = false;
    private int value;

    synchronized void produce(int v) throws InterruptedException {
        while (available) {
            wait();
        }

        value = v;
        available = true;
        System.out.println("Produced: " + value);
        notifyAll();
    }

    synchronized int consume() throws InterruptedException {
        while (!available) {
            wait();
        }

        int result = value;
        available = false;
        System.out.println("Consumed: " + result);
        notifyAll();
        return result;
    }
}

public class Main {
    public static void main(String[] args) {
        SharedData data = new SharedData();

        Thread producer = new Thread(() -> {
            try {
                data.produce(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                data.consume();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        producer.start();
    }
}
```

Use `while`, not `if`, to recheck the condition after waking. In production, a `BlockingQueue` is often simpler for producer-consumer workflows.

## 8. `ExecutorService` and thread pools

A thread pool reuses worker threads and reduces thread creation overhead.

```java
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        ExecutorService executor =
            Executors.newFixedThreadPool(3);

        try {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;

                executor.submit(() -> {
                    System.out.println(
                        "Task " + taskId + " running on " +
                        Thread.currentThread().getName()
                    );
                });
            }
        } finally {
            executor.shutdown();
        }
    }
}
```

A fixed pool of three workers executes at most three tasks simultaneously. Task ordering is not guaranteed.

### Important executor methods

| Method | Purpose |
|---|---|
| `execute(Runnable)` | Runs a task without returning a `Future` |
| `submit(Runnable)` | Submits a task and returns a `Future<?>` |
| `submit(Callable<T>)` | Submits a result-producing task |
| `shutdown()` | Rejects new tasks; submitted tasks can finish |
| `shutdownNow()` | Attempts to interrupt running tasks and returns tasks not started |
| `awaitTermination()` | Waits for executor termination up to a timeout |
| `invokeAll()` | Submits tasks and waits for all to complete |
| `invokeAny()` | Returns one successful result from submitted tasks |

`shutdownNow()` is best-effort. Tasks that ignore interruption may continue.

### Common pool factories

- `Executors.newFixedThreadPool(n)` — fixed worker count.
- `Executors.newSingleThreadExecutor()` — one worker executes tasks sequentially.
- `Executors.newCachedThreadPool()` — creates threads as needed and reuses idle ones; can grow substantially.
- `Executors.newScheduledThreadPool(n)` — delayed and periodic tasks.

For production workloads with strict resource limits, consider a `ThreadPoolExecutor` with a bounded queue and a deliberate rejection policy.

## 9. `Callable`, `Future`, and `CompletableFuture`

| Feature | `Runnable` | `Callable` | `Future` | `CompletableFuture` |
|---|---|---|---|---|
| Purpose | Define a task | Define a result-producing task | Represent an asynchronous result | Compose asynchronous stages |
| Returns a value | No | Yes | Retrieves a result | Yes, can transform results |
| Checked exceptions | Cannot declare from `run()` | Can throw from `call()` | `get()` can throw `ExecutionException` | Supports completion/exception stages |

### `CompletableFuture` example (Java 8+)

```java
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        CompletableFuture<Integer> future =
            CompletableFuture.supplyAsync(() -> 10)
                .thenApply(n -> n * 2)
                .thenApply(n -> n + 5);

        System.out.println(future.join()); // 25
    }
}
```

Important methods:

- `supplyAsync()` — asynchronous work returning a value.
- `runAsync()` — asynchronous work without a result.
- `thenApply()` — transforms a result.
- `thenCompose()` — chains another asynchronous operation.
- `thenCombine()` — combines two independent results.
- `exceptionally()` — recovers from an exception.
- `handle()` — processes success or failure.
- `allOf()` — waits for multiple futures to complete.
- `join()` — waits and wraps failures in an unchecked completion exception.

Async stages without an explicit executor typically use the common `ForkJoinPool`.

## 10. `ReentrantLock` and concurrency utilities

### `ReentrantLock`

```java
import java.util.concurrent.locks.ReentrantLock;

class SafeCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}
```

Always unlock in `finally`. `ReentrantLock` supports `tryLock()`, interruptible acquisition, and optional fairness.

### Important concurrency classes

| Class | Purpose |
|---|---|
| `ConcurrentHashMap` | Thread-safe concurrent map |
| `CopyOnWriteArrayList` | Read-heavy lists with infrequent writes |
| `BlockingQueue` | Producer-consumer handoff |
| `CountDownLatch` | Wait until a fixed count of events completes |
| `CyclicBarrier` | Let a group meet at a synchronization point |
| `Semaphore` | Limit concurrent access to a resource |
| `AtomicInteger` | Atomic integer updates |
| `ReadWriteLock` | Separate read and write locking |

### `CountDownLatch` example

```java
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 1; i <= 3; i++) {
            final int id = i;

            new Thread(() -> {
                try {
                    System.out.println("Task " + id);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("All tasks completed");
    }
}
```

The main thread waits until all three workers call `countDown()`.

## 11. Deadlock, starvation, livelock, and race conditions

- **Deadlock:** Threads wait indefinitely for locks held by one another.
- **Race condition:** Concurrent access to shared state produces timing-dependent results.
- **Starvation:** A thread repeatedly fails to obtain CPU time or required resources.
- **Livelock:** Threads remain active but make no meaningful progress.

### Deadlock example

```java
public class DeadlockDemo {
    public static void main(String[] args) {
        Object lockA = new Object();
        Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("T1 locked A");
                synchronized (lockB) {
                    System.out.println("T1 locked B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("T2 locked B");
                synchronized (lockA) {
                    System.out.println("T2 locked A");
                }
            }
        });

        t1.start();
        t2.start();
    }
}
```

This can deadlock if each thread acquires its first lock before the other does. Prevent it by acquiring locks in a consistent order, reducing nested locking, or using timed `tryLock()`.

## 12. Frequently asked interview questions

1. **Concurrency vs parallelism?** Concurrency means tasks make progress during overlapping periods. Parallelism means they execute at the same instant, typically on separate cores.
2. **Can we call `start()` twice?** No. It throws `IllegalThreadStateException`.
3. **Can we override `run()`?** Yes; it defines the task's code.
4. **Why prefer `Runnable` over extending `Thread`?** It separates task from thread and allows the class to extend another class.
5. **What is a daemon thread?** A background thread that does not prevent the JVM from exiting when only daemon threads remain. Set daemon status before starting.
6. **Does `sleep()` release a lock?** No.
7. **Does `wait()` release a lock?** It releases the monitor on the object on which it is called.
8. **`volatile` vs `synchronized`?** `volatile` gives visibility/order guarantees for a variable; `synchronized` also provides mutual exclusion.
9. **What is thread safety?** Correct behavior during concurrent use without unsafe caller coordination.
10. **`submit()` vs `execute()`?** `submit()` returns a `Future`; `execute()` does not.
11. **What is a `Future`?** A handle to an asynchronous result; `get()` waits for completion.
12. **`notify()` vs `notifyAll()`?** One waiter versus all waiters on the same monitor.
13. **How do you stop a running thread?** Cooperative cancellation, commonly with interruption or a cancellation flag.
14. **What is a thread pool?** Reusable workers that execute submitted tasks.
15. **`CountDownLatch` vs `CyclicBarrier`?** A latch waits for a count to reach zero and cannot be reset; a barrier coordinates a group and can be reused.
16. **`AtomicInteger` vs `synchronized`?** Atomic integer operations update one integer atomically; synchronization protects a critical section and potentially multiple variables.

## 13. Modern Java: virtual threads (Java 21+)

Virtual threads are lightweight JVM-managed threads designed to make large numbers of mostly blocking tasks easier to manage. They do not remove the need for synchronization and do not inherently speed up CPU-bound work.

```java
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread t = Thread.startVirtualThread(() ->
            System.out.println("Running in a virtual thread")
        );

        t.join();
    }
}
```

- Virtual threads were finalized in Java 21.
- They are useful for I/O-heavy workloads, such as many concurrent HTTP requests.
- `Executors.newVirtualThreadPerTaskExecutor()` is another way to create an executor that starts a virtual thread per task.

## 14. Suggested five-day study plan

- **Day 1 — Basics:** Thread vs process, `Thread`, `Runnable`, `start()` vs `run()`, lifecycle.
- **Day 2 — Coordination:** `sleep()`, `join()`, `interrupt()`, `wait()`/`notify()`, producer-consumer.
- **Day 3 — Thread safety:** Race conditions, `synchronized`, `volatile`, `AtomicInteger`, locks.
- **Day 4 — APIs:** `ExecutorService`, pools, `Callable`, `Future`, `CompletableFuture`, concurrent collections.
- **Day 5 — Scenarios:** Deadlock prevention, latches, semaphores, coding practice, virtual threads.

**Recommended first topics:** Master `Runnable`, `synchronized`, `volatile`, `wait/notify`, and `ExecutorService`. Then practise a shared counter, producer-consumer, and deadlock-prevention example from memory.
