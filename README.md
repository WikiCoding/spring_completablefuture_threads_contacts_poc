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