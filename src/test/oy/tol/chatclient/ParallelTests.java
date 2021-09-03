package oy.tol.chatclient;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

public class ParallelTests {

    private static ChatHttpClient httpClient1 = null;
    private static ChatHttpClient httpClient1get = null;
    private static ChatHttpClient httpClient2 = null;

    
    ParallelTests() {
        client1 = new Client1();
        client2 = new Client2();
        httpClient1 = new ChatHttpClient(client1, ChatUnitTestSettings.clientSideCertificate);
        httpClient2 = new ChatHttpClient(client2, ChatUnitTestSettings.clientSideCertificate);
        httpClient1get = new ChatHttpClient(client1, ChatUnitTestSettings.clientSideCertificate);
    }

    @Test
    @BeforeAll
    @DisplayName("Setting up the test environment")
    public static void initialize() {
        assertTrue(ChatUnitTestSettings.readSettingsXML(), () -> "Could not initialize the tests. Check your test setting XML file");
        System.out.println("Initializing ParallelTests");
    }
    
    @Test
    @AfterAll
    public static void teardown() {
        System.out.println("Finished ParallelTests.");
    }

    @RepeatedTest(500)
    @Execution(ExecutionMode.CONCURRENT)
    @DisplayName("Get messages in parallel from server")
    void executeChatGet() {
        if (ChatUnitTestSettings.serverVersion < 5) {
            return;
        }
        try {
            int code = httpClient1get.getChatMessages(null);
            assertTrue((code == 200 || code == 204), () -> "Must get 200 or 204 from server");
        } catch (Exception e) {
            fail("Getting messages from server in parallel failed: " + e.getMessage());
        }
    }

    @Execution(ExecutionMode.CONCURRENT)
    @TestFactory
    @DisplayName("First thread A posting chat messages")
    Collection<DynamicTest> testParallelDynamicTests1() {
        final int DYNAMIC_POST_COUNT = 500;
        List<DynamicTest> testArray = new ArrayList<DynamicTest>();
        if (ChatUnitTestSettings.serverVersion < 5) {
            return testArray;
        }
        System.out.println(Thread.currentThread().getName() + " => Parallel tests A starting...");
        for (int counter = 0; counter < DYNAMIC_POST_COUNT; counter++) {
            final int passingInt = counter;
            testArray.add(dynamicTest("Dynamic test A" + counter, () -> {
                int code = httpClient1.postChatMessage("Dynamically posting A-" + passingInt, null);
                assertTrue((code == 200 || code == 429), () -> "Server returned code " + code + " " + httpClient1.getServerNotification());
                if (code >= 400) {
                    System.out.println("Server returned " + code + " " + httpClient1.getServerNotification());
                } 
                TimeUnit.MILLISECONDS.sleep(100);
            }));
        }
        return testArray;
    }

    @Execution(ExecutionMode.CONCURRENT)
    @TestFactory
    @DisplayName("Second thread B posting chat messages")
    Collection<DynamicTest> testParallelDynamicTests2() {
        final int DYNAMIC_POST_COUNT = 500;
        List<DynamicTest> testArray = new ArrayList<DynamicTest>();
        if (ChatUnitTestSettings.serverVersion < 5) {
            return testArray;
        }
        System.out.println(Thread.currentThread().getName() + " => Parallel tests B starting...");
        for (int counter = 0; counter < DYNAMIC_POST_COUNT; counter++) {
            final int passingInt = counter;
            testArray.add(dynamicTest("Dynamic test B" + counter, () -> {
                int code = httpClient2.postChatMessage("Dynamically posting B-" + passingInt, null);
                assertTrue((code == 200 || code == 429), () -> "Server returned code " + code + " " + httpClient2.getServerNotification());
                if (code >= 400) {
                    System.out.println("Server returned " + code + " " + httpClient2.getServerNotification());
                } 
                TimeUnit.MILLISECONDS.sleep(100);
            }));
        }
        return testArray;
    }

    class Client1 implements ChatClientDataProvider {
        @Override
        public String getServer() {
            return ChatUnitTestSettings.dataProvider.getServer();
        }
        @Override
        public String getUsername() {
            return ChatUnitTestSettings.existingUser;
        }
    
        @Override
        public String getPassword() {
            return ChatUnitTestSettings.existingPassword;
        }
    
        @Override
        public String getNick() {
            return ChatUnitTestSettings.existingUser;
        }
    
        @Override
        public String getEmail() {
            return "not needed in this test";
        }
    
        @Override
        public int getServerVersion() {
            return ChatUnitTestSettings.serverVersion;
        }
    }
    private static Client1 client1;
    class Client2 implements ChatClientDataProvider {
        @Override
        public String getServer() {
            return ChatUnitTestSettings.dataProvider.getServer();
        }
        @Override
        public String getUsername() {
            return ChatUnitTestSettings.existingUser2;
        }
    
        @Override
        public String getPassword() {
            return ChatUnitTestSettings.existingPassword2;
        }
    
        @Override
        public String getNick() {
            return ChatUnitTestSettings.existingUser2;
        }
    
        @Override
        public String getEmail() {
            return "not needed in this test";
        }
    
        @Override
        public int getServerVersion() {
            return ChatUnitTestSettings.serverVersion;
        }
    }
    private static Client2 client2;
}
