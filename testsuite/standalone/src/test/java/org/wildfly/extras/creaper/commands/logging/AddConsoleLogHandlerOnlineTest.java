package org.wildfly.extras.creaper.commands.logging;

import com.google.common.base.Charsets;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class AddConsoleLogHandlerOnlineTest {
    private static final String TEST_HANDLER_NAME = "creaper-handler";
    private static final Address TEST_HANDLER_ADDRESS =
            Address.subsystem("logging").and("console-handler", TEST_HANDLER_NAME);
    private OnlineManagementClient client;
    private Operations ops;
    private Administration administration;

    @Before
    public void connect() throws IOException {
        client = ManagementClient.online(OnlineOptions.standalone().localDefault().build());
        ops = new Operations(client);
        administration = new Administration(client);
    }

    @After
    public void cleanup() throws IOException, CliException, OperationException, TimeoutException, InterruptedException {
        try {
            ops.removeIfExists(TEST_HANDLER_ADDRESS);
            administration.reloadIfRequired();
        } finally {
            client.close();
        }
    }

    @Test
    public void addHandler() throws Exception {
        AddConsoleLogHandler addConsoleHandler = Logging.handler().console().add(TEST_HANDLER_NAME)
                .level(LogLevel.WARN)
                .filter("match(\"new-filter*\")")
                .autoFlush(true)
                .enabled(true)
                .patternFormatter("new-pattern")
                .target(ConsoleTarget.STDOUT)
                .encoding(Charsets.ISO_8859_1)
                .build();

        client.apply(addConsoleHandler);

        assertTrue("console handler should be created", ops.exists(TEST_HANDLER_ADDRESS));

        ModelNodeResult result = ops.readAttribute(TEST_HANDLER_ADDRESS, "level");
        result.assertSuccess();
        assertEquals("level should be changed", LogLevel.WARN.value(), result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "filter-spec");
        result.assertSuccess();
        assertEquals("filter-spec should be changed", "match(\"new-filter*\")", result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "autoflush");
        result.assertSuccess();
        assertTrue("autoflush should be changed", result.booleanValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "enabled");
        result.assertSuccess();
        assertTrue("enabled should be changed", result.booleanValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "formatter");
        result.assertSuccess();
        assertEquals("pattern-formatter should be changed", "new-pattern", result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "encoding");
        result.assertSuccess();
        assertEquals("pattern-formatter should be changed", Charsets.ISO_8859_1.displayName(), result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "target");
        result.assertSuccess();
        assertEquals("pattern-formatter should be changed", ConsoleTarget.STDOUT.value(), result.stringValue());
    }

    @Test
    public void replaceExisting() throws Exception {
        AddConsoleLogHandler addConsoleHandler = Logging.handler().console().add(TEST_HANDLER_NAME)
                .autoFlush(true)
                .enabled(true)
                .level(LogLevel.OFF)
                .filter("match(\"filter\")")
                .encoding(Charsets.UTF_8)
                .target(ConsoleTarget.STDOUT)
                .patternFormatter("aaa")
                .build();

        client.apply(addConsoleHandler);

        assertTrue("console handler should be created", ops.exists(TEST_HANDLER_ADDRESS));

        addConsoleHandler = Logging.handler().console().add(TEST_HANDLER_NAME)
                .autoFlush(false)
                .enabled(false)
                .level(LogLevel.ALL)
                .filter("match(\"new-filter\")")
                .encoding(Charsets.UTF_8)
                .target(ConsoleTarget.STDOUT)
                .patternFormatter("bbb")
                .replaceExisting()
                .build();

        client.apply(addConsoleHandler);

        assertTrue("console handler should be created", ops.exists(TEST_HANDLER_ADDRESS));

        ModelNodeResult result = ops.readAttribute(TEST_HANDLER_ADDRESS, "level");
        result.assertSuccess();
        assertEquals("level should be changed", LogLevel.ALL.value(), result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "filter-spec");
        result.assertSuccess();
        assertEquals("filter-spec should be changed", "match(\"new-filter\")", result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "autoflush");
        result.assertSuccess();
        assertFalse("autoflush should be changed", result.booleanValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "enabled");
        result.assertSuccess();
        assertFalse("enabled should be changed", result.booleanValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "formatter");
        result.assertSuccess();
        assertEquals("pattern-formatter should be changed", "bbb", result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "encoding");
        result.assertSuccess();
        assertEquals("pattern-formatter should not be changed", Charsets.UTF_8.displayName(), result.stringValue());

        result = ops.readAttribute(TEST_HANDLER_ADDRESS, "target");
        result.assertSuccess();
        assertEquals("pattern-formatter should not be changed", ConsoleTarget.STDOUT.value(), result.stringValue());
    }

    @Test(expected = CommandFailedException.class)
    public void replaceExisting2() throws Exception {
        AddConsoleLogHandler addConsoleHandler = Logging.handler().console().add(TEST_HANDLER_NAME)
                .autoFlush(true)
                .enabled(true)
                .level(LogLevel.OFF)
                .filter("match(\"filter\")")
                .encoding(Charsets.UTF_8)
                .target(ConsoleTarget.STDOUT)
                .patternFormatter("aaa")
                .build();

        client.apply(addConsoleHandler);

        addConsoleHandler = Logging.handler().console().add(TEST_HANDLER_NAME)
                .autoFlush(false)
                .enabled(false)
                .level(LogLevel.ALL)
                .filter("match(\"new-filter\")")
                .encoding(Charsets.UTF_8)
                .target(ConsoleTarget.STDOUT)
                .patternFormatter("bbb")
                .build();

        client.apply(addConsoleHandler);
    }
}
