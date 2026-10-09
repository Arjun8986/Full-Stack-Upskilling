# 01 – Multithreading Basics & Ways to Create Threads

## 1. Core concepts

| Term | Meaning |
|---|---|
| **Process** | An independent running program with its own memory space (e.g., your JVM). |
| **Thread** | A lightweight unit of execution *inside* a process. Threads of one process **share heap memory** but each has its **own stack** and program counter. |
| **Multithreading** | Running multiple threads within one process to do work concurrently. |
| **Concurrency** | Dealing with many tasks at once (interleaving; may run on 1 core). |
| **Parallelism** | Actually executing many tasks at the same instant (needs multiple cores). |
| **Context switch** | CPU saving one thread's state and loading another's. It has a cost. |

**Why use threads?** Responsiveness (UI/server not blocked), better CPU utilisation, doing I/O waits in the background, handling many client requests.

**Costs / risks:** race conditions, deadlocks, harder debugging, context-switch overhead, memory visibility bugs.

**Main thread:** every Java program starts with the `main` thread. Other (non-daemon) threads keep the JVM alive until they finish.

```java
// File: MainThreadDemo.java
public class MainThreadDemo {
    public static void main(String[] args) {
        Thread t = Thread.currentThread();
        System.out.println("Name     : " + t.getName());      // main
        System.out.println("Priority : " + t.getPriority());  // 5
        System.out.println("Group    : " + t.getThreadGroup().getName());
    }
}
```

---

## 2. Ways to create a thread (very common interview question)

| # | Way | Returns result? | Can throw checked exception? | Notes |
|---|---|---|---|---|
| 1 | Extend `Thread` | No | No | Uses up your single inheritance slot |
| 2 | Implement `Runnable` | No | No | **Preferred** over extending Thread |
| 3 | Runnable as a **lambda** | No | No | Shortest form (Java 8+) |
| 4 | `Callable` + `FutureTask` / `ExecutorService` | **Yes** | **Yes** | Use for results |
| 5 | `ExecutorService` thread pool | Optional | Via Future | **Best practice in real projects** |
| 6 | `CompletableFuture` | Yes | Handled via `exceptionally` | Async pipelines |
| 7 | **Virtual threads** (Java 21) | Optional | – | Millions of cheap threads |

### 2.1 Extending the `Thread` class

```java
// File: ExtendThreadDemo.java
class MyThread extends Thread {
    @Override
    public void run() {                       // the task
        System.out.println("Running in: " + Thread.currentThread().getName());
    }
}

public class ExtendThreadDemo {
    public static void main(String[] args) {
        MyThread t1 = new MyThread();
        t1.setName("worker-1");
        t1.start();                           // creates a NEW call stack and calls run()
    }
}
```

**Drawbacks:** Java has no multiple inheritance, so your class can't extend anything else; tightly couples the *task* with the *thread mechanism*.

### 2.2 Implementing `Runnable` (preferred)

```java
// File: RunnableDemo.java
class PrintTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task run by: " + Thread.currentThread().getName());
    }
}

public class RunnableDemo {
    public static void main(String[] args) {
        Thread t = new Thread(new PrintTask(), "runnable-thread");
        t.start();
    }
}
```

Why better: separates *what to run* (task) from *how to run* (thread); class can still extend another class; the same Runnable can be given to a thread pool.

### 2.3 Lambda / anonymous class

```java
// File: LambdaThreadDemo.java
public class LambdaThreadDemo {
    public static void main(String[] args) {
        // Runnable is a functional interface (single abstract method)
        Thread t1 = new Thread(() ->
            System.out.println("Lambda thread: " + Thread.currentThread().getName()));
        t1.start();

        // Anonymous inner class (older style)
        Thread t2 = new Thread(new Runnable() {
            @Override public void run() {
                System.out.println("Anonymous thread");
            }
        });
        t2.start();
    }
}
```

### 2.4 `Callable` + `Future` (returns a value, can throw)

`Runnable.run()` returns `void`. `Callable<V>.call()` returns `V` and may `throw Exception`.

```java
// File: CallableDemo.java
import java.util.concurrent.*;

public class CallableDemo {
    public static void main(String[] args) throws Exception {
        Callable<Integer> sumTask = () -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) sum += i;
            return sum;
        };

        // (a) Using FutureTask directly with a Thread
        FutureTask<Integer> ft = new FutureTask<>(sumTask);
        new Thread(ft).start();
        System.out.println("FutureTask result = " + ft.get());   // blocks until done

        // (b) Using an ExecutorService
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Integer> f = pool.submit(sumTask);
        System.out.println("Executor result   = " + f.get(2, TimeUnit.SECONDS));
        pool.shutdown();
    }
}
```

