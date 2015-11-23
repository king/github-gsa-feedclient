// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * GSA Feed Client to retrieve information from GitHub
 * See GSA Feed documentation here:
 *    https://www.google.com/support/enterprise/static/gsa/docs/admin/70/gsa_doc_set/feedsguide/feedsguide.html#1074230
 * 
 * This Client is a WEB Crawler, i.e. "metadata-and-url" feed type
 * It retrieves from GitHub only the URL for "README.md" files (in RAW format to easy the GSA crawling).
 * For each README.md entry it gathers the following metadata:
 * 
 *  	- Owner              (Repository Owner)
 *      - Owner Type         (User or Organization)
 *      - Repo name          (Repository name)
 *      - Repo Description   (Repository Description)
 *      - Last Update        (Date of last change in RFC822 format)
 *      - Language           (Project language)
 *      - Forks              (Num. of Repository forks)
 *      - Stargazers         (Num. of Repository watchers)
 *      - RecordType         (User, Org, Repo or File)
 * 
 * 
 * IMPORTANT!!
 * 	It's fastest and less demanding to retrieve All Repositories in one single API call (/repositories)
 *  but we need to have the name of the "default branch" (usually "master") and such info not returned in that call.
 *  So, in order to populate GSA Record we must execute:
 *     1 - Get All Users
 *     2 - Get All Organizations
 *     3 - For each owner (Users AND Organizations) get ALL their Repos
 *         Now we have all needed information 
 * 
 * @author King Engineering System & Support
 *
 */
public class GithubGSAFeedClient {

	private static Logger log = LoggerFactory.getLogger(GithubGSAFeedClient.class);
	
	public GithubGSAFeedClient(String gsaDataSource, String githubServer, String gsaServer) throws ParserConfigurationException {
		super();
		
		if (gsaServer != null && gsaServer.endsWith("/"))
			gsaServer.substring(0, gsaServer.length()-1);
		
		if (githubServer != null && !githubServer.endsWith("/"))
			githubServer+="/";
		
		// API Handler for Github
		GithubAPIClient githubAPIClient = new GithubAPIClient(githubServer);
		
		// XML Document with README.md URLs (we want GSA to crawl that content)
		GSADocumentFormatter gsaURLDocument = new GSADocumentFormatter(gsaDataSource,
				GSADocumentFormatter.FEED_TYPE_METADATA_AND_URL);
		// XML Document with Owners & Repos URLs. We're going to provide content as well because we don't want GSA to crawl it
		GSADocumentFormatter gsaContentDocument = new GSADocumentFormatter(gsaDataSource,
				GSADocumentFormatter.FEED_TYPE_INCREMENTAL);

		
		// Get all Users and Organizations from Github
		ArrayList<GithubOwner> gitHubOwners = githubAPIClient.getUsers();
		gitHubOwners.addAll(githubAPIClient.getOrganizations());

        log.info("Fetching repositories from " + gitHubOwners.size() + " owners");
		
		// Get All repositories for each Owner (User and Organizations)
		ArrayList<GithubRepo> githubRepoList = new ArrayList<GithubRepo>();
		for(GithubOwner owner: gitHubOwners) 
			githubRepoList.addAll(githubAPIClient.getRepositories(owner));	
		
		log.info( githubRepoList.size() + " Repositories fetched from Github");
			
		// Add Owner Records (Owner's description) into "static content" XML
		for(GithubOwner owner: gitHubOwners)
			gsaContentDocument.addOwnerRecord(githubAPIClient.getFakeOwnerURL(owner) ,owner);
		
		// For each repo ...
		for(GithubRepo repo: githubRepoList) {
			// Add Repository Record (Repo description) into "static content" XML
			gsaContentDocument.addRepositoryRecord(githubAPIClient.getFakeRepositoryURL(repo), repo);
			// And Repository README into "dinamic" metadata and url XML
			gsaURLDocument.addRepositoryReadmeRecord(repo);
		}

		// Create the Web Form for given server
		GSAFeedForm gsaForm = new GSAFeedForm(gsaServer);
		// Send information for "Web Feed" and "Incremental Feed"
		gsaForm.sendForm(gsaURLDocument);
		gsaForm.sendForm(gsaContentDocument);	
		
/*		
       // EXAMPLE to Write XMLs to a Document 
       try {
			gsaURLDocument.writeToFile("/Users/admin/Documents/tmp/GHE_URL_Content.xml");
			gsaContentDocument.writeToFile("/Users/admin/Documents/tmp/GHE_Static_Content.xml");
		} catch (TransformerException e) {
		}
	*/	
	}


	public static void main(String[] args) {
		if (args.length > 2) {
			try {
				new GithubGSAFeedClient(args[0], args[1], args[2]);
			} catch (ParserConfigurationException e) {
				log.error("Exception parsing document " + e.getMessage());
			}
		} 
		else {
			log.error("Error in initial parameters " + args);
			log.warn("This app needs 3 params: GSADataSource, GitHub_Server and GSA_Server");
			System.out.println("This app needs 3 params: GSADataSource, GitHub_Server and GSA_Server");
			System.exit(1);
		}
	}
}
