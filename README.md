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
In general, I came to the conclusion that in fact, I don't see a performance improvement with this 
*CompletableFuture* + blocking JDBC approach.

When we do use an Async Driver to interact with the database, we do also see the database calls releasing threads much quicker
so there's no bottleneck there and that's where we do see more performance improvements and more throughput in general.

Maybe virtual threads is something to explore further in a near future, in order to work in that performance and throughput direction without using Spring Reactive Web

# Performance
- First run with 3 as Max Thread pool size using the async endpoint. I observed many requests failing due to the thread 
pool becoming saturated, resulting in RejectedExecutionException when the executor could no longer accept new tasks. 
This is a thread pool exhaustion issue, not a connection rejection at the network level.
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

- Now a run a full blocking endpoint. After reviewing the servlet container (Tomcat) configuration, I confirmed that the default servlet thread pool (maxThreads) is set to 10.
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

- To fully leverage non-blocking behavior, the next step is to test with Spring WebFlux integrated with R2DBC for database operations. 
My current WebFlux test shows similar throughput (~*83 reqs/sec*)
So my conclusion here is that my bottleneck here isn't just yet the limits of the CPU or Network Bandwidth, 
it has to do with the test config that is sleeping for 1 sec after each request, so I'll remove that delay completely and test again.
```
TOTAL RESULTS

    checks_total.......................: 29943   83.010486/s
    checks_succeeded...................: 100.00% 29943 out of 29943
    checks_failed......................: 0.00%   0 out of 29943

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=3.68ms min=1.82ms med=3.56ms max=38.86ms p(90)=4.34ms p(95)=4.61ms
      { expected_response:true }............................................: avg=3.68ms min=1.82ms med=3.56ms max=38.86ms p(90)=4.34ms p(95)=4.61ms
    http_req_failed.........................................................: 0.00%  0 out of 29943
    http_reqs...............................................................: 29943  83.010486/s

    EXECUTION
    iteration_duration......................................................: avg=1s     min=1s     med=1s     max=1.07s   p(90)=1s     p(95)=1s
    iterations..............................................................: 29943  83.010486/s
    vus.....................................................................: 2      min=2          max=100
    vus_max.................................................................: 100    min=100        max=100

    NETWORK
    data_received...........................................................: 3.2 MB 8.9 kB/s
    data_sent...............................................................: 3.1 MB 8.6 kB/s
```

- Removing the **sleep(1)** to flood the server with as much requests as possible and investigate the results.
Here's the results from Spring Reactive server, around *3065 reqs/sec*:
```
TOTAL RESULTS

    checks_total.......................: 1103228 3064.688882/s
    checks_succeeded...................: 100.00% 1103228 out of 1103228
    checks_failed......................: 0.00%   0 out of 1103228

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=27.03ms min=1.77ms med=28.93ms max=129.58ms p(90)=36.36ms p(95)=40.4ms        
      { expected_response:true }............................................: avg=27.03ms min=1.77ms med=28.93ms max=129.58ms p(90)=36.36ms p(95)=40.4ms        
    http_req_failed.........................................................: 0.00%   0 out of 1103228
    http_reqs...............................................................: 1103228 3064.688882/s

    EXECUTION
    iteration_duration......................................................: avg=27.19ms min=1.85ms med=29.08ms max=130.29ms p(90)=36.55ms p(95)=40.58ms       
    iterations..............................................................: 1103228 3064.688882/s
    vus.....................................................................: 1       min=1            max=100
    vus_max.................................................................: 100     min=100          max=100

    NETWORK
    data_received...........................................................: 118 MB  328 kB/s
    data_sent...............................................................: 115 MB  319 kB/s
```

Here's the results for the Sync approach with Tomcat Server with a much lower result of around *2495 reqs/sec*, around 19% less. 
Also, it's possible to observe that quite a couple of requests failed due to timeout right at the start of the performance test and then it recovered and stayed stable until the end:
```
TOTAL RESULTS

    checks_total.......................: 964376 2495.349349/s
    checks_succeeded...................: 99.93% 963751 out of 964376
    checks_failed......................: 0.06%  625 out of 964376

    ✗ 200
      ↳  99% — ✓ 963751 / ✗ 625

    HTTP
    http_req_duration.......................................................: avg=10.42ms min=0s     med=8.51ms max=123.61ms p(90)=20.18ms p(95)=23.7ms
      { expected_response:true }............................................: avg=10.43ms min=57µs   med=8.52ms max=123.61ms p(90)=20.18ms p(95)=23.7ms
    http_req_failed.........................................................: 0.06%  625 out of 964376
    http_reqs...............................................................: 964376 2495.349349/s

    EXECUTION
    iteration_duration......................................................: avg=31.84ms min=1.48ms med=8.71ms max=30s      p(90)=20.44ms p(95)=24.03ms
    iterations..............................................................: 964376 2495.349349/s
    vus.....................................................................: 1      min=1             max=100
    vus_max.................................................................: 100    min=100           max=100

    NETWORK
    data_received...........................................................: 155 MB 402 kB/s
    data_sent...............................................................: 100 MB 259 kB/s
```

