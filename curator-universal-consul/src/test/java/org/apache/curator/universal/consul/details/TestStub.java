package org.apache.curator.universal.consul.details;

import org.apache.curator.universal.api.NodePath;
import org.apache.curator.universal.consul.client.ConsulClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestStub
{
    public static class Person
    {
        private final String firstName;
        private final String lastName;
        private final int age;

        public Person()
        {
            this("", "", 0);
        }

        public Person(String firstName, String lastName, int age)
        {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public String getFirstName()
        {
            return firstName;
        }

        public String getLastName()
        {
            return lastName;
        }

        public int getAge()
        {
            return age;
        }
    }

    @Test
    public void testFoo() throws Exception
    {
        try ( ConsulClient client = ConsulClient.build(HttpAsyncClients.createDefault(), new URI("http://localhost:8500")).build() )
        {
            client.start();
            client.blockUntilSession(Duration.ofHours(1));

            CountDownLatch latch = new CountDownLatch(2);
/*
            NodePath path = NodePath.parse("/a/b/c");
            ModelSpec<Person> modelSpec = ModelSpec.builder(path, JacksonModelSerializer.build(Person.class)).build();

            ModeledHandle<Person> wrapped = handle.wrap(modelSpec);

            CompletionStage<String> stage = wrapped.set(new Person("Jordan", "Zimmerman", 53));
            stage.exceptionally(e -> {
                throw new RuntimeException(e);
            });
            CompletionStage<Person> stage2 = stage.thenCompose(s -> {
                latch.countDown();
                return wrapped.read();
            });
            stage2.thenAccept(person -> {
                System.out.println(person);
                latch.countDown();
            });
            stage2.exceptionally(e -> {
                throw new RuntimeException(e);
            });

*/
/*
            CuratorLock lock = handle.createLock(NodePath.parse("/a/lock"));
            lock.acquire(1, TimeUnit.DAYS);
            Executors.newSingleThreadScheduledExecutor().schedule(lock::release, 1, TimeUnit.MINUTES);

            try ( ConsulClient client2 = ConsulClient.build(HttpAsyncClients.createDefault(), new URI("http://localhost:8500")).build() )
            {
                client2.start();
                client2.blockUntilSession(Duration.ofHours(1));
                CuratorHandle handle2 = CuratorHandleFactory.wrap(client2);
                CuratorLock lock2 = handle2.createLock(NodePath.parse("/a/lock"));
                lock2.acquire(1, TimeUnit.DAYS);
            }

            latch.await();
*/
            complete(client.set(NodePath.parse("/a/b/c"), "one".getBytes()));
            complete(client.set(NodePath.parse("/a/b/c/d"), "two".getBytes()));
            complete(client.set(NodePath.parse("/a/b/c/d/e"), "three".getBytes()));
            complete(client.set(NodePath.parse("/a/b/c/d/f"), "four".getBytes()));
            complete(client.set(NodePath.parse("/a/b/c/d/f/g"), "five".getBytes()));

            complete(client.children(NodePath.parse("/a/b/c/d")).thenAccept(System.out::println));

            ConsulCacheImpl cache = new ConsulCacheImpl((ConsulClientImpl)client, NodePath.parse("/a/b/c"));
            cache.start();
            Thread.currentThread().join();
        }
    }

    protected void complete(CompletionStage<?> stage)
    {
        try
        {
            stage.toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
        catch ( InterruptedException e )
        {
            Thread.interrupted();
        }
        catch ( ExecutionException e )
        {
            if ( e.getCause() instanceof AssertionError )
            {
                throw (AssertionError)e.getCause();
            }
            Assert.fail("get() failed", e);
        }
        catch ( TimeoutException e )
        {
            Assert.fail("get() timed out");
        }
    }
}
