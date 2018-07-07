package jsqlstreamstore;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;
import jsqlstreamstore.subscriptions.PollingStreamStoreNotifier;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig;

public class PollingStreamStoreNotifierTests {

    // TODO: move this to some fixture or something
    private EmbeddedPostgres postgres;
    private PostgresStreamStore store;

    @Before
    public void setUp() throws Exception {
        postgres = new EmbeddedPostgres();
        String url = postgres.start(cachedRuntimeConfig(Paths.get(System.getProperty("java.io.tmpdir"), "pgembed")));

        PostgresStreamStoreSettings settings = new PostgresStreamStoreSettings.Builder(url).build();

        store = new PostgresStreamStore(settings);

        Flyway flyway = new Flyway();
        flyway.setDataSource(url, EmbeddedPostgres.DEFAULT_USER, EmbeddedPostgres.DEFAULT_PASSWORD);
        flyway.setLocations("classpath:db/migrations");
        flyway.migrate();
    }

    @Test
    public void whenExceptionOccursReadingHeadPositionThenPollingShouldStillContinue() {
        AtomicLong readHeadCount = new AtomicLong(0L);
        Supplier<Long> readHeadPosition = () -> {
            readHeadCount.incrementAndGet();
            if(readHeadCount.get() % 2 == 0) {
                throw new RuntimeException("oops");
            }
            return readHeadCount.get();
        };

        AtomicInteger received = new AtomicInteger(0);
        TestObserver to = new TestObserver(new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Long aVoid) {
                received.incrementAndGet();
                if (received.get() > 5) {
                    this.onComplete();
                }
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        PollingStreamStoreNotifier notifier = new PollingStreamStoreNotifier(readHeadPosition, 10);
        notifier.subscribe(to);

        to.assertSubscribed();
        to.awaitCount(5);
        to.assertValueCount(5);
        assertEquals(5, received.get());
    }

}