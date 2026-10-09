# 03 – Synchronization, Locks & Thread Safety

## 1. The problem: race condition

When two threads read–modify–write shared data without coordination, updates get lost.

```java
// File: RaceConditionDemo.java
public class RaceConditionDemo {
    static int counter = 0;

    public static void main(String[] args) throws Exception {
        Runnable inc = () -> { for (int i = 0; i < 100_000; i++) counter++; };
        Thread a = new Thread(inc), b = new Thread(inc);
        a.start(); b.start();
        a.join();  b.join();
        System.out.println("Expected 200000, got " + counter);   // usually less!
    }
}
```

`counter++` is **not atomic**: it is *read → add → write* (3 steps), and threads interleave between them.

Three things to understand: **atomicity**, **visibility**, **ordering**.

| Concept | Meaning | Fixed by |
|---|---|---|
| Atomicity | An operation happens fully or not at all | `synchronized`, locks, `Atomic*` |
| Visibility | A write by one thread is seen by others | `volatile`, `synchronized`, locks |
| Ordering | Compiler/CPU may reorder instructions | `volatile`, `synchronized`, happens-before rules |

---

## 2. `synchronized`

Every Java object has an **intrinsic lock (monitor)**. Only one thread can hold it at a time.

### 2.1 Synchronized instance method – locks on `this`

```java
// File: SyncMethodDemo.java
class Counter {
    private int count = 0;
    public synchronized void increment() { count++; }
    public synchronized int get() { return count; }
}

public class SyncMethodDemo {
    public static void main(String[] args) throws Exception {
        Counter c = new Counter();
        Runnable r = () -> { for (int i = 0; i < 100_000; i++) c.increment(); };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println(c.get());   // always 200000
    }
}
```

### 2.2 Synchronized block – smaller critical section (preferred)

```java
// File: SyncBlockDemo.java
public class SyncBlockDemo {
    private final Object lock = new Object();   // private, final lock object
    private int count;

    public void increment() {
        // non-critical work can happen here without holding the lock
        synchronized (lock) {
            count++;
        }
    }
    public static void main(String[] args) throws Exception {
        SyncBlockDemo d = new SyncBlockDemo();
        Thread a = new Thread(() -> { for (int i = 0; i < 50_000; i++) d.increment(); });
        Thread b = new Thread(() -> { for (int i = 0; i < 50_000; i++) d.increment(); });
        a.start(); b.start(); a.join(); b.join();
        System.out.println(d.count);   // 100000
    }
}
```

### 2.3 Static synchronized – locks on the `Class` object

```java
// File: StaticSyncDemo.java
class Registry {
    private static int total;
    public static synchronized void add() { total++; }       // lock = Registry.class
    public static int total() { synchronized (Registry.class) { return total; } }
}
public class StaticSyncDemo {
    public static void main(String[] args) throws Exception {
        Runnable r = () -> { for (int i = 0; i < 50_000; i++) Registry.add(); };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println(Registry.total());
    }
}
```

**Instance lock and class lock are different locks** – a thread in a static synchronized method does not block a thread in an instance synchronized method.

### 2.4 Key facts for interviews

