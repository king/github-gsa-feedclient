// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/**
 * It handles the XML to submit to GSA containing Records we want GSA to index.
 * See more info here: 
 * 	https://www.google.com/support/enterprise/static/gsa/docs/admin/70/gsa_doc_set/feedsguide/feedsguide.html#1073054
 * 
 * Records have the following information:
 *  	url =  The unique identifier for the document. This is the URL used by the search appliance when crawling and indexing the document.
 *      displayurl = (optional) The URL that should be provided in search results for a document.
 *      mimetype = This attribute tells the system what kind of content to expect from the content element.
 *      
 * Optionally it can provide the Contents so we don't need GSA to crawl provided URL     
 *      
 * 		content = (optional)  Content feed 
 * 
 * And the following metadata:
 * 			owner = Repo Owner Name
 *          owner Type = Repo Owner Type (User or Organization)
 *          repoName = Repo Name
 *          language = Repo Language
 *          forks = Number of Forks
 *          stargazers = Number of Stargazers
 *          repolastupdated = Date of last Update
 *          recordType = Record Type (User, Org, Repo or File)
 * 
 * @author King Engineering Systems & Support
 *
 */
public class GSADocumentFormatter {

	private static Logger log = LoggerFactory.getLogger(GSADocumentFormatter.class);
	
	// GSA XML Doc Type
	private static final String GSA_XML_PUBLIC_ID  = "-//Google//DTD GSA Feeds//EN";
	private static final String GSA_QUALIFIED_NAME = "gsafeed";
	
	// GSA Feed Types
	public static String FEED_TYPE_METADATA_AND_URL = "metadata-and-url";
	public static String FEED_TYPE_INCREMENTAL      = "incremental";
	public static String FEED_TYPE_FULL             = "full";
	
	// XML Elements
	private static String GSAFEED_ELEMENT     = "gsafeed";
	private static String HEADER_ELEMENT      = "header";
	private static String DATASOURCE_ELEMENT  = "datasource";
	private static String FEEDTYPE_ELEMENT    = "feedtype";
	private static String GROUP_ELEMENT       = "group";
	private static String RECORD_ELEMENT      = "record";
	private static String METADATA_ELEMENT    = "metadata";
	private static String META_ELEMENT        = "meta";
	private static String CONTENT_ELEMENT     = "content";
	
	private static String URL_ATTR            = "url";
	private static String DISPLAY_URL_ATTR    = "displayurl";
	private static String MIME_TYPE_ATTR      = "mimetype";
	private static String NAME_ATTR           = "name";
	private static String CONTENT_ATTR        = "content";

	// King Metadata GSA Keys for Github repositories
	private static String OWNER_METADATA       = "owner";
	private static String OWNER_TYPE_METADATA  = "ownerType";
	private static String REPO_NAME_METADATA   = "reponame";
	private static String FORKS_METADATA       = "forks";
	private static String STARGAZERS_METADATA  = "stargazers";
	private static String LAST_UPDATE_METADATA = "repolastupdated";
	private static String LANGUAGE_METADATA    = "language";
	private static String RECORD_TYPE_METADATA = "recordType";
	
	// Values for "Record Type" metadata. Used in GSA to display different styles
	private static String RECORD_TYPE_REPO     = "Repo";
	private static String RECORD_TYPE_ORG      = "Org";
	private static String RECORD_TYPE_USER     = "User";
	private static String RECORD_TYPE_FILE     = "File";
	
	// Mime types used for Contents provided here
	private static String MIME_HTML     = "text/html";
	private static String MIME_PLAIN    = "text/plain";
	
	// XML Document
	private Document  feederDoc = null;
	// Group XML element
	private Element   groupElement = null;
	// GSA Datasource
	private String    datasource;
	// GSA Feed Type (https://www.google.com/support/enterprise/static/gsa/docs/admin/70/gsa_doc_set/feedsguide/feedsguide.html#1074377)
	private String    feedType;
	// Dates in XML should be RFC822 formatted
	private SimpleDateFormat rfc822Format;
	
	/**
	 * This class creates the XML to send to GSA. It will contain feeding information for GSA
	 * 
	 * @param datasource  GSA category for that contents
	 * @param feedType    GSA Feed Type
	 * 
	 * @throws ParserConfigurationException  Error handling XML
	 * 
	 */
	public GSADocumentFormatter(String datasource, String feedType) throws ParserConfigurationException {
		super();

		this.feedType = feedType;
		this.datasource = datasource;
		
		// Dates for GSA should be in RFC822 (Mon, 15 Nov 2004 04:58:08 GMT).
		rfc822Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Root element
		feederDoc = docBuilder.newDocument();
		Element rootElement = feederDoc.createElement(GSAFEED_ELEMENT);
		feederDoc.appendChild(rootElement);
		
		// Header
		Element header = feederDoc.createElement(HEADER_ELEMENT);
		rootElement.appendChild(header);
		// Header - Data Source
		Element datasourceElement = feederDoc.createElement(DATASOURCE_ELEMENT);
		datasourceElement.appendChild(feederDoc.createTextNode(datasource));
		header.appendChild(datasourceElement);

		// Header - Feed Type
		Element feedTypeElement = feederDoc.createElement(FEEDTYPE_ELEMENT);
		feedTypeElement.appendChild(feederDoc.createTextNode(feedType));
		header.appendChild(feedTypeElement);
		
		// Group (just create it and ready to include Records)
		groupElement = feederDoc.createElement(GROUP_ELEMENT);
		rootElement.appendChild(groupElement);		
	}
	
	
	/**
	 * It returns GSA Datasource associated with this XML
	 * 
	 * @return GSA Datasource
	 */
	public String getDatasource() {
		return datasource;
	}

	/**
	 * It returns Feed Type of this Document:
	 *    incremental
	 *    full
	 *    metadata-and-url
	 * 
	 * @return
	 */
	public String getFeedType() {
		return feedType;
	}

	/**
	 * It adds a Owner Record to GROUP. 
	 * Owner Record provide the Owner Description as content to index and
	 * the Owner HTML as identifier 
	 * So if provided owner has no description then it's not added to XML
	 *  
	 * @param owner  GithubOwner Object with owner information
	 * 
	 * @return Was it added to XML?
	 */
	public boolean addOwnerRecord(String ownerURLId, GithubOwner owner){
		boolean bAdded = false;
		// If it exists an Owner Description
		String description = owner.getDescription();
		if (description != null && description.trim().length() > 0) {
			if (owner.getUrl() != null && owner.getUrl().trim().length() > 0) {
				// Add Record element
				Element record = getRecordElement(ownerURLId, owner.getUrl(), MIME_PLAIN);
				if (record != null) {
					Element contenElement = getContentElement(description);
					if (contenElement != null) // Add Content Element
						record.appendChild(contenElement);
					// Add Metadata element
					record.appendChild(getMetadataElement(owner));
					// Add record to Group
					this.groupElement.appendChild(record);
					bAdded = true;
				}
			}
		}
		return bAdded;
	}
	
	/**
	 * It adds a Repository Record to GROUP. 
	 * Repository Record provide the Repo Description as content to index and
	 * the Repo HTML as identifier 
	 * So if provided repo has no description then it's not added to XML
	 *  
	 * @param repo  GithubRepo Object with repository information
	 * 
	 * @return Was it added to XML?
	 */
	public boolean addRepositoryRecord(String repoURLId, GithubRepo repo){
		boolean bAdded = false;
		// If it exists a Repo Description
		String description = repo.getDescription();
		if (description != null && description.trim().length() > 0) {
			// Add Record element
			Element record = getRecordElement(repoURLId, repo.getHtmlURL(), MIME_PLAIN);
			if (record != null) {
				Element contenElement = getContentElement(description);
				if (contenElement != null) // Add Content Element
					record.appendChild(contenElement);
				// Add Metadata element
				record.appendChild(getMetadataElement(repo, true));
				// Add record to Group
				this.groupElement.appendChild(record);
				bAdded = true;
			}
		}
		return bAdded;
	}
	/**
	 * It adds a Repository REAME.md Record to GROUP. 
	 * Repo Readme Record in XML just provides the README.md URL because 
	 * we want GSA to crawl its contents.
	 *
	 *  @param repo         GithubRepo Object with repository information
	 * 
	 * @return Was it added to XML?
	 */
	public boolean addRepositoryReadmeRecord(GithubRepo repo){
		boolean bAdded = false;
		// If Repository has a README file then ...
		if (repo.getReadme() != null) {
			// Add Record element
			Element record = getRecordElement(repo.getReadme().getRawURL(), 
					repo.getReadme().getHtmlURL(), MIME_HTML);
			// Just add Metadata  (No Contents!!)
			if (record != null) { 
				record.appendChild(getMetadataElement(repo, false));
				// Add record to Group
				this.groupElement.appendChild(record);
				bAdded = true;			
			}
		}
		return bAdded;
	}
	
