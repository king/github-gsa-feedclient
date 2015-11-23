// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 *  Web Form to feed GSA:
 * 
 *	<form enctype="multipart/form-data" method=POST
 *     		action="http://<APPLIANCE-HOSTNAME>:19900/xmlfeed">
 *      	<p>Name of datasource:
 *		        <input type="text" name="datasource">
 *		        <br>(No spaces or non alphanumeric characters)
 *	      	</p>
 *		        <p>Type of feed:
 *		        <input type="radio" name="feedtype" value="full" checked>Full
 *		        <input type="radio" name="feedtype" value="incremental">Incremental
 *		        <input type="radio" name="feedtype" value="metadata-and-url">Metadata and URL
 *	        </p>
 *	      	<p>
 *		        XML file to push:
 *		        <input type="file" name="data">
 *	      	</p>
 *		    <p>
 *		        <input type="submit" value=">Submit<">
 *		    </p>
 *	</form>	 
 * 
 * @see https://www.google.com/support/enterprise/static/gsa/docs/admin/70/gsa_doc_set/feedsguide/feedsguide.html#1075077
 * 
 * @author King Engineering Systems & Support
 *
 */
public class GSAFeedForm {
	
	private static Logger log = LoggerFactory.getLogger(GSAFeedForm.class);
	
	private static String FEEDTYPE_PARAM   = "feedtype";
	private static String DATASOURCE_PARAM = "datasource";
	private static String XMLFILE_PARAM    = "data";
	
	// GSA XML Feed URL
	private static int PORT           = 19900;
	private static String FEED_URL    = "/xmlfeed";
	
	private String gsaServer;
	
	public GSAFeedForm(String gsaServer) {
		super();
		this.gsaServer = gsaServer + ":" + PORT + FEED_URL;
	}
 

	/**
	 * It sends XML with feeds to GSA using proper form:
	 * 
	 * @param xmlDocument  GSADocumentFormatter containing XML Document information
	 * 
	 * @return Was posted Ok?
	 */
	public boolean sendForm(GSADocumentFormatter xmlDocument){
		boolean bSent = false;
			
		HttpClient client = HttpClientBuilder.create().build();

		try {
			HttpPost postPageRequest;
			postPageRequest = new HttpPost(this.gsaServer);
			InputStream is = xmlDocument.writeToInputStream();
			
			// Add Form parameters
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			
			builder.addTextBody(FEEDTYPE_PARAM, xmlDocument.getFeedType(),
					ContentType.TEXT_PLAIN);		
			builder.addTextBody(DATASOURCE_PARAM, xmlDocument.getDatasource(),
					ContentType.TEXT_PLAIN);			
			builder.addBinaryBody(XMLFILE_PARAM, is);
			HttpEntity multipartEntity = builder.build();
				    
		    postPageRequest.setEntity(multipartEntity);
		    	    
			HttpResponse postPageResponse = client.execute(postPageRequest);
			int status = postPageResponse.getStatusLine().getStatusCode();
			
			if (!(bSent = (status == 200))) 
				log.error("GitHub API (" + this.gsaServer + ") returned " + status);
			else
				log.info("XML For datasource '" +xmlDocument.getDatasource()+ "' and FeedType '"+
						xmlDocument.getFeedType()+"' was posted successfully to GSA!!");
			
		} catch (Exception e) {
			log.error("Exception "+ e.getMessage() + " in HTTP request " + this.gsaServer);
		}
		return bSent;
	 }
}
