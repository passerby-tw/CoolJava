package io.bgplayground.cooljava;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class PageThreadTest {
    @Test void submissionDoesNotWaitForCalculation() throws Exception {
        PageThread page=new PageThread();
        CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1), cleaned=new CountDownLatch(1);
        AtomicReference<Thread> running=new AtomicReference<>();
        page.submit(() -> {
            running.set(Thread.currentThread()); entered.countDown();
            try { release.await(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            assertNotSame(Thread.currentThread(),running.get());
            AtomicBoolean queuedRan=new AtomicBoolean();
            page.submit(() -> queuedRan.set(true));
            page.close(cleaned::countDown);
            assertEquals(1,cleaned.getCount()); // close did not wait on active computation
            release.countDown();
            assertTrue(cleaned.await(5,TimeUnit.SECONDS));
            assertFalse(queuedRan.get()); // stale page callback is discarded
        } finally { release.countDown(); }
    }
}