### 2.5 `ExecutorService` thread pool (real-world way)

```java
// File: ExecutorDemo.java
import java.util.concurrent.*;

public class ExecutorDemo {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            pool.execute(() ->
                System.out.println("Task " + id + " on " + Thread.currentThread().getName()));
        }
        pool.shutdown();                          // stop accepting new tasks
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
```

More on pools in file `04`.

### 2.6 `CompletableFuture`

```java
// File: CompletableFutureBasics.java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureBasics {
    public static void main(String[] args) {
        CompletableFuture<String> cf = CompletableFuture
            .supplyAsync(() -> "Hello")             // runs in ForkJoinPool.commonPool()
            .thenApply(s -> s + " World")
            .thenApply(String::toUpperCase);
        System.out.println(cf.join());              // HELLO WORLD
    }
}
```

### 2.7 Virtual threads (Java 21+)

```java
// File: VirtualThreadDemo.java
import java.util.concurrent.*;

public class VirtualThreadDemo {
    public static void main(String[] args) throws Exception {
        // Style 1
        Thread vt = Thread.ofVirtual().name("virtual-1").start(() ->
            System.out.println("Hi from " + Thread.currentThread()));
        vt.join();

        // Style 2: one cheap virtual thread per task
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                int id = i;
                ex.submit(() -> System.out.println("task " + id
                        + " virtual=" + Thread.currentThread().isVirtual()));
            }
        } // close() waits for tasks
    }
}
```

Virtual threads are managed by the JVM (not 1:1 with OS threads) – ideal for **blocking I/O** workloads. Not a benefit for CPU-bound work.

---

## 3. `start()` vs `run()` – classic question

```java
// File: StartVsRun.java
public class StartVsRun {
    public static void main(String[] args) {
        Thread t = new Thread(() ->
            System.out.println("Executing in: " + Thread.currentThread().getName()));

        t.run();     // plain method call -> runs in "main", NO new thread
        t.start();   // new thread is created -> runs in "Thread-0"
    }
}
```

| | `start()` | `run()` |
|---|---|---|
| Creates a new thread | **Yes** | No |
| Runs on | New call stack | Caller's thread |
| Can be called twice | **No** → `IllegalThreadStateException` | Yes (it's just a method) |

```java
// File: StartTwice.java
public class StartTwice {
    public static void main(String[] args) {
        Thread t = new Thread(() -> {});
        t.start();
        try {
            t.start();                       // second start
        } catch (IllegalThreadStateException e) {
            System.out.println("Cannot restart a thread: " + e);
        }
    }
}
```

---

## 4. `Thread` vs `Runnable` vs `Callable` – quick answers

* **Thread vs Runnable?** Thread *is* a worker; Runnable *is* the task. Prefer Runnable: allows extending another class, reusable in pools, loose coupling.
* **Runnable vs Callable?** Callable returns a value and may throw checked exceptions; used with `submit()`.
* **What if you override `run()` but call `start()` on a thread constructed with a Runnable?** Overridden `run()` of the Thread subclass wins; the Runnable is ignored.
* **Can a thread be restarted?** No. Create a new one.
* **What happens if `run()` throws an unchecked exception?** The thread dies; the exception goes to the `UncaughtExceptionHandler` (see file `02`).

---

## 5. Run order is not guaranteed

```java
// File: NondeterministicOrder.java
public class NondeterministicOrder {
    public static void main(String[] args) {
        for (int i = 1; i <= 3; i++) {
            int id = i;
            new Thread(() -> System.out.println("Thread " + id)).start();
        }
        System.out.println("main finished starting threads");
    }
}
```

Output order changes from run to run – the **thread scheduler** decides. Never rely on priority or start order for correctness; use `join()`, latches or locks.

---

## 6. Key takeaways

* Create threads with **Runnable / Callable + ExecutorService** in production code.
* Always call **`start()`**, never `run()`.
* A thread can be started only **once**.
* Use `Callable`/`Future`/`CompletableFuture` when you need a **result or exception**.
* Java 21 **virtual threads** make thread-per-request affordable for I/O-heavy apps.

➡ Next: [`02-thread-lifecycle-and-methods.md`](02-thread-lifecycle-and-methods.md)
