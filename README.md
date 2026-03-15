<div align="center">

<img src="https://upload.wikimedia.org/wikipedia/commons/0/0c/Thread_pool.svg" alt="Thread Pool Diagram" width="600">

# Thread Pool Implementation

A from-scratch implementation of Java's `ExecutorService` / `ThreadPoolExecutor` concept,
built for learning. Demonstrates worker threads, a blocking task queue, futures,
pluggable rejection policies, and shutdown lifecycle.

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.6%2B-C71A36?logo=apachemaven&logoColor=white)
![Tests](https://img.shields.io/badge/Tests-12%20passing-brightgreen?logo=junit5&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?logo=apache&logoColor=white)

</div>

---

## Usage

### Simple construction

```java
ThreadPool pool = new ThreadPool(4); // 4 worker threads

// Submit a fire-and-forget task
pool.submit(() -> System.out.println("Hello from " + Thread.currentThread().getName()));

// Submit a task that returns a result
Future<Integer> future = pool.submit(() -> 2 + 2);
Integer result = future.get(); // blocks until done → 4

// Shutdown — finish queued tasks, then stop
pool.shutdown();
pool.awaitTermination(5000);
```

### Builder with custom configuration

```java
ThreadPool pool = ThreadPool.builder(4)
    .taskQueue(new BlockingTaskQueue())
    .rejectionPolicy(RejectionPolicies.callerRuns())
    .build();
```

### Rejection policies

| Policy | Behaviour |
|--------|-----------|
| `RejectionPolicies.abort()` | Throws `IllegalStateException` **(default)** |
| `RejectionPolicies.discard()` | Silently drops the task |
| `RejectionPolicies.callerRuns()` | Runs the task on the submitting thread |

---

## How It Works

```
   Producer threads                             Worker threads
  ┌──────────────┐                            ┌──────────────┐
  │  submit(task) │──┐                    ┌──>│   Worker-0   │──> take() ──> task.run()
  │  submit(task) │──┤  ┌─────────────┐   │   └──────────────┘
  │  submit(task) │──┼─>│  Blocking   │───┤   ┌──────────────┐
  │  submit(task) │──┤  │  Task Queue │───┼──>│   Worker-1   │──> take() ──> task.run()
  │  submit(task) │──┘  └─────────────┘   │   └──────────────┘
  └──────────────┘                        │   ┌──────────────┐
                                          └──>│   Worker-2   │──> take() ──> task.run()
                                              └──────────────┘
```

| Step | What happens |
|------|-------------|
| **1. Submit** | Puts a task on the `TaskQueue` (default: `BlockingTaskQueue`) |
| **2. Workers loop** | `take()` a task (blocks if queue empty) → `run()` it → repeat |
| **3. Future** | Wraps a `Callable` — calling `get()` blocks until the result is ready |
| **4. Shutdown** | Sends a `PoisonPill` per worker; each worker exits when it receives one |

---

## Shutdown Lifecycle

```
  RUNNING  ──shutdown()──▶  SHUTDOWN  ──all workers exit──▶  TERMINATED
```

| State | Behaviour |
|-------|----------|
| **RUNNING** | Accepts and executes tasks |
| **SHUTDOWN** | Rejects new tasks (via `RejectionPolicy`), workers drain the queue then exit via poison pill |
| **TERMINATED** | All workers have exited; `awaitTermination()` returns `true` |

---

## Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Strategy** | `RejectionPolicy`, `TaskQueue` | Pluggable rejection behaviour and queue implementation |
| **Builder** | `ThreadPoolBuilder` | Flexible, readable pool construction |
| **Observer** | `Worker.Callback` | Decouples worker lifecycle events from the pool |
| **Poison Pill** | `PoisonPill` | Graceful shutdown signalling through the task queue |

---

## Project Structure

```
src/main/java/com/custom/threadpool/
│
├── ExecutorService.java                API — submit, shutdown, awaitTermination
├── Callable.java                       API — functional interface for value-returning tasks
│
├── core/                               Pool implementation
│   ├── ThreadPool.java                   Thread pool — submit, shutdown, lifecycle
│   ├── ThreadPoolBuilder.java            Fluent builder for custom pool configuration
│   └── Worker.java                       Daemon thread: take → run → repeat
│
├── future/                             Asynchronous result
│   ├── Future.java                       Interface — get(), isDone()
│   └── FutureImpl.java                   Implementation — wait/notify coordination
│
├── queue/                              Task queue
│   ├── TaskQueue.java                    Interface — put(), take(), isEmpty()
│   └── BlockingTaskQueue.java            Implementation — synchronized + wait/notify
│
├── rejection/                          Rejection strategy
│   ├── RejectionPolicy.java             Interface — reject(Runnable)
│   └── RejectionPolicies.java           Built-in policies: abort, discard, callerRuns
│
└── shootdown/                          Shutdown signalling
    └── PoisonPill.java                   Sentinel task that tells a worker to exit

src/test/java/com/custom/threadpool/
└── ThreadPoolExecutorServiceTest.java  12 tests
```

---

## Mapping to Real Java Classes

| Custom Class | Real JDK Class |
|---|---|
| `ExecutorService` | `java.util.concurrent.ExecutorService` |
| `ThreadPool` | `java.util.concurrent.ThreadPoolExecutor` |
| `Callable<V>` | `java.util.concurrent.Callable<V>` |
| `Future<V>` | `java.util.concurrent.Future<V>` |
| `FutureImpl<V>` | `java.util.concurrent.FutureTask<V>` |
| `TaskQueue` / `BlockingTaskQueue` | `java.util.concurrent.BlockingQueue` |
| `Worker` | `ThreadPoolExecutor.Worker` |
| `RejectionPolicy` | `java.util.concurrent.RejectedExecutionHandler` |
| `RejectionPolicies.abort()` | `ThreadPoolExecutor.AbortPolicy` |
| `RejectionPolicies.discard()` | `ThreadPoolExecutor.DiscardPolicy` |
| `RejectionPolicies.callerRuns()` | `ThreadPoolExecutor.CallerRunsPolicy` |
| `PoisonPill` | Interrupt + state checks in real JDK |

---

## Building and Testing

```bash
mvn test
```

Runs **12 tests** covering:
- Basic task execution (Runnable and Callable)
- Concurrency (multiple threads, task ordering)
- Future results and exception propagation
- Shutdown lifecycle and `awaitTermination`
- Worker resilience (bad tasks don't kill workers)
- Builder and rejection policies (discard, caller-runs)

---

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