Here's the results for the Tomcat server with Async approach and max thread pool size of 10. 
We can state that 10 threads in the pool is way too less and therefore quite a lot of requests fail.
```
TOTAL RESULTS

    checks_total.......................: 34811  91.041487/s
    checks_succeeded...................: 55.14% 19197 out of 34811
    checks_failed......................: 44.85% 15614 out of 34811

    ✗ 200
      ↳  55% — ✓ 19197 / ✗ 15614

    HTTP
    http_req_duration.......................................................: avg=12.85ms  min=0s     med=3.23ms max=30.77s  p(90)=7.09ms p(95)=8.31ms
      { expected_response:true }............................................: avg=4.73ms   min=1.86ms med=4.25ms max=29.74ms p(90)=7.54ms p(95)=8.58ms
    http_req_failed.........................................................: 44.85% 15614 out of 34811
    http_reqs...............................................................: 34811  91.041487/s

    EXECUTION
    iteration_duration......................................................: avg=907.21ms min=1.94ms med=4.26ms max=30.77s  p(90)=9.6ms  p(95)=14.38ms
    iterations..............................................................: 34811  91.041487/s
    vus.....................................................................: 2      min=2              max=100
    vus_max.................................................................: 100    min=100            max=100

    NETWORK
    data_received...........................................................: 7.0 MB 18 kB/s
    data_sent...............................................................: 3.5 MB 9.2 kB/s
```

Here's the results for the Tomcat server with Async approach and max thread pool size of 100. 
It's possible to see that fewer requests are failing (same number as sync) and we're ranging *2499 reqs/sec* which is 
basically the same number of requests that the sync approach made.
```
TOTAL RESULTS

    checks_total.......................: 906457 2498.835623/s
    checks_succeeded...................: 99.95% 906032 out of 906457
    checks_failed......................: 0.04%  425 out of 906457

    ✗ 200
      ↳  99% — ✓ 906032 / ✗ 425

    HTTP
    http_req_duration.......................................................: avg=17.71ms min=0s       med=16ms    max=194.15ms p(90)=32.34ms p(95)=36.89ms
      { expected_response:true }............................................: avg=17.71ms min=966.45µs med=16.01ms max=194.15ms p(90)=32.34ms p(95)=36.9ms
    http_req_failed.........................................................: 0.04%  425 out of 906457
    http_reqs...............................................................: 906457 2498.835623/s

    EXECUTION
    iteration_duration......................................................: avg=34.03ms min=1.87ms   med=16.2ms  max=30.01s   p(90)=32.61ms p(95)=37.23ms
    iterations..............................................................: 906457 2498.835623/s
    vus.....................................................................: 5      min=2             max=100
    vus_max.................................................................: 100    min=100           max=100

    NETWORK
    data_received...........................................................: 146 MB 403 kB/s
    data_sent...............................................................: 94 MB  260 kB/s
```

- Now running the Spring Reactive WebFlux at 500 VUs without sleep
```
TOTAL RESULTS

    checks_total.......................: 1057655 2938.111333/s
    checks_succeeded...................: 100.00% 1057655 out of 1057655
    checks_failed......................: 0.00%   0 out of 1057655

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=141.67ms min=2.36ms med=151.98ms max=551.73ms p(90)=191.05ms p(95)=208.2ms
      { expected_response:true }............................................: avg=141.67ms min=2.36ms med=151.98ms max=551.73ms p(90)=191.05ms p(95)=208.2ms
    http_req_failed.........................................................: 0.00%   0 out of 1057655
    http_reqs...............................................................: 1057655 2938.111333/s

    EXECUTION
    iteration_duration......................................................: avg=141.85ms min=2.47ms med=152.15ms max=551.79ms p(90)=191.21ms p(95)=208.33ms
    iterations..............................................................: 1057655 2938.111333/s
    vus.....................................................................: 1       min=1            max=500
    vus_max.................................................................: 500     min=500          max=500

    NETWORK
    data_received...........................................................: 114 MB  317 kB/s
    data_sent...............................................................: 110 MB  306 kB/s
```

- Now running the Spring Reactive WebFlux at 5000 VUs without sleep
```
TOTAL RESULTS

    checks_total.......................: 1053590 2926.975107/s
    checks_succeeded...................: 100.00% 1053590 out of 1053590
    checks_failed......................: 0.00%   0 out of 1053590

    ✓ 200

    HTTP
    http_req_duration.......................................................: avg=1.42s min=2.6ms  med=1.6s max=2.13s p(90)=1.86s p(95)=1.91s
      { expected_response:true }............................................: avg=1.42s min=2.6ms  med=1.6s max=2.13s p(90)=1.86s p(95)=1.91s
    http_req_failed.........................................................: 0.00%   0 out of 1053590
    http_reqs...............................................................: 1053590 2926.975107/s

    EXECUTION
    iteration_duration......................................................: avg=1.42s min=2.72ms med=1.6s max=2.13s p(90)=1.86s p(95)=1.91s
    iterations..............................................................: 1053590 2926.975107/s
    vus.....................................................................: 48      min=37           max=5000
    vus_max.................................................................: 5000    min=5000         max=5000

    NETWORK
    data_received...........................................................: 114 MB  316 kB/s
    data_sent...............................................................: 110 MB  304 kB/s
```

# Conclusions
At this point, my conclusion is that more available threads in the thread pool make the Tomcat server handle more 
requests but since I'm using JDBC when interacting with the database the thread is blocked, and therefore I'm limited to 
how fast that blocking operation completes so the results when using completely Sync operations and using Async with CompletableFuture 
everywhere doesn't make a difference in performance, but it does make the program more complex. 
Maybe that approach really shines when using for example *WebClient* for http calls, but then we must account for Race Conditions 
and maybe also use Concurrent Collections or synchronized blocks since most likely we will need the result from that http request to proceed so at the end of the day the request might still have that bottleneck.
When going with a Netty server provided with Spring Reactive Project (WebFlux), we're able to handle much more requests/second, 
and we also use R2DBC driver enabling async communication with the database and better managed Thread releases. As a bonus we also have access to the HTTP 2.0 server.