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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.keich.lync.LyncApi.UserHrefChangeHost;

public class LyncClient {
	private LyncApi lyncApi;
	private Logger log = Logger.getLogger(LyncClient.class.getName());

	public LyncClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		super();
		lyncApi = new LyncApi();
	}

	public void Connect() throws Exception {
		log.info("Enter");
		lyncApi.conAutoDiscoveryHref();
		lyncApi.conAuthenticate();
		boolean appisCreated = false;
		while (!appisCreated) {
			try {
				lyncApi.createApplication();
				appisCreated = true;
			} catch (UserHrefChangeHost e) {
				lyncApi.conAuthenticate();

			}
			Thread.sleep(1000);
		}
		
		lyncApi.startEventThread();
		log.info("Leave");
	}

	public void SendMessgae(String subject, String message) {
		log.info("Enter");
		try {
			lyncApi.createConversations(subject, message);
		} catch (UnsupportedOperationException | IOException e) {
			log.logp(Level.SEVERE, "LyncClient", "SendMessgae", "Error while try send message", e);
		}
		log.info("Leave");
	}

}
