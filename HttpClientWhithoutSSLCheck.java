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
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import com.fasterxml.jackson.core.JsonProcessingException;

public class HttpClientWhithoutSSLCheck {
	private Logger log = Logger.getLogger(LyncApi.class.getName());
	private CloseableHttpClient httpclient;

	private void preapreHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		log.info("Enter");
		SSLContextBuilder builder = SSLContexts.custom();

		builder.loadTrustMaterial(null, new TrustStrategy() {
			@Override
			public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				return true;
			}
		});

		SSLContext sslContext = builder.build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslsf).build();

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		httpclient = HttpClients.custom().setConnectionManager(cm).build();
		log.info("Leave");
	}

	public HttpClientWhithoutSSLCheck() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		super();
		log.info("Enter");
		preapreHttpClient();
		log.info("Leave");
	}

	public String getRequest(String url, String authHeaderValue)
			throws JsonProcessingException, UnsupportedOperationException, IOException {
		log.info("Enter");
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader(HttpHeaders.ACCEPT,ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());
		httpget.setHeader(HttpHeaders.AUTHORIZATION, authHeaderValue);
		log.info("HTTP GET URL " + httpget.getURI());
		Header[] httpHeaders = httpget.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP GET Header " + header.toString());
		}

		HttpResponse httpResponse = httpclient.execute(httpget);
		log.info("HTTP RET Status " + httpResponse.getStatusLine());
		httpHeaders = httpResponse.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP RET Header " + header.toString());
		}
		String body = readHttpEntityBody(httpResponse.getEntity().getContent());
		log.info("HTTP BODY " + body);
		log.info("Leave");
		return body;
	}

	public Header[] getRequestOnlyHeaders(String url, String authHeaderValue)
			throws JsonProcessingException, UnsupportedOperationException, IOException {
		log.info("Enter");
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());
		httpget.setHeader(HttpHeaders.AUTHORIZATION, authHeaderValue);
		log.info("HTTP GET URL " + httpget.getURI());
		Header[] httpHeaders = httpget.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP GET Header " + header.toString());
		}
		HttpResponse httpResponse = httpclient.execute(httpget);
		log.info("HTTP RET Status " + httpResponse.getStatusLine());
		httpHeaders = httpResponse.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP RET Header " + header.toString());
		}
		log.info("Leave");
		return httpHeaders;
	}

	public String postRequestJSON(String url, AbstractHttpEntity entity, String authHeaderValue)
			throws ClientProtocolException, IOException {
		return postRequestContent(url, entity, authHeaderValue,ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());
	}

	public String postRequestFrom(String url, AbstractHttpEntity entity, String authHeaderValue)
			throws ClientProtocolException, IOException {
		return postRequestContent(url, entity, authHeaderValue,
				ContentType.APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.UTF_8).toString());
	}

	public String postRequestContent(String url, AbstractHttpEntity entity, String authHeaderValue, String contentType)
			throws ClientProtocolException, IOException {
		log.info("Enter");
		HttpPost httpPost = new HttpPost(url);
		httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());
		httpPost.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
		if (authHeaderValue != "") {
			httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeaderValue);
		}
		httpPost.setEntity(entity);
		log.info("HTTP POST URL" + httpPost.getURI());
		Header[] httpHeaders = httpPost.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP POST Header " + header.toString());
		}
		String data = readHttpEntityBody(httpPost.getEntity().getContent());
		log.info("HTTP Palyload " + data);

		HttpResponse httpResponse = httpclient.execute(httpPost);
		log.info("HTTP RET Status " + httpResponse.getStatusLine());
		httpHeaders = httpResponse.getAllHeaders();
		for (Header header : httpHeaders) {
			log.info("HTTP RET Header " + header.toString());
		}
		log.info("HTTP RET Status " + httpResponse.getEntity().getContent());
		String body = readHttpEntityBody(httpResponse.getEntity().getContent());
		log.info("HTTP BODY " + body);
		log.info("Leave");
		return body;
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

}
