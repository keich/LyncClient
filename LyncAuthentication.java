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

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;

class LyncAuthentication {

	private String accessToken;
	private Date tokenCreationTime;
	private Long expiresIn;
	private JsonNode applicationJsonNode;
	private String hostName;
	private String eventUrl;

	synchronized String getEventUrl() {
		return eventUrl;
	}

	synchronized void setEventUrl(String eventUrl) {
		this.eventUrl = eventUrl;
	}

	synchronized public String getHostName() {
		return hostName;
	}

	LyncAuthentication(String accessToken, Date tokenCreationTime, Long expiresIn, String hostName) {
		super();
		this.accessToken = accessToken;
		this.tokenCreationTime = tokenCreationTime;
		this.expiresIn = expiresIn;
		this.hostName = hostName;
	}

	synchronized String getAccessToken() {
		return accessToken;
	}

	synchronized void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	synchronized Date getTokenCreationTime() {
		return tokenCreationTime;
	}

	synchronized void setTokenCreationTime(Date tokenCreationTime) {
		this.tokenCreationTime = tokenCreationTime;
	}

	synchronized Long getExpiresIn() {
		return expiresIn;
	}

	synchronized void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	synchronized JsonNode getApplicationJsonNode() {
		return applicationJsonNode;
	}

	synchronized void setApplicationJsonNode(JsonNode json) {
		this.applicationJsonNode = json;
	}

}