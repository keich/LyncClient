package ru.mykeich.lync;

/*
MIT License

Copyright (c) 2019 Alexander Zazhigin mykeich@yandex.ru

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LyncApi {
	private HttpClientWhithoutSSLCheck httpClient;
	private Logger log = Logger.getLogger(LyncApi.class.getName());

	private ObjectMapper jsonMapper = new ObjectMapper();
	private Map<String, String> lyncRegistryMap = new ConcurrentHashMap<String, String>();
	private static Map<String, LyncAuthentication> authenticationMap = new ConcurrentHashMap<String, LyncAuthentication>();
	public final Map<String, Conversation> conversations = new ConcurrentHashMap<String, Conversation>();

	private static final String LYNC_DISCOVERY_URL = "LYNC_DISCOVERY_URL";

	private static final String LYNC_DISCOVERY_LINKS_SELF_HREF = "_links-self-href";
	private static final String LYNC_DISCOVERY_LINKS_USER_HREF = "_links-user-href";
	private static final String LYNC_DISCOVERY_LINKS_XFRAME_HREF = "_links-xframe-href";

	private static final String HTTP_HEADER_VALUE_MSRTCOAUTH = "MsRtcOAuth";
	private static final String HTTP_HEADER_VALUE_BEARER = "Bearer";

	private static final String PATTERN_MSRTCOAUTH = "MsRtcOAuth\\s*href=\"(.*)\",";

	private static final String PATTERN_BEARER = "(.*),\\s*client_id=\"(.*)\"";

	private Properties properties = new Properties();

	static class UserHrefChangeHost extends Exception {
		private static final long serialVersionUID = 146L;
	}

	public LyncApi() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		super();
		log.info("Enter");
		properties.put(LYNC_DISCOVERY_URL, "https://test.com");
		httpClient = new HttpClientWhithoutSSLCheck();
		log.info("Leave");
	}

	void closeConnection() {
		log.info("Enter");
		String clientId = (String) authenticationMap.keySet().toArray()[0];
		authenticationMap.remove(clientId);
		log.info("Leave");
	}

	void startEventThread() {
		log.info("Enter");
		String clientId;
		clientId = (String) authenticationMap.keySet().toArray()[0];
		EventThread thread = new EventThread(clientId);
		thread.start();

		log.info("Leave");
	}

	public class EventThread extends Thread {
		String clientId;

		public EventThread(String clientId) {
			super();
			this.clientId = clientId;
		}

		boolean isLyncClientActive(String clientId) {
			LyncAuthentication autch;
			autch = authenticationMap.get(clientId);
			log.info("autch " + autch);
			if (autch == null) {
				return false;
			}

			return true;
		}

		public void run() {
			log.info("Enter");
			while (isLyncClientActive(clientId)) {
				log.info("Read events");
				try {
					Thread.sleep(2000);
					try {
						getEvent(clientId);
					} catch (JsonProcessingException e) {
						log.logp(Level.WARNING, "LyncApi.EventThread", "run", "JsonProcessingException", e);
					} catch (UnsupportedOperationException e) {
						log.logp(Level.WARNING, "LyncApi.EventThread", "run", "UnsupportedOperationException", e);
					} catch (IOException e) {
						log.logp(Level.WARNING, "LyncApi.EventThread", "run", "IOException", e);
					}
				} catch (InterruptedException e) {
					log.logp(Level.WARNING, "LyncApi.EventThread", "run",
							"Sleep throws InterruptedException. Stop event thread", e);
					break;
				}
			}
			log.info("Leave");
		}
	}

	private static byte[] key = { 0x34, 0x63, 0x69, 0x73, 0x47, 0x73, 0x41, 0x53, 0x64, 0x66, 0x72, 0x65, 0x74, 0x4c,
			0x63, 0x79 };

	public static String decrypt(String strToDecrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			final String decryptedString = new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt)));
			return decryptedString;
		} catch (Exception e) {
			System.err.println("Error while decrypting" + e.getMessage());
		}
		return null;
	}

	public static String encrypt(String strToEncrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			final SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			final String encryptedString = Base64.encodeBase64String(cipher.doFinal(strToEncrypt.getBytes()));
			return encryptedString;
		} catch (Exception e) {
			System.err.println("Error while encrypting" + e.getMessage());
		}
		return null;
	}

	void conAutoDiscoveryHref() throws ClientProtocolException, IOException {
		log.info("Enter");

		String lyncDiscoveryServer = properties.getProperty(LYNC_DISCOVERY_URL);
		String body = httpClient.getRequest(lyncDiscoveryServer, "");
		JsonNode jsonNode = jsonMapper.readTree(body);

		String linksSelf = jsonNode.get("_links").get("self").get("href").asText();
		String linksUser = jsonNode.get("_links").get("user").get("href").asText();
		;
		String linksXframe = jsonNode.get("_links").get("xframe").get("href").asText();
		log.info("HTTP RET linksSelf " + linksSelf);
		log.info("HTTP RET linksUser " + linksUser);
		log.info("HTTP RET linksXframe " + linksXframe);

		lyncRegistryMap.put(LYNC_DISCOVERY_LINKS_SELF_HREF, linksSelf);
		lyncRegistryMap.put(LYNC_DISCOVERY_LINKS_USER_HREF, linksUser);
		lyncRegistryMap.put(LYNC_DISCOVERY_LINKS_XFRAME_HREF, linksXframe);
		;
		log.info("Leave");
	}

	void conAuthenticate() throws Exception {
		log.info("Enter");
		String lyncUserHref = lyncRegistryMap.get(LYNC_DISCOVERY_LINKS_USER_HREF);

		Header[] httpHeaders = httpClient.getRequestOnlyHeaders(lyncUserHref, "");

		String OAuthHeaderValue = "";
		String clientIdHeaderValue = "";
		for (Header header : httpHeaders) {
			if (header.getValue().contains(HTTP_HEADER_VALUE_MSRTCOAUTH)) {
				OAuthHeaderValue = header.getValue();
			}
			if (header.getValue().contains(HTTP_HEADER_VALUE_BEARER)) {
				clientIdHeaderValue = header.getValue();
			}
		}

		if (clientIdHeaderValue == "") {
			throw new Exception("Not fount header " + HTTP_HEADER_VALUE_BEARER + " in HTTP headers");
		}

		if (OAuthHeaderValue == "") {
			throw new Exception("Not fount header " + HTTP_HEADER_VALUE_MSRTCOAUTH + " in HTTP headers");
		}

		log.info("Found MsRtcOAuth heaqder " + OAuthHeaderValue);
		log.info("Found bearer heaqder " + clientIdHeaderValue);

		Matcher matcherMsRtcOauth = Pattern.compile(PATTERN_MSRTCOAUTH, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
				.matcher(OAuthHeaderValue);
		Matcher matcherBearer = Pattern.compile(PATTERN_BEARER, Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
				.matcher(clientIdHeaderValue);

		String OAuthUrl = "";
		if (matcherMsRtcOauth.find()) {
			OAuthUrl = matcherMsRtcOauth.group(1);
			log.info("Parse header " + HTTP_HEADER_VALUE_MSRTCOAUTH + " " + OAuthUrl);
		}

		String bearer = "";
		if (matcherBearer.find()) {
			bearer = matcherBearer.group(2);
			log.info("Parse header " + HTTP_HEADER_VALUE_BEARER + " " + bearer);
		}

		if (OAuthHeaderValue == "") {
			throw new Exception("Error regex parse header " + OAuthHeaderValue);
		}

		if (clientIdHeaderValue == "") {
			throw new Exception("Error regex parse header " + clientIdHeaderValue);
		}

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("grant_type", "password"));
		nvps.add(new BasicNameValuePair("username", "test\\test"));
		nvps.add(new BasicNameValuePair("password", decrypt("changeme")));

		String body = httpClient.postRequestFrom(OAuthUrl, new UrlEncodedFormEntity(nvps), "");
		JsonNode jsonNode = jsonMapper.readTree(body);
		String accessToken = jsonNode.get("access_token").asText();
		log.info("Parse json access_token:" + accessToken);
		String expiresIn = jsonNode.get("expires_in").asText();
		log.info("Parse json expiresIn:" + expiresIn);
		String tokenType = jsonNode.get("token_type").asText();
		log.info("Parse json tokenType:" + tokenType);

		String authHeaderValue = tokenType + " " + accessToken;
		URI uri = new URI(OAuthUrl);
		LyncAuthentication auth = new LyncAuthentication(authHeaderValue, new Date(), Long.valueOf(expiresIn),
				uri.getScheme() + "://" + uri.getHost());
		authenticationMap.put(bearer, auth);

		log.info("Leave");
	}

	void createApplication() throws ClientProtocolException, IOException, UserHrefChangeHost, URISyntaxException {
		log.info("Enter");
		lyncRegistryMap.get(LYNC_DISCOVERY_LINKS_USER_HREF);
		LyncAuthentication autch;
		String clientId;
		clientId = (String) authenticationMap.keySet().toArray()[0];
		autch = authenticationMap.get(clientId);

		String authHeaderValue = autch.getAccessToken();

		String lyncUserHref = lyncRegistryMap.get(LYNC_DISCOVERY_LINKS_USER_HREF);
		String body = httpClient.getRequest(lyncUserHref, authHeaderValue);
		JsonNode jsonNode = jsonMapper.readTree(body);
		String applicationsUrl = jsonNode.get("_links").get("applications").get("href").asText();
		URI uri = new URI(applicationsUrl);
		String checkUri = uri.getScheme() + "://" + uri.getHost();
		String host = autch.getHostName();
		log.info("checkUri " + checkUri + " registration host " + host);
		if (!checkUri.equalsIgnoreCase(host)) {
			authenticationMap.remove(clientId);
			lyncRegistryMap.remove(LYNC_DISCOVERY_LINKS_USER_HREF);
			lyncRegistryMap.put(LYNC_DISCOVERY_LINKS_USER_HREF,
					jsonNode.get("_links").get("self").get("href").asText());
			log.warning("Change register server from " + host + " to " + checkUri);
			throw new UserHrefChangeHost();
		}

		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("UserAgent", "Java Client For UCWA");
		paramsMap.put("EndpointId", clientId);
		paramsMap.put("Culture", "en-US");

		Writer strWriter = new StringWriter();
		jsonMapper.writeValue(strWriter, paramsMap);

		StringEntity entity = new StringEntity(strWriter.toString(), ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
		body = httpClient.postRequestJSON(applicationsUrl, entity, authHeaderValue);
		jsonNode = jsonMapper.readTree(body);
		autch.setApplicationJsonNode(jsonNode);
		String eventNext = jsonNode.get("_links").get("events").get("href").asText();
		log.info("get next event URL " + eventNext);
		autch.setEventUrl(eventNext);

		log.info("Leave");
	}

	void getEvent(String clientId) throws JsonProcessingException, UnsupportedOperationException, IOException {
		log.info("Enter");
		LyncAuthentication auth;

		auth = authenticationMap.get(clientId);

		String body = httpClient.getRequest(auth.getHostName() + auth.getEventUrl(), auth.getAccessToken());
		JsonNode jsonNode = jsonMapper.readTree(body);
		String nextEventUrl = jsonNode.get("_links").get("next").get("href").asText();
		log.info("Parse next event URL " + nextEventUrl);
		auth.setEventUrl(nextEventUrl);
		log.info("Leave");
	}

	public String encode64(String plainText) {
		byte[] encoded = Base64.encodeBase64(plainText.getBytes());
		log.info("Original String: " + plainText);
		log.info("Base64 Encoded String : " + new String(encoded));
		return new String(encoded);
	}

	public String readHttpEntityBody(InputStream inputStream) throws IOException {
		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}

		body = stringBuilder.toString();
		return body;
	}

	void createConversations(String subject, String message)
			throws JsonProcessingException, UnsupportedOperationException, IOException {
		log.info("Enter");
		LyncAuthentication autch;
		String clientId;
		clientId = (String) authenticationMap.keySet().toArray()[0];
		autch = authenticationMap.get(clientId);

		String startMessagingUrl = autch.getApplicationJsonNode().get("_embedded").get("communication").get("_links")
				.get("startMessaging").get("href").asText();
		startMessagingUrl = autch.getHostName() + startMessagingUrl;

		ObjectNode jNode = jsonMapper.createObjectNode();
		jNode.put("to", "sip:test@test.com");;
		jNode.put("subject", subject);
		jNode.put("operationId", "24cb7404e0a247c5a2d4eb0373a47dbf");

		ObjectNode messageNode = jsonMapper.createObjectNode();
		messageNode.put("href", "data:"+ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8).toString()+";base64," + encode64(message));
		ObjectNode _linksNode = jsonMapper.createObjectNode();
		_linksNode.set("message", messageNode);
		jNode.set("_links", _linksNode);

		StringEntity entuty = new StringEntity(jNode.toString(), ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
		httpClient.postRequestJSON(startMessagingUrl, entuty, autch.getAccessToken());
		// JsonNode jsonNode = jsonMapper.readTree(body);
		log.info("Leave");
	}
}
