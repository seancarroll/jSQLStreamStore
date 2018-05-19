package jsqlstreamstore;

import com.fasterxml.uuid.Generators;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import jsqlstreamstore.streams.*;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.Assert.*;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig;


public class PostgresStreamStoreTest {

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

    @After
    public void tearDown() throws Exception {
        if (postgres != null && postgres.getProcess().isPresent()) {
            postgres.stop();
        }
        // postgres.getProcess().ifPresent(PostgresProcess::stop);
    }

    @Test
    public void readAllForwardTest() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage});

        ReadAllPage all = store.readAllForwards(0, 10, true);

        assertTrue(all.isEnd());
        assertEquals(ReadDirection.FORWARD, all.getReadDirection());
        assertEquals(1, all.getMessages().length);
        assertEquals(newMessage.getMessageId(), all.getMessages()[0].getMessageId());
    }

    @Test
    public void readStreamForwards() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage});

        ReadStreamPage page = store.readStreamForwards("test", 0, 10, false);

        assertTrue(page.isEnd());
        assertEquals(ReadDirection.FORWARD, page.getReadDirection());
        assertEquals(1, page.getMessages().length);
        assertEquals(newMessage.getMessageId(), page.getMessages()[0].getMessageId());
    }

    @Test
    public void readAllForwardNext() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        NewStreamMessage newMessageToo = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Shawn\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage, newMessageToo});

        ReadAllPage page = store.readAllForwards(0, 1, false);
        assertEquals(1, page.getMessages().length);
        assertEquals(newMessage.getMessageId(), page.getMessages().clone()[0].getMessageId());

        ReadAllPage page2 = page.readNext();
        assertEquals(1, page2.getMessages().length);
        assertEquals(newMessageToo.getMessageId(), page2.getMessages().clone()[0].getMessageId());
    }

    @Test
    public void readAllBackwardsTest() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage});

        ReadAllPage all = store.readAllBackwards(Position.END, 10, false);

        assertTrue(all.isEnd());
        assertEquals(ReadDirection.BACKWARD, all.getReadDirection());
        assertEquals(1, all.getMessages().length);
        assertEquals(newMessage.getMessageId(), all.getMessages()[0].getMessageId());
    }


    @Test
    public void readStreamBackwards() throws SQLException, InterruptedException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        NewStreamMessage newMessageToo = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Shawn\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage, newMessageToo});

        ReadStreamPage page = store.readStreamBackwards("test", StreamVersion.END, 1, false);

        assertFalse(page.isEnd());
        assertEquals(ReadDirection.BACKWARD, page.getReadDirection());
        assertEquals(1, page.getMessages().length);
        assertEquals(newMessageToo.getMessageId(), page.getMessages()[0].getMessageId());
    }

    @Test
    public void appendStreamExpectedVersionNoStream() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage});
        ReadStreamPage page = store.readStreamForwards("test", 0, 1, true);
        ReadStreamPage pageBackwards = store.readStreamBackwards("test", StreamVersion.END, 1, true);
        ReadAllPage allPage = store.readAllForwards(0, 10, true);

        assertNotNull(page);
        assertNotNull(allPage);
        assertEquals(1, allPage.getMessages().length);
        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
        assertEquals(1, page.getMessages().length);
        assertEquals(1, pageBackwards.getMessages().length);
    }

    @Test
    public void appendStreamExpectedVersionAny() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[] {newMessage});

        ReadStreamPage page = store.readStreamForwards("test", 0, 1, true);

        assertNotNull(page);
        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
        assertEquals(1, page.getMessages().length);
    }


//    @Test
//    public void appendStreamIdempotent() throws SQLException {
//        NewStreamMessage newMessage = new NewStreamMessage(
//            Generators.timeBasedGenerator().generate(),
//            "someType",
//            "{\"name\":\"Sean\"}");
//
//        AppendResult result = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[] {newMessage, newMessage});
//
//        ReadStreamPage page = store.readStreamForwards("test", 0, 10, true);
//
//        assertNotNull(page);
//        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
//        assertEquals(1, page.getMessages().length);
//    }

//    @Test
//    public void appendStreamIdempotentSeparateInserts() throws SQLException {
//        NewStreamMessage newMessage = new NewStreamMessage(
//            Generators.timeBasedGenerator().generate(),
//            "someType",
//            "{\"name\":\"Sean\"}");
//
//        AppendResult result = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[] {newMessage});
//        AppendResult resultTwo = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[] {newMessage});
//        ReadStreamPage page = store.readStreamForwards("test", 0, 10, true);
//
//        assertNotNull(page);
//        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
//        assertEquals(1, page.getMessages().length);
//    }

//    @Test
//    public void appendStreamIdempotentFail() throws SQLException {
//
//        UUID id = Generators.timeBasedGenerator().generate();
//        NewStreamMessage newMessage = new NewStreamMessage(
//            id,
//            "someType",
//            "{\"name\":\"Sean\"}");
//
//        NewStreamMessage newMessageTwo = new NewStreamMessage(
//            id,
//            "someType",
//            "{\"name\":\"Dan\"}");
//
//        AppendResult result = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[] {newMessage});
//
//        try {
//            AppendResult resultTwo = store.appendToStream("test", ExpectedVersion.ANY, new NewStreamMessage[]{newMessageTwo});
//            fail("should throw");
//        } catch(Exception ex) {
//
//        }
//
//        ReadStreamPage page = store.readStreamForwards("test", 0, 10, true);
//
//        assertNotNull(page);
//        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
//        assertEquals(1, page.getMessages().length);
//    }


    @Test
    public void deleteStream() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage});

        store.deleteStream("test", ExpectedVersion.ANY);

        ReadStreamPage page = store.readStreamForwards("test", 0, 1, true);
        assertEquals(PageReadStatus.STREAM_NOT_FOUND, page.getStatus());
    }

    @Test
    public void deleteMessage() throws SQLException {
        NewStreamMessage newMessage = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Sean\"}");

        NewStreamMessage newMessageToo = new NewStreamMessage(
            Generators.timeBasedGenerator().generate(),
            "someType",
            "{\"name\":\"Shawn\"}");

        AppendResult result = store.appendToStream("test", ExpectedVersion.NO_STREAM, new NewStreamMessage[] {newMessage, newMessageToo});

        store.deleteMessage("test", newMessage.getMessageId());

        ReadStreamPage page = store.readStreamForwards("test", 0, 10, false);
        assertEquals(PageReadStatus.SUCCESS, page.getStatus());
        assertEquals(1, page.getMessages().length);
        assertEquals(newMessageToo.getMessageId(), page.getMessages().clone()[0].getMessageId());
    }

    // TODO: add test for expired messages

}