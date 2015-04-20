---
permalink: "/index.html"
---

### Yeah, We Know...

Technically, you're not supposed to execute logic in threads within a JEE application. There are (will be?) JEE mechanisms available for threaded processing.

But sometimes, in some CDI applications, it makes sense to manage your own threads. Weft is an API that organizes and injects Executor instances for your CDI application.

### One Annotation To Create Them All

Controlling which executor gets injected is simple, using `org.commonjava.cdi.util.weft.ExecutorConfig`:

```
@ExecutorConfig( named="my-threads" )
@Inject
private Executor executor;
```

In case your executor isn't configured (see Configuring, below), you can also set some default parameters in the annotation:

```
@ExecutorConfig( named="my-threads", daemon=true, priority=8, threads=10 )
[...]
```

### Configuring

The `ExecutorProvider` component (which manages the Executor instances injected by Weft) `@Inject`'s an instance of `WeftConfig`, which can specify priorities, thread counts, and the daemon flag for each named Executor your application uses. It also accepts a default priority and thread count setting to be applied to any Executor not otherwise configured. Settings in the injected `WeftConfig` instance always override those given in the `@ExecutorConfig` annotation, allowing the user to fine tune threading in the installed application.