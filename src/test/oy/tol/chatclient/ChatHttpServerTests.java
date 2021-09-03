package oy.tol.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;

/*
These tests test the server using the ChatHttpClient.
*/
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Tests using ChatHttpClient")
public class ChatHttpServerTests implements ChatClientDataProvider {

    private static ChatHttpClient httpClient = null;
    private String username = null;
    private String password = null;
    private String email = null;

    ChatHttpServerTests() {
        httpClient = new ChatHttpClient(this, ChatUnitTestSettings.clientSideCertificate);
    }

    @Test
    @BeforeAll
    @DisplayName("Setting up the test environment")
    public static void initialize() {
        assertTrue(ChatUnitTestSettings.readSettingsXML(), () -> "Could not initialize the tests. Check your test setting XML file");
        System.out.println("Initializing ChatHttpServerTests");
    }
    
    @Test
    @AfterAll
    public static void teardown() {
        System.out.println("Finished ChatHttpServerTests.");
    }

    @Test 
    @Order(1)
    @DisplayName("Testing HTTP GET /chat without valid user credentials, must throw")
    void getWithoutCredentials() {
        System.out.println("Testing HTTP GET /chat without valid user credentials, must throw");
        assertThrows(Exception.class, () -> httpClient.getChatMessages(null));
    }

    @Test 
    @Order(2)
    @DisplayName("Testing HTTP GET /chat with invalid user credentials, must throw")
    void getWithInvalidCredentials() {
        System.out.println("Testing HTTP GET /chat with invalid user credentials, must throw");
        username = "randomnonexistentusernamehere";
        password = "invalidpasswordtoo";
        assertThrows(Exception.class, () -> httpClient.getChatMessages(null));
    }

    @Test 
    @Order(3)
    @DisplayName("Testing user registration")
    void testUserRegistration() {
        System.out.println("Testing user registration");
        username = randomString(15);
        password = randomString(15);
        email = randomString(30);
        try {
            assertEquals(200, httpClient.registerUser());
		} catch (Exception e) {
			fail("Exception in registering a user");
		}
    }

    @Order(4)
    @RepeatedTest(10)
    @DisplayName("Testing getting messages from server")
    void testGetMessages() {
        try {
            System.out.println("Testing getting messages from server");
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            int result = httpClient.getChatMessages(null);
            assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

    @Order(5)
    @DisplayName("Testing posting empty messages to server")
    void testPostEmptyMessages() {
        try {
            System.out.println("Testing posting empty messages to server");
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            String message = "";
            int result = httpClient.postChatMessage(message, null);
            assertTrue(result >= 400, () -> "Must get error from server");
		} catch (Exception e) {
			fail("Exception in posting empty chat message to server: " + e.getMessage());
		}
    }

    @Order(5)
    @DisplayName("Testing posting whitespace messages to server")
    void testPostWhitespaceMessages() {
        try {
            System.out.println("Testing posting whitespace messages to server");
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            String message = "    ";
            int result = httpClient.postChatMessage(message, null);
            assertTrue(result >= 400, () -> "Must get error from server");
		} catch (Exception e) {
			fail("Exception in posting empty chat message to server: " + e.getMessage());
		}
    }

    @Order(7)
    @RepeatedTest(10)
    @DisplayName("Testing posting messages to server")
    void testPostMessages() {
        try {
            System.out.println("Testing posting messages to server");
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            String message = randomString(120);
            int result = httpClient.postChatMessage(message, null);
            assertTrue((result == 200 || result == 429), () -> "Must get 200 from server (or 429 if posting too fast).");
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

    @Order(8)
    @Test
    @DisplayName("Testing posting and getting messages to and from server")
    void testHeavyGetPostMessages() {
        try {
            System.out.println("Testing posting and getting messages to and from server");
            // Must be an existing user in the database.
            username = ChatUnitTestSettings.existingUser;
            password = ChatUnitTestSettings.existingPassword;
            final int MSGS_TO_ADD = 10;
            final int LOOPS_TO_RUN = 10;
            int loop = LOOPS_TO_RUN;
            int result = httpClient.getChatMessages(null);
            assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
            List<ChatMessage> messages = httpClient.getNewMessages();
            while (loop >= 0) {
                for (int looper = 0; looper < MSGS_TO_ADD; looper++) {
                    String message = randomString(120);
                    result = httpClient.postChatMessage(message, null);
                    assertTrue(result == 200, () -> "Must get 200 from server");
                }
                result = httpClient.getChatMessages(null);
                assertTrue(result == 200 || result == 204, () -> "Must get 200 or 204 from server");
                messages = httpClient.getNewMessages();
                loop--;
            }
		} catch (Exception e) {
			fail("Exception in getting chat messages from server: " + e.getMessage());
		}
    }

	@Override
	public String getServer() {
        return ChatUnitTestSettings.dataProvider.getServer();
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getNick() {
		return username;
	}

	@Override
	public String getEmail() {
		return email;
	}

	@Override
	public int getServerVersion() {
		return ChatUnitTestSettings.serverVersion;
	}

    private String randomString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
    
        String generatedString = random.ints(leftLimit, rightLimit + 1)
          .limit(length)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
    
        return generatedString;
    }

}
