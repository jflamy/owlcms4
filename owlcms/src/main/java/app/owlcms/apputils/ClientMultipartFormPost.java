/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * original code from
 * https://stackoverflow.com/questions/2469451/upload-files-from-java-client-to-a-http-server
 *******************************************************************************/

package app.owlcms.apputils;

import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class ClientMultipartFormPost {

	static Logger logger = (Logger) LoggerFactory.getLogger(ClientMultipartFormPost.class);

	public static void sendStream(URL u, InputStream is, ContentType ct) throws Exception {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httppost = new HttpPost(u.toURI());

//            FileBody bin = new FileBody(new File("E:\\meter.jpg"));
//            StringBody comment = new StringBody("A binary file of some kind", ContentType.TEXT_PLAIN);

			HttpEntity reqEntity = MultipartEntityBuilder.create()
			        .addPart("zip", new InputStreamBody(is, ct))
			        .build();

			httppost.setEntity(reqEntity);

			System.out.println("executing request " + httppost.getRequestLine());
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				logger.info("{}", response.getStatusLine());
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					logger.debug("Response content length: {}", resEntity.getContentLength());
				}
				EntityUtils.consume(resEntity);
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}
	}

}