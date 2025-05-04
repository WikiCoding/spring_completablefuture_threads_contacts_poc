# Summary

Simple CRUD app (without update actually) making use, the most as possible of non-blocking operations with the purpose 
of releasing threads asap and with that improving performance. 
I'm not making use of RDBC and the Spring Reactive Project, which means the calls to the database actually blocks a thread.
To prove it, I added a 10sec delay on the **saveAsync** method on the service and logs to see in which thread I'm in!
I also made the max thread pool size to 3 so it's easier to see it in action.
Since Java 21 there's Project Loom which enables the usage of virtual threads, and that means that the real thread is actually released.
Example:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        contactsRepository.save(contact); // blocking
        Thread.sleep(10_000); // blocking
    });
}
```
This blocks a virtual thread, but real threads are freed up, so it's much more scalable.
In general, although I don't have here a performance test comparison, I did it previously in another project and I came 
to the conclusion that in fact, I do see a performance improvement with this *CompletableFuture* + blocking JDBC approach
but it actually happens because also as it's possible to observe in the logs, the servlet threads (eg. http-nio-8080-exec-1)
are released much quicker (as soon as the request leaves the controller) to it allows for more requests per second at the server level.
Then it's down to the Service layer to be able to handle all those requests in parallel on the available thread pool size.

When we do use an Async Driver to interact with the database, we do also see the database calls releasing threads much quicker
so there's no bottleneck there and that's where we do see more performance improvements and more throughput in general.

Maybe virtual threads is something to explore further in a near future, in order to work in that performance and throughput direction without using Spring Reactive Web

# Performance
- First run with 3 as Max Thread pool size using the async endpoint. We see that quite a lot of requests are failing due to time out/server rejecting new connections.
The errors logging to the console started around the 97 VUs and was:
```
ERROR 31236 --- [asynccrudcompletablefuture] [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.core.task.TaskRejectedException: ExecutorService in active state did not accept task: java.util.concurrent.CompletableFuture$AsyncSupply@2ced34af] with root cause
java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.CompletableFuture$AsyncSupply@2ced34af rejected from org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor$1@7fb1820d[Running, pool size = 3, active threads = 3, queued tasks = 3, completed tasks = 5555]
```
So the results were:
```
TOTAL RESULTS

    checks_total.......................: 11758  31.222878/s
    checks_succeeded...................: 23.60% 2775 out of 11758
    checks_failed......................: 76.39% 8983 out of 11758

    ✗ 200
      ↳  23% — ✓ 2775 / ✗ 8983

    HTTP
    http_req_duration.......................................................: avg=10.64ms min=0s     med=2.7ms  max=30.29s  p(90)=4.2ms  p(95)=4.94ms
      { expected_response:true }............................................: avg=4.11ms  min=2.09ms med=3.76ms max=13.47ms p(90)=5.73ms p(95)=6.74ms
    http_req_failed.........................................................: 76.39% 8983 out of 11758
    http_reqs...............................................................: 11758  31.222878/s

    EXECUTION
    iteration_duration......................................................: avg=2.64s   min=1s     med=1s     max=31.3s   p(90)=1s     p(95)=16.72s
    iterations..............................................................: 11757  31.220223/s
    vus.....................................................................: 2      min=2             max=100
    vus_max.................................................................: 100    min=100           max=100

    NETWORK
    data_received...........................................................: 2.7 MB 7.1 kB/s
    data_sent...............................................................: 1.2 MB 3.1 kB/s

```

- Now a run a full blocking endpoint. After some manual tests I figured out that the servlet thread pool is 10 by default.
This time around, it didn't fail any request at 97 neither 100 VUs, but I'll rerun my async version with a thread pool size of 10 so it's comparable.
Also, as expected, the servlet thread is not released after leaving the controller so it actually uses the same thread until the request is finished.
These were the results from the sync run and also since there's no failures the performance results are much better:
```
TOTAL RESULTS

    checks_total.......................: 29945   83.027751/s
    checks_succeeded...................: 100.00% 29945 out of 29945
    checks_failed......................: 0.00%   0 out of 29945

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=3.59ms min=1.47ms med=3.48ms max=20.43ms p(90)=4.23ms p(95)=4.48ms
      { expected_response:true }............................................: avg=3.59ms min=1.47ms med=3.48ms max=20.43ms p(90)=4.23ms p(95)=4.48ms
    http_req_failed.........................................................: 0.00%  0 out of 29945
    http_reqs...............................................................: 29945  83.027751/s

    EXECUTION
    iteration_duration......................................................: avg=1s     min=1s     med=1s     max=1.04s   p(90)=1s     p(95)=1s
    iterations..............................................................: 29945  83.027751/s
    vus.....................................................................: 2      min=2          max=100
    vus_max.................................................................: 100    min=100        max=100

    NETWORK
    data_received...........................................................: 4.8 MB 13 kB/s
    data_sent...............................................................: 3.1 MB 8.6 kB/s
```

- Async with Thread Pool of size 10. This time around it didn't starve at 97 nor 100 VUs. It actually wasn't able to handle more requests/sec so results were very much equal to the sync processing but with slightly more code complexity.
Here are the performance results, sitting at *82,97 reqs/sec* very close to the sync's *83.028 reqs/sec*:
```
TOTAL RESULTS

    checks_total.......................: 29938   82.970757/s
    checks_succeeded...................: 100.00% 29938 out of 29938
    checks_failed......................: 0.00%   0 out of 29938

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=4.05ms min=2.2ms med=3.92ms max=32.41ms p(90)=4.78ms p(95)=5.22ms
      { expected_response:true }............................................: avg=4.05ms min=2.2ms med=3.92ms max=32.41ms p(90)=4.78ms p(95)=5.22ms
    http_req_failed.........................................................: 0.00%  0 out of 29938
    http_reqs...............................................................: 29938  82.970757/s

    EXECUTION
    iteration_duration......................................................: avg=1s     min=1s    med=1s     max=1.04s   p(90)=1s     p(95)=1s
    iterations..............................................................: 29938  82.970757/s
    vus.....................................................................: 2      min=2          max=100
    vus_max.................................................................: 100    min=100        max=100

    NETWORK
    data_received...........................................................: 4.8 MB 13 kB/s
    data_sent...............................................................: 3.1 MB 8.6 kB/s
```

- All there is left to do, is to test with Spring Reactive WebFlux approach to make use of R2DBC async capabilities and check the actual performance gains.