	/**
	 * It returns record XML Element with given attributes
	 * 
	 * <record url="My_URL" displayurl="My_URL_to_Display" mimetype="mime/type">
	 * </record>
	 * 
	 * @param url           Unique identifier for the document  (mandatory)      
	 * @param displayurl    URL to display in results (optional) 
	 * @param mimetype      Mime type of contents ("text/plain" by default in case of null)
	 * 
	 * @return XML Element or NULL if URL was not informed
	 */
	private Element getRecordElement(String url, String displayurl, String mimetype){
		Element recordElement = null;
		if (mimetype == null)
			mimetype = MIME_PLAIN;
		if (url == null)
			url = displayurl;
		
		if (url != null) {
			recordElement = feederDoc.createElement(RECORD_ELEMENT);
				
			// Set URL Attribute
			Attr urlAttr = feederDoc.createAttribute(URL_ATTR);
			urlAttr.setValue(url);
			recordElement.setAttributeNode(urlAttr);
			
			// Set Display URL Attribute
			if (displayurl != null) {
				Attr displayUrlAttr = feederDoc.createAttribute(DISPLAY_URL_ATTR);
				displayUrlAttr.setValue(displayurl);
				recordElement.setAttributeNode(displayUrlAttr);
			}
			
			// Set MIME_TYPE Attribute
			Attr mimeAttr = feederDoc.createAttribute(MIME_TYPE_ATTR);
			mimeAttr.setValue(mimetype);
			recordElement.setAttributeNode(mimeAttr);
		}
		return recordElement;
	}
	
	/**
	 * It returns record Content XML Element with given contents as CData
	 * 
	 * <content><![CDATA[Here goes my contents]]></content>
	 * 
	 * @param content    Record Content (mandatory)         
	 * 
	 * @return XML Element or NULL if content was not informed
	 */
	private Element getContentElement(String content){
		Element contentElement = null;
		
		if (content != null) {
			contentElement = feederDoc.createElement(CONTENT_ELEMENT);
				
			CDATASection cdata = feederDoc.createCDATASection(content);
			contentElement.appendChild(cdata);			
		}
		return contentElement;
	}
	
	/**
	 * It returns record Metadata XML Element with given owner information into 
	 * proper meta elements.
	 *   <metadata>
	 *     	 <meta name="MyMetaKey" content="MyMetaValue"/>
	 *       ....
	 *   </metadata>
	 * 
	 * @param owner    GithubOwner Object with owner information 
	 * 
	 * @return XML Element 
	 */
	private Element getMetadataElement(GithubOwner owner){
		Element metadataElement = null;
		
		metadataElement = feederDoc.createElement(METADATA_ELEMENT);
		// Owner
		addMetaElement(metadataElement, OWNER_METADATA, owner.getName());
		// Owner Type
		addMetaElement(metadataElement, OWNER_TYPE_METADATA, owner.getType());
		// Record Type
		addMetaElement(metadataElement, RECORD_TYPE_METADATA, 
				owner.isOrganization()?RECORD_TYPE_ORG:RECORD_TYPE_USER);
		
		return metadataElement;
	}
	
	/**
	 * It returns record Metadata XML Element with given repo information into 
	 * proper meta elements.
	 *   <metadata>
	 *     	 <meta name="MyMetaKey" content="MyMetaValue"/>
	 *       ....
	 *   </metadata>
	 * 
	 * @param repo               GithubRepo Object with all repository information
	 * @param bRepositoryRecord  Is it a Repository Record or a ReadMe Record?
	 * 
	 * @return XML Element 
	 */
	private Element getMetadataElement(GithubRepo repo, boolean bRepositoryRecord){
		Element metadataElement = null;
		
		metadataElement = feederDoc.createElement(METADATA_ELEMENT);
		// Owner
		addMetaElement(metadataElement, OWNER_METADATA, repo.getOwner().getName());
		// Owner Type
		addMetaElement(metadataElement, OWNER_TYPE_METADATA, repo.getOwner().getType());
		// Repo Name
		addMetaElement(metadataElement, REPO_NAME_METADATA, repo.getName());
		// Repo Last Update (prevent date format changes capturing parsing exceptions) 
		try {
			addMetaElement(metadataElement, LAST_UPDATE_METADATA, rfc822Format.format(repo.getLastUpdate()));
		} catch(Exception e) {}
		// Repo Language
		addMetaElement(metadataElement, LANGUAGE_METADATA, repo.getLanguage());
		// Repo Forks
		addMetaElement(metadataElement, FORKS_METADATA, Integer.toString(repo.getForks()));
		// Repo Stargazers
		addMetaElement(metadataElement, STARGAZERS_METADATA, Integer.toString(repo.getStargazers()));
		// Record Type
		addMetaElement(metadataElement, RECORD_TYPE_METADATA, 
				bRepositoryRecord?RECORD_TYPE_REPO:RECORD_TYPE_FILE);
		
		return metadataElement;
	}
	
