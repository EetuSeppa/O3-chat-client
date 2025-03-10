package oy.tol.chatclient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatHttpClient {

	// Different paths (contexts) the server supports and this client implements.
	private static final String CHAT = "chat";
	private static final String REGISTRATION = "registration";
	private static final String UPDATE = "updateUserInfo";
	private static final String CREATE = "createChannel";
	private static final String CHANGE = "changeChannel";

	// When using JSON (excercise 3), List<ChatMessage> is used,
	// and earlier, use List<String>.
	private List<ChatMessage> newMessages = null;
	private List<String> plainStringMessages = null;

	private String serverNotification = "";

	private ChatClientDataProvider dataProvider = null;

	private static final int CONNECT_TIMEOUT = 10 * 1000;
	private static final int REQUEST_TIMEOUT = 30 * 1000;

	private static final DateTimeFormatter jsonDateFormatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	private String latestDataFromServerIsFrom = null;

	private String certificateFile;

	private boolean useHttpsInRequests = true;

	ChatHttpClient(ChatClientDataProvider provider, String certificateFileWithPath) {
		this(provider, certificateFileWithPath, true);
	}

	ChatHttpClient(ChatClientDataProvider provider, String certificateFileWithPath, boolean useHttps) {
		dataProvider = provider;
		certificateFile = certificateFileWithPath;
		useHttpsInRequests = useHttps;
		if (null == certificateFile) {
			useHttpsInRequests = false;
		}
	}

	public String getServerNotification() {
		return serverNotification;
	}

	public List<ChatMessage> getNewMessages() {
		return newMessages;
	}

	public List<String> getPlainStringMessages() {
		return plainStringMessages;
	}

	public synchronized JSONObject changeChannel(String channelName) throws KeyManagementException,
	KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CHANGE; 
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-cache");

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		byte msgBytes[];
		JSONObject msg = new JSONObject();
		
		msg.put("channelName", channelName);
		msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		if (responseCode == 200 || responseCode == 204) {
			latestDataFromServerIsFrom = null; //Set latest data to null to get all messages in new channels
			String input;
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			String totalInput = "";
			while ((input = in.readLine()) != null) {
				totalInput += input;
			}
			JSONObject jsonObject = new JSONObject(totalInput);
			jsonObject.put("responseCode", responseCode);
			return jsonObject;
		} else {
			JSONObject errorObject = new JSONObject();
			errorObject.put("responseCode", responseCode);
			return errorObject;
		}

	}


	public synchronized int createChannel(String newChannelName, String description, String username) throws KeyManagementException,
	KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CREATE; 
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-cache");

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		byte msgBytes[];
		JSONObject msg = new JSONObject();
		
		msg.put("newChannelName", newChannelName);
		msg.put("description", description);
		msg.put("createdBy", username);
		msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		return responseCode;

	}

	public synchronized int updateUserData(String oldUsername, String username, String password, String email) throws KeyManagementException, 
	KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += UPDATE; 
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-cache");

		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/json");

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		byte msgBytes[];
		JSONObject msg = new JSONObject();
		
		msg.put("oldUsername", oldUsername);
		msg.put("user", username);
		msg.put("password", password);
		msg.put("email", email);
		msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		return responseCode;
	}

	public synchronized int getChatMessages(String channelName) throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CHAT;
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);
		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-cache");

		connection.setRequestMethod("GET");
		if (dataProvider.getServerVersion() >= 3) {
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			connection.setRequestProperty("Content-Type", "text/plain");
		}
		if (dataProvider.getServerVersion() >= 5 && null != latestDataFromServerIsFrom) {
			connection.setRequestProperty("If-Modified-Since", latestDataFromServerIsFrom);
		}

		if (channelName != null) {
			connection.setRequestProperty("Channel-Name", channelName);
		}

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		int responseCode = connection.getResponseCode();
		if (responseCode == 204) {
			newMessages = null;
		} else if (responseCode >= 200 && responseCode < 300) {
			if (dataProvider.getServerVersion() >= 5) {
				latestDataFromServerIsFrom = connection.getHeaderField("Last-Modified");
			}
			String input;
			BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
			if (dataProvider.getServerVersion() >= 3) {
				String totalInput = "";
				while ((input = in.readLine()) != null) {
					totalInput += input;
				}
				JSONArray jsonArray = new JSONArray(totalInput);
				if (jsonArray.length() > 0) {
					newMessages = new ArrayList<ChatMessage>();
					for (int index = 0; index < jsonArray.length(); index++) {
						JSONObject object = jsonArray.getJSONObject(index);
						ChatMessage msg = ChatMessage.from(object);
						newMessages.add(msg);
					}
					Collections.sort(newMessages, new Comparator<ChatMessage>() {
						@Override
						public int compare(ChatMessage lhs, ChatMessage rhs) {
							return lhs.sent.compareTo(rhs.sent);
						}
					});
				}
			} else { // Server not yet supports JSON.
				plainStringMessages = new ArrayList<String>();
				while ((input = in.readLine()) != null) {
					plainStringMessages.add(input);
				}
			}
			in.close();
			serverNotification = "";
		} else {
			newMessages = null;
			plainStringMessages = null;
			InputStream in = connection.getInputStream();
			if (null != in) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String inputLine;
				while ((inputLine = reader.readLine()) != null) {
					serverNotification += " " + inputLine;
				}
				in.close();
			}
		}
		return responseCode;
	}

	public synchronized int postChatMessage(String message, String channelName) throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += CHAT;
		URL url = new URL(addr);

		String auth = dataProvider.getUsername() + ":" + dataProvider.getPassword();

		HttpURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes;
		if (dataProvider.getServerVersion() >= 3) {
			JSONObject msg = new JSONObject();
			msg.put("user", dataProvider.getNick());
			msg.put("message", message);
			if (channelName != null) {
				msg.put("channelName", channelName);
			} else {
				msg.put("channelName", "null");
			}

			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
			String dateText = now.format(jsonDateFormatter);
			msg.put("sent", dateText);
			msgBytes = msg.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			msgBytes = message.getBytes("UTF-8");
			connection.setRequestProperty("Content-Type", "text/plain");
		}
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 300) {
			// Successfully posted.
			serverNotification = "";
		} else {
			// Sometimes -- no idea why! -- connection.getInputStream() throws IOExeption
			// when conducting parallell chat post tests. This is no fault of the server,
			// so as a temporary fix catch exceptions here and put an indication about this
			// in the serverNotification. This way the tests do not fail to indicate server error
			// because this is not a server error.
			try {
				InputStream in = connection.getErrorStream();
				if (null != in) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					String inputLine;
					while ((inputLine = reader.readLine()) != null) {
						serverNotification += " " + inputLine;
					}
					in.close();
				}	
			} catch (IOException e) {
				serverNotification = "Could not read server error message from connection input stream " + e.getMessage();
			}
		}
		return responseCode;
	}

	public synchronized int registerUser() throws KeyManagementException, KeyStoreException, CertificateException,
			NoSuchAlgorithmException, IOException {
		String addr = dataProvider.getServer();
		if (!addr.endsWith("/")) {
			addr += "/";
		}
		addr += REGISTRATION;
		URL url = new URL(addr);

		HttpURLConnection connection = createTrustingConnectionDebug(url);

		byte[] msgBytes;
		if (dataProvider.getServerVersion() >= 3) {
			JSONObject registrationMsg = new JSONObject();
			registrationMsg.put("username", dataProvider.getUsername());
			registrationMsg.put("password", dataProvider.getPassword());
			registrationMsg.put("email", dataProvider.getEmail());
			msgBytes = registrationMsg.toString().getBytes(StandardCharsets.UTF_8);
			connection.setRequestProperty("Content-Type", "application/json");
		} else {
			String registrationMsg = dataProvider.getUsername() + ":" + dataProvider.getPassword();
			msgBytes = registrationMsg.getBytes("UTF-8");
			connection.setRequestProperty("Content-Type", "text/plain");
		}

		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestProperty("Content-Length", String.valueOf(msgBytes.length));

		OutputStream writer = connection.getOutputStream();
		writer.write(msgBytes);
		writer.close();

		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 300) {
			// Successfully registered.
			serverNotification = "";
		} else {
			InputStream in = connection.getInputStream();
			if (null != in) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String inputLine;
				while ((inputLine = reader.readLine()) != null) {
					serverNotification += " " + inputLine;
				}
				in.close();
			}
		}
		return responseCode;
	}

	// For accepting self signed certificates. Not to be used in production
	// software!

	private HttpURLConnection createTrustingConnectionDebug(URL url) throws KeyStoreException, CertificateException,
			NoSuchAlgorithmException, FileNotFoundException, KeyManagementException, IOException {
		if (useHttpsInRequests) {
			Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(certificateFile));
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setCertificateEntry("localhost", certificate);
	
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			trustManagerFactory.init(keyStore);
	
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
	
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(sslContext.getSocketFactory());
			// All requests use these common timeouts.
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(REQUEST_TIMEOUT);
			return connection;
		} else {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			return connection;
		}
	}
}
