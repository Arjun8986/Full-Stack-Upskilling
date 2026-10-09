# 02 – Thread Lifecycle & Important Methods

## 1. Thread lifecycle (states)

`Thread.State` enum has 6 values:

```
        start()               scheduler picks
 NEW ───────────► RUNNABLE ◄──────────────────► (running)
                    │  ▲
   waiting for lock │  │ lock acquired
                    ▼  │
                  BLOCKED
                    │
   wait() / join()  │  notify()/notifyAll()/target thread ends
   (no timeout)     ▼  │
                  WAITING
   sleep(ms) / wait(ms) / join(ms)
                  TIMED_WAITING ──(timeout)──► RUNNABLE
                    │
   run() completes / uncaught exception
                    ▼
               TERMINATED
```

| State | When |
|---|---|
| `NEW` | Thread object created, `start()` not yet called |
| `RUNNABLE` | Ready to run or running (Java doesn't distinguish "running") |
| `BLOCKED` | Waiting to **acquire a monitor lock** (`synchronized`) |
| `WAITING` | Waiting indefinitely: `Object.wait()`, `join()`, `LockSupport.park()` |
| `TIMED_WAITING` | Waiting with timeout: `sleep(ms)`, `wait(ms)`, `join(ms)` |
| `TERMINATED` | Finished execution |

### Demo: observing the states

```java
// File: LifecycleDemo.java
public class LifecycleDemo {
    public static void main(String[] args) throws Exception {
        Object lock = new Object();

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(300);                 // TIMED_WAITING
                synchronized (lock) {
                    lock.wait();                   // WAITING
                }
            } catch (InterruptedException ignored) { }
        });

        System.out.println("1. " + t.getState());  // NEW
        t.start();
        System.out.println("2. " + t.getState());  // RUNNABLE
        Thread.sleep(100);
        System.out.println("3. " + t.getState());  // TIMED_WAITING
        Thread.sleep(500);
        System.out.println("4. " + t.getState());  // WAITING
        synchronized (lock) { lock.notify(); }
        t.join();
        System.out.println("5. " + t.getState());  // TERMINATED
    }
}
```

### Demo: BLOCKED state

```java
// File: BlockedDemo.java
public class BlockedDemo {
    static final Object LOCK = new Object();

    public static void main(String[] args) throws Exception {
        Thread holder = new Thread(() -> {
            synchronized (LOCK) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        });
        Thread waiter = new Thread(() -> { synchronized (LOCK) { } });

        holder.start();
        Thread.sleep(100);              // let holder grab the lock
        waiter.start();
        Thread.sleep(100);
        System.out.println("waiter state = " + waiter.getState());  // BLOCKED
        holder.join();
        waiter.join();
    }
}
```

---

## 2. Methods of `Thread` class

### 2.1 Cheat-sheet table

| Method | Purpose |
|---|---|
| `start()` | Begin execution in a new thread (calls `run()`) |
| `run()` | The task body |
| `static sleep(ms)` | Pause **current** thread; **keeps locks** |
| `join()` / `join(ms)` | Wait for another thread to finish |
| `static yield()` | Hint that current thread is willing to give up CPU (no guarantee) |
| `interrupt()` | Set interrupt flag / wake from sleep, wait, join |
| `isInterrupted()` | Check flag, **does not clear** it |
| `static interrupted()` | Check **and clear** flag of current thread |
| `isAlive()` | `true` if started and not terminated |
| `setDaemon(boolean)` / `isDaemon()` | Daemon thread flag (call **before** `start()`) |
| `setPriority(int)` / `getPriority()` | 1 (MIN) – 5 (NORM) – 10 (MAX); only a hint |
| `setName` / `getName` | Thread name (useful in logs) |
| `static currentThread()` | Reference to the executing thread |
| `getId()` / `threadId()` | Unique id (`threadId()` from Java 19) |
| `getState()` | Returns `Thread.State` |
| `setUncaughtExceptionHandler` | Handle uncaught exceptions |
| `static onSpinWait()` | Hint inside busy-wait loops |

**Methods on `Object`** (used for inter-thread communication, must be called inside `synchronized`): `wait()`, `wait(ms)`, `notify()`, `notifyAll()`.

**Deprecated/never use:** `stop()`, `suspend()`, `resume()` – unsafe (can leave data inconsistent / cause deadlock).

### 2.2 `sleep()`

```java
// File: SleepDemo.java
public class SleepDemo {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 3; i > 0; i--) {
            System.out.println("Countdown " + i);
            Thread.sleep(500);       // throws InterruptedException (checked)
        }
        System.out.println("Go!");
    }
}
```

### 2.3 `join()`

```java
// File: JoinDemo.java
public class JoinDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            System.out.println("worker done");
        });
        worker.start();

        worker.join();                         // main waits here
        // worker.join(200);                   // wait max 200 ms
        System.out.println("main continues AFTER worker");
    }
}
```

Typical use: start several threads then `join()` each to wait for all results.

### 2.4 `yield()` and priorities

```java
// File: YieldPriorityDemo.java
public class YieldPriorityDemo {
    public static void main(String[] args) {
        Thread low  = new Thread(() -> System.out.println("low"));
        Thread high = new Thread(() -> System.out.println("high"));
        low.setPriority(Thread.MIN_PRIORITY);   // 1
        high.setPriority(Thread.MAX_PRIORITY);  // 10
        low.start();
        high.start();                           // may or may not run first!
        Thread.yield();                         // only a hint to the scheduler
    }
}
```

> Priorities and yield are **hints**; the OS scheduler may ignore them. Do not build logic on them.

### 2.5 Interrupting a thread (cooperative cancellation)

`interrupt()` doesn't kill a thread. It sets a flag; the thread must check it or be in a blocking call that throws `InterruptedException`.

```java
// File: InterruptDemo.java
public class InterruptDemo {
    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("working...");
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // sleep() CLEARS the flag when it throws -> restore it
                    Thread.currentThread().interrupt();
                    System.out.println("interrupted, cleaning up");
                }
            }
            System.out.println("exiting gracefully");
        });
        t.start();
        Thread.sleep(1000);
        t.interrupt();
        t.join();
    }
}
```

**Interview points**
* When `InterruptedException` is thrown, the interrupt flag is **cleared**. Either rethrow it or call `Thread.currentThread().interrupt()` to restore it. Never swallow it silently.
* `isInterrupted()` doesn't clear the flag; static `Thread.interrupted()` clears it.
* Another safe stop pattern: a `volatile boolean running` flag.

```java
// File: VolatileStopFlag.java
public class VolatileStopFlag {
    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            long n = 0;
            while (running) { n++; }
            System.out.println("worker stopped (iterations > 0: " + (n > 0) + ")");
        });
        t.start();
        Thread.sleep(200);
        running = false;       // visible to worker because it's volatile
        t.join();
    }
}
```

### 2.6 Daemon threads

Daemon threads are background service threads (e.g., GC). The **JVM exits when only daemon threads remain**.

```java
// File: DaemonDemo.java
public class DaemonDemo {
    public static void main(String[] args) throws Exception {
        Thread d = new Thread(() -> {
            while (true) {
                System.out.println("daemon heartbeat");
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        });
        d.setDaemon(true);        // MUST be before start(), else IllegalThreadStateException
        d.start();
        Thread.sleep(700);
        System.out.println("main ends -> JVM exits, daemon killed");
    }
}
```

Daemon status is inherited from the creating thread. Don't use daemon threads for work that must complete (file writes, DB transactions) – they can be cut mid-way.

### 2.7 Handling uncaught exceptions

```java
// File: UncaughtHandlerDemo.java
public class UncaughtHandlerDemo {
    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> { throw new IllegalStateException("boom"); }, "bad-thread");
        t.setUncaughtExceptionHandler((th, ex) ->
            System.out.println("Handler caught from " + th.getName() + ": " + ex));
        t.start();
        t.join();

        // Global fallback for all threads:
        // Thread.setDefaultUncaughtExceptionHandler(...)
    }
}
```

Exceptions thrown in one thread **cannot** be caught by a `try/catch` in another thread (e.g., main). With `ExecutorService.submit()`, the exception is captured inside the `Future` and thrown as `ExecutionException` on `get()`.

### 2.8 Thread naming & ids

```java
// File: NamingDemo.java
public class NamingDemo {
    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() ->
            System.out.println(Thread.currentThread().getName()
                + " id=" + Thread.currentThread().threadId()), "order-processor-1");
        t.start();
        t.join();
    }
}
```

Always name threads/pools – thread dumps and logs become readable.

---

## 3. `wait()` / `notify()` / `notifyAll()`

Used for **inter-thread communication**. Rules:

1. Must be called while holding the object's monitor (inside `synchronized`), otherwise `IllegalMonitorStateException`.
2. `wait()` **releases** the lock and waits; `notify()` wakes **one** waiting thread; `notifyAll()` wakes **all**.
3. Always call `wait()` in a **`while` loop** checking the condition (guards against *spurious wakeups* and stolen notifications).

```java
// File: WaitNotifyDemo.java
public class WaitNotifyDemo {
    private final Object lock = new Object();
    private boolean dataReady = false;

    void consumer() throws InterruptedException {
        synchronized (lock) {
            while (!dataReady) {           // loop, not if
                lock.wait();               // releases lock
            }
            System.out.println("Consumer got the data");
        }
    }

    void producer() {
        synchronized (lock) {
            dataReady = true;
            System.out.println("Producer: data ready");
            lock.notifyAll();
        }
    }

    public static void main(String[] args) throws Exception {
        WaitNotifyDemo d = new WaitNotifyDemo();
        Thread c = new Thread(() -> {
            try { d.consumer(); } catch (InterruptedException ignored) {}
        });
        c.start();
        Thread.sleep(300);
        d.producer();
        c.join();
    }
}
```

### `sleep()` vs `wait()` (asked in almost every interview)

| | `sleep()` | `wait()` |
|---|---|---|
| Defined in | `Thread` (static) | `Object` |
| Releases lock | **No** | **Yes** |
| Needs `synchronized` | No | **Yes** |
| Wake up by | Timeout / interrupt | `notify`/`notifyAll`/timeout/interrupt |
| Purpose | Pause execution | Inter-thread coordination |

### Why are `wait/notify` in `Object` and not `Thread`?
Because the lock (monitor) belongs to **objects**, and any object can be used as a lock. Threads wait *on an object's monitor*.

### `notify()` vs `notifyAll()`
`notify()` wakes an arbitrary single waiter (risk: wrong thread woken → lost signal). `notifyAll()` is the safe default when multiple conditions share one monitor.

---

## 4. `join()` vs `sleep()` vs `yield()` quick compare

| | Effect on caller | Releases locks? |
|---|---|---|
| `sleep(ms)` | Pause for a time | No |
| `join()` | Wait until other thread finishes | No (join uses wait internally, but you don't hold the target's lock) |
| `yield()` | Hint to let others run | No |

---

## 5. Thread groups, `ThreadLocal`, and others (brief)

* **ThreadGroup** – legacy way to group threads; rarely used now.
* **ThreadLocal** – covered in file `03`.
* **ThreadFactory** – customise naming/daemon flag of pool threads (file `04`).

➡ Next: [`03-synchronization-and-locks.md`](03-synchronization-and-locks.md)