	/**
	 * Auxiliar method to add a Meta element to parent Node (Metadata element)
	 * only if it exists.
	 * WARNING!! GSA Can't deal with "Metas" without value so if value is NULL
	 * it will not be added to XML.
	 * 
	 * @param parentElement Parent Element  (mandatory)
	 * @param key           Meta name  (mandatory)
	 * @param value         Meta content (not included in case of null)
	 * 
	 * @return Was the element added?
	 */
	private boolean addMetaElement(Element parentElement, String key, String value){
		if (parentElement != null && key!=null && value!=null) {
			Element metaElement = getMetaElement(key, value);
			if (metaElement != null) {
				parentElement.appendChild(metaElement);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * It returns Metadata meta XML Element with given name and contents (i.e. key and value)
	 * GSA does not process a metadata element containing a content attribute with an empty string so
	 * in case of empty or null value it returns NULL
	 * 
	 * <meta name="MyMetaKey" content="MyMetaValue"/>
	 * 
	 * @param key    Meta name  (mandatory)
	 * @param value  Meta content 
	 * 		         
	 * 
	 * @return XML Element or NULL if key was not informed
	 */
	private Element getMetaElement(String key, String value){
		Element metaElement = null;
		
		if ((key != null && key.trim().length() > 0) && 
			(value != null && value.trim().length() > 0)) {
			metaElement = feederDoc.createElement(META_ELEMENT);

			// Set Name-Content Attributes		
			Attr contentAttr = feederDoc.createAttribute(CONTENT_ATTR);
			contentAttr.setValue(value == null? "":value);
			metaElement.setAttributeNode(contentAttr);
			
			Attr nameAttr = feederDoc.createAttribute(NAME_ATTR);
			nameAttr.setValue(key);
			metaElement.setAttributeNode(nameAttr);	
		}
		return metaElement;
	}
	
	/**
	 * It returns Document Transformer with proper Doc Type
	 * 
	 * @param bStandAlone  Header key "standalone"
	 * @param bIndent      Header key "indent"
	 * 
	 * @return Document Transformer
	 * 
	 * @throws TransformerConfigurationException Exception accessing Transformer factory
	 */
	private Transformer getDocTransformer(boolean bStandAlone, boolean bIndent) throws TransformerConfigurationException {	
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		
		DOMImplementation domImpl = feederDoc.getImplementation();
		DocumentType doctype = domImpl.createDocumentType(GSA_QUALIFIED_NAME, GSA_XML_PUBLIC_ID, "");
		
		transformer.setOutputProperty(OutputKeys.STANDALONE, bStandAlone?"yes":"no");
		transformer.setOutputProperty(OutputKeys.INDENT, bIndent?"yes":"no");
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
		transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		
		return transformer;
	}
	
	/**
	 * Writes XML Document to given Input Stream
	 * 
	 * @param is  InputStream where to write XML Content
	 * 
	 * @throws TransformerException  Error transforming Document into InputStream
	 */
	public InputStream writeToInputStream() throws TransformerException {
		Transformer transformer = getDocTransformer(true, false);	
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		DOMSource source = new DOMSource(feederDoc);
		StreamResult result = new StreamResult(outputStream);
		transformer.transform(source, result);
		InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
		try {
			outputStream.close();
		} catch (IOException e) {
			log.warn("Exception " + e.getMessage() + " closing OutputStream");
		}
		return is;
	}
	
	/**
	 * Writes XML Document to given File
	 * 
	 * @param file  Full path to file
	 * 
	 * @throws TransformerException  Error transforming Document into file
	 */
	public void writeToFile(String file) throws TransformerException {	
		Transformer transformer = getDocTransformer(true, true);	
		DOMSource source = new DOMSource(feederDoc);
		StreamResult result = new StreamResult(new File(file));
		transformer.transform(source, result);
		log.info("XML For datasource '" + this.getDatasource()+ "' and FeedType '"+
				this.getFeedType()+"' was written successfully to "+file);
	}
	
	/**
	 * Writes XML Document to console
	 * 
	 * @throws TransformerException  Error transforming Document into file
	 */
	public void writeToConsole() throws TransformerException {	
		Transformer transformer = getDocTransformer(true, true);	
		DOMSource source = new DOMSource(feederDoc);
	    StreamResult result = new StreamResult(System.out);
		transformer.transform(source, result);
		log.info("XML For datasource '" + this.getDatasource()+ "' and FeedType '"+
				this.getFeedType()+"' was written successfully to console");
	}
}