* `synchronized` is **reentrant**: a thread holding a lock can enter another synchronized block on the same lock.
* Gives **mutual exclusion + visibility** (release → acquire creates a happens-before edge).
* Lock is released automatically on normal exit **or exception**.
* Cannot be used on variables or constructors (constructors can't be `synchronized`).
* No fairness, no timeout, can't be interrupted while waiting → use `ReentrantLock` if you need those.
* Don't synchronize on: a `String` literal, boxed `Integer`, `this` exposed publicly, or a lock object that changes.

```java
// File: ReentrantSyncDemo.java
public class ReentrantSyncDemo {
    synchronized void outer() { System.out.println("outer"); inner(); }
    synchronized void inner() { System.out.println("inner (same lock, re-entered)"); }
    public static void main(String[] args) { new ReentrantSyncDemo().outer(); }
}
```

---

## 3. `volatile`

Guarantees **visibility** and **no reordering** around the variable. Does **NOT** guarantee atomicity.

```java
// File: VolatileVisibility.java
public class VolatileVisibility {
    private static volatile boolean ready = false;   // remove volatile => may loop forever
    private static int number = 0;

    public static void main(String[] args) throws Exception {
        Thread reader = new Thread(() -> {
            while (!ready) { Thread.onSpinWait(); }
            System.out.println("number = " + number);   // 42 guaranteed
        });
        reader.start();
        number = 42;
        ready = true;            // write to volatile publishes 'number' too
        reader.join();
    }
}
```

`volatile` is right for: status flags, one-writer/many-reader values, safe publication of immutable objects, double-checked locking.
`volatile` is **wrong** for: `count++` (read-modify-write).

### Singleton with double-checked locking (needs `volatile`)

```java
// File: SingletonDCL.java
public class SingletonDCL {
    private static volatile SingletonDCL instance;
    private SingletonDCL() {}

    public static SingletonDCL getInstance() {
        if (instance == null) {                      // 1st check (no lock)
            synchronized (SingletonDCL.class) {
                if (instance == null) {              // 2nd check
                    instance = new SingletonDCL();   // volatile prevents half-built publication
                }
            }
        }
        return instance;
    }
    public static void main(String[] args) {
        System.out.println(getInstance() == getInstance());   // true
    }
}
```

### `volatile` vs `synchronized`

| | `volatile` | `synchronized` |
|---|---|---|
| Visibility | Yes | Yes |
| Atomicity of compound ops | **No** | Yes |
| Blocking | No | Yes |
| Applies to | Variables | Methods / blocks |

---

## 4. Atomic classes (`java.util.concurrent.atomic`)

Lock-free thread safety using CPU **CAS (compare-and-swap)**.

```java
// File: AtomicDemo.java
import java.util.concurrent.atomic.*;

public class AtomicDemo {
    public static void main(String[] args) throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Runnable r = () -> { for (int i = 0; i < 100_000; i++) counter.incrementAndGet(); };
        Thread a = new Thread(r), b = new Thread(r);
        a.start(); b.start(); a.join(); b.join();
        System.out.println(counter.get());            // 200000

        // CAS in action
        AtomicInteger x = new AtomicInteger(5);
        System.out.println(x.compareAndSet(5, 10));   // true  -> x = 10
        System.out.println(x.compareAndSet(5, 20));   // false -> still 10
        System.out.println(x.updateAndGet(v -> v * 2)); // 20

        // For heavy contention counters prefer LongAdder
        LongAdder adder = new LongAdder();
        adder.increment();
        System.out.println(adder.sum());
    }
}
```

Other classes: `AtomicLong`, `AtomicBoolean`, `AtomicReference<V>`, `LongAdder`, `AtomicIntegerArray`.
**ABA problem:** value changes A→B→A so CAS can't notice; use `AtomicStampedReference`.

---

## 5. `ReentrantLock` and `Lock` interface

More flexible than `synchronized`.

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Automatic release | Yes | **No – must `unlock()` in `finally`** |
| `tryLock()` / timeout | No | Yes |
| Interruptible wait | No | `lockInterruptibly()` |
| Fairness option | No | `new ReentrantLock(true)` |
| Multiple conditions | One wait-set | Many `Condition`s |

```java
// File: ReentrantLockDemo.java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock();
    private int balance = 100;

    void withdraw(int amt) {
        lock.lock();
        try {                                  // ALWAYS try/finally
            if (balance >= amt) balance -= amt;
        } finally {
            lock.unlock();
        }
    }

    boolean tryWithdraw(int amt) throws InterruptedException {
        if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {   // don't wait forever
            try {
                if (balance >= amt) { balance -= amt; return true; }
                return false;
            } finally { lock.unlock(); }
        }
        return false;     // could not get lock in time
    }

    public static void main(String[] args) throws Exception {
        ReentrantLockDemo d = new ReentrantLockDemo();
        d.withdraw(30);
        System.out.println(d.tryWithdraw(50) + " balance=" + d.balance);
        System.out.println("held by me? " + d.lock.isHeldByCurrentThread());
    }
}
```

Useful methods: `lock()`, `unlock()`, `tryLock()`, `tryLock(time, unit)`, `lockInterruptibly()`, `isLocked()`, `isHeldByCurrentThread()`, `getHoldCount()`, `newCondition()`.

### 5.1 `Condition` (replacement for wait/notify)

```java
// File: BoundedBufferCondition.java
import java.util.*;
import java.util.concurrent.locks.*;

public class BoundedBufferCondition<T> {
    private final Queue<T> q = new LinkedList<>();
    private final int cap;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    BoundedBufferCondition(int cap) { this.cap = cap; }

    void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (q.size() == cap) notFull.await();
            q.add(item);
            notEmpty.signal();
        } finally { lock.unlock(); }
    }

    T take() throws InterruptedException {
        lock.lock();
        try {
            while (q.isEmpty()) notEmpty.await();
            T item = q.poll();
            notFull.signal();
            return item;
        } finally { lock.unlock(); }
    }

    public static void main(String[] args) throws Exception {
        BoundedBufferCondition<Integer> buf = new BoundedBufferCondition<>(2);
        Thread prod = new Thread(() -> {
            try { for (int i = 1; i <= 5; i++) { buf.put(i); System.out.println("put " + i); } }
            catch (InterruptedException ignored) {}
        });
        Thread cons = new Thread(() -> {
            try { for (int i = 1; i <= 5; i++) { System.out.println("took " + buf.take()); Thread.sleep(100);} }
            catch (InterruptedException ignored) {}
        });
        prod.start(); cons.start(); prod.join(); cons.join();
    }
}
```

### 5.2 `ReadWriteLock` – many readers OR one writer

```java
// File: ReadWriteLockDemo.java
import java.util.*;
import java.util.concurrent.locks.*;

public class ReadWriteLockDemo {
    private final Map<String, String> cache = new HashMap<>();
    private final ReadWriteLock rw = new ReentrantReadWriteLock();

    String get(String k) {
        rw.readLock().lock();            // many threads can read together
        try { return cache.get(k); } finally { rw.readLock().unlock(); }
    }
    void put(String k, String v) {
        rw.writeLock().lock();           // exclusive
        try { cache.put(k, v); } finally { rw.writeLock().unlock(); }
    }
    public static void main(String[] args) {
        ReadWriteLockDemo c = new ReadWriteLockDemo();
        c.put("a", "1");
        System.out.println(c.get("a"));
    }
}
```

Java also has `StampedLock` (optimistic reads, not reentrant) for read-heavy, performance-critical code.

---

## 6. Producer–Consumer

### 6.1 Using `BlockingQueue` (simplest and recommended)

```java
// File: ProducerConsumerBQ.java
import java.util.concurrent.*;

public class ProducerConsumerBQ {
    public static void main(String[] args) throws Exception {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
        final int POISON = -1;

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    queue.put(i);                    // blocks when full
                    System.out.println("Produced " + i);
                }
                queue.put(POISON);                   // tell consumer to stop
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int v = queue.take();            // blocks when empty
                    if (v == POISON) break;
                    System.out.println("   Consumed " + v);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start(); consumer.start();
        producer.join();  consumer.join();
    }
}
```

### 6.2 Using `wait()/notify()`

```java
// File: ProducerConsumerWaitNotify.java
import java.util.*;

public class ProducerConsumerWaitNotify {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 2;

    public synchronized void produce(int v) throws InterruptedException {
        while (queue.size() == CAPACITY) wait();
        queue.add(v);
        System.out.println("Produced " + v);
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while (queue.isEmpty()) wait();
        int v = queue.poll();
        System.out.println("   Consumed " + v);
        notifyAll();
        return v;
    }

    public static void main(String[] args) throws Exception {
        ProducerConsumerWaitNotify pc = new ProducerConsumerWaitNotify();
        Thread p = new Thread(() -> {
            try { for (int i = 1; i <= 5; i++) pc.produce(i); } catch (InterruptedException ignored) {}
        });
        Thread c = new Thread(() -> {
            try { for (int i = 1; i <= 5; i++) pc.consume(); } catch (InterruptedException ignored) {}
        });
        p.start(); c.start(); p.join(); c.join();
    }
}
```

---

## 7. Deadlock, livelock, starvation

### 7.1 Deadlock – threads wait for each other forever

Four **necessary conditions** (Coffman): mutual exclusion, hold & wait, no pre-emption, circular wait. Break any one to prevent deadlock.

```java
// File: DeadlockDemo.java
public class DeadlockDemo {
    static final Object A = new Object(), B = new Object();

    public static void main(String[] args) throws Exception {
        Thread t1 = new Thread(() -> {
            synchronized (A) {
                sleep(100);
                synchronized (B) { System.out.println("t1 got both"); }
            }
        }, "T1");
        Thread t2 = new Thread(() -> {
            synchronized (B) {                  // opposite order -> deadlock
                sleep(100);
                synchronized (A) { System.out.println("t2 got both"); }
            }
        }, "T2");
        t1.setDaemon(true); t2.setDaemon(true);   // so this demo can exit
        t1.start(); t2.start();
        t1.join(1000); t2.join(1000);
        System.out.println("T1=" + t1.getState() + " T2=" + t2.getState()); // BLOCKED BLOCKED
    }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
```

**How to prevent**
1. **Lock ordering** – always acquire locks in the same global order.
2. Use `tryLock(timeout)` and back off.
3. Hold fewer locks / keep critical sections small.
4. Prefer higher-level utilities (concurrent collections, queues).

**How to detect:** `jstack <pid>` (prints "Found one Java-level deadlock"), VisualVM, or `ThreadMXBean.findDeadlockedThreads()`.

```java
// File: DeadlockFix.java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class DeadlockFix {
    static final ReentrantLock L1 = new ReentrantLock(), L2 = new ReentrantLock();

    static void transfer(String who, ReentrantLock first, ReentrantLock second) throws InterruptedException {
        while (true) {
            if (first.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    if (second.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try { System.out.println(who + " got both"); return; }
                        finally { second.unlock(); }
                    }
                } finally { first.unlock(); }
            }
            Thread.sleep((long) (Math.random() * 20));   // back off, then retry
        }
    }
    public static void main(String[] args) throws Exception {
        Thread a = new Thread(() -> { try { transfer("A", L1, L2); } catch (InterruptedException ignored) {} });
        Thread b = new Thread(() -> { try { transfer("B", L2, L1); } catch (InterruptedException ignored) {} });
        a.start(); b.start(); a.join(); b.join();
    }
}
```

### 7.2 Livelock & starvation

| Problem | Description |
|---|---|
| **Livelock** | Threads keep reacting to each other and change state but make no progress (two people stepping aside in a corridor). Fix: randomised back-off. |
| **Starvation** | A thread never gets CPU/lock because others always win (e.g., low priority, unfair lock). Fix: fair locks (`new ReentrantLock(true)`), avoid long lock holds. |

---

## 8. `ThreadLocal`

Gives each thread its **own copy** of a variable – no sharing, no locking.

```java
// File: ThreadLocalDemo.java
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreadLocalDemo {
    // SimpleDateFormat is NOT thread-safe -> give each thread its own instance
    private static final ThreadLocal<SimpleDateFormat> FMT =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public static void main(String[] args) throws Exception {
        Runnable r = () -> System.out.println(
            Thread.currentThread().getName() + " -> " + FMT.get().format(new Date()));
        Thread a = new Thread(r, "A"), b = new Thread(r, "B");
        a.start(); b.start(); a.join(); b.join();
    }
}
```

Use cases: per-request user/transaction context, non-thread-safe helpers. **Pitfall:** in thread pools, threads are reused – always call `remove()` in `finally` or you leak memory / leak data between requests.

---

## 9. Thread-safe vs not thread-safe (quick table)

| Not thread-safe | Thread-safe alternative |
|---|---|
| `HashMap` | `ConcurrentHashMap` |
| `ArrayList` | `CopyOnWriteArrayList`, `Collections.synchronizedList` |
| `StringBuilder` | `StringBuffer` (synchronized) |
| `SimpleDateFormat` | `DateTimeFormatter` (immutable) |
| `int` counter | `AtomicInteger`, `LongAdder` |

**Immutability** is the easiest thread safety: `final` fields, no setters (e.g., `String`, `LocalDate`, records).

---

## 10. Java Memory Model – happens-before (short)

A write is guaranteed visible to a later read if there is a *happens-before* relation:

* Unlock of a monitor → later lock of the **same** monitor.
* Write to a `volatile` → later read of that variable.
* `Thread.start()` → everything in the started thread.
* Everything in a thread → another thread's successful `join()` on it.
* Completion of a task submitted to an executor → result retrieved by `Future.get()`.

---

## 11. Key takeaways

* Shared **mutable** state + multiple threads ⇒ you need synchronization (or avoid sharing / make it immutable).
* `synchronized` = atomicity + visibility; `volatile` = visibility only; `Atomic*` = lock-free atomic ops.
* Always `unlock()` in `finally`.
* Prevent deadlocks with **consistent lock ordering** or `tryLock`.
* Prefer high-level tools: `BlockingQueue`, concurrent collections, executors.

➡ Next: [`04-executors-and-concurrency-utilities.md`](04-executors-and-concurrency-utilities.md)
