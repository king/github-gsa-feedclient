// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helper class to gather information through GitHub API 
 * 
 * @author King Engineering Systems & Support
 *
 */
public class GithubAPIClient {

	
	private static Logger log = LoggerFactory.getLogger(GithubAPIClient.class);
	
	// Return Code OK for HTML
	private static final int    RETURN_CODE_OK = 200;
	
	// GitHub API Keys
	private static String OWNER_LOGIN_KEY      = "login";
	private static String OWNER_REPO_URL_KEY   = "repos_url";
	private static String OWNER_DESC_KEY       = "description";
	private static String OWNER_HTML_URL_KEY   = "html_url";
	
	private static String REPO_DESC_KEY        = "description";
	private static String REPO_NAME_KEY        = "name";
	private static String REPO_HTML_URL_KEY    = "html_url";
	private static String REPO_CONTENT_URL_KEY = "contents_url";
	private static String REPO_LANGUAGE_KEY    = "language";
	private static String REPO_UPDATED_KEY     = "updated_at";
	private static String REPO_STARGAZERS_KEY  = "stargazers_count";
	private static String REPO_FORKS_KEY       = "forks_count";
	private static String REPO_DEF_BRANCH_KEY  = "default_branch";
	
	private static String README_SIZE_KEY      = "size";
	private static String README_HTML_URL_KEY  = "html_url";
	private static String README_RAW_URL_KEY   = "download_url";
	
	
	// GitHub API methods (https://developer.github.com/v3/)
	private static String API_PREFIX    = "api/v3/";
	private static String PAGINATION    = "?per_page=10000";  // Prevent pagination
	private static String GET_USERS_API = API_PREFIX + "users" + PAGINATION;
	private static String GET_ORGS_API  = API_PREFIX + "organizations" + PAGINATION;

	
	// Github URL paths
	private static String CONTENTS_XTRA = "{+path}";
	private static String DESCRIPTION   = "description";
	private static String README_FILE   = "README.md";
		
	private String baseGHUrl;
	private SimpleDateFormat dateFormat;

	public GithubAPIClient(String baseGHUrl) {
		super();
		this.baseGHUrl = baseGHUrl;
		// Dates in RFC822 format "2015-08-20T15:45:46Z" always UTC
		dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	}
	
	/**
	 * Owners description is included in GSA but we don't want GSA to crawl them.
	 * To prevent this GSA crawling we provide the contents but ALSO a fake URL to act as
	 * this document Unique Identifier.
	 * We're not using the actual URL because it has lots of links that we want to keep away from GSA
	 * 
	 * @param owner  GitHubOwner Object with owner info
	 * 
	 * @return Non-existing URL to act as GSA Document Unique Identifier 
	 */
	public String getFakeOwnerURL(GithubOwner owner){
		return getFakeOwnerURL(owner.getName());
	}
	
	/**
	 * Owners description is included in GSA but we don't want GSA to crawl them.
	 * To prevent this GSA crawling we provide the contents but ALSO a fake URL to act as
	 * this document Unique Identifier.
	 * We're not using the actual URL because it has lots of links that we want to keep away from GSA
	 * 
	 * @param owner  Owner name
	 * 
	 * @return Non-existing URL to act as GSA Document Unique Identifier 
	 */
	public String getFakeOwnerURL(String owner){
		StringBuffer sb = new StringBuffer(this.baseGHUrl).append(DESCRIPTION).append("/").append(owner);
		return sb.toString();
	}
	
	/**
	 * Repositories description is included in GSA but we don't want GSA to crawl them.
	 * To prevent this GSA crawling we provide the contents but ALSO a fake URL to act as
	 * this document Unique Identifier.
	 * We're not using the actual URL because it has lots of links that we want to keep away from GSA
	 * 
	 * @param repo  GitHubRepository Object with repository info
	 * 
	 * @return Non-existing URL to act as GSA Document Unique Identifier 
	 */
	public String getFakeRepositoryURL(GithubRepo repo){
		return getFakeRepositoryURL(repo.getOwner().getName(), repo.getName());
	}
	
	/**
	 * Repositories description is included in GSA but we don't want GSA to crawl them.
	 * To prevent this GSA crawling we provide the contents but ALSO a fake URL to act as
	 * this document Unique Identifier.
	 * We're not using the actual URL because it has lots of links that we want to keep away from GSA
	 * 
	 * @param owner   Repository owner's name
	 * @param repo    Repository name
	 * 
	 * @return Non-existing URL to act as GSA Document Unique Identifier 
	 */
	public String getFakeRepositoryURL(String owner, String repo){
		StringBuffer sb = new StringBuffer(this.baseGHUrl).append(DESCRIPTION).append("/").append(owner).append("/").append(repo);
		return sb.toString();
	}
	
	/**
	 * It submits an HTTP Request to given URL and returns contents into 
	 * a JSON Array Object
	 * 
	 * @param githubApiUrl GitHub API URL
	 * 
	 * @return JSON Array Object with retrieved information
	 */
	private JSONArray getGitHubAPIArrayInformation(String githubApiUrl){
		String httpInfo = getGitHubAPIInformation(githubApiUrl);
		if (httpInfo != null) 
			return new JSONArray(httpInfo);
		return null;
	}
	
	/**
	 * It submits an HTTP Request to given URL and returns contents into 
	 * a single JSON Object
	 * 
	 * @param githubApiUrl GitHub API URL
	 * 
	 * @return JSON Object with retrieved information
	 */
	private JSONObject getGitHubAPIObjectInformation(String githubApiUrl){
		String httpInfo = getGitHubAPIInformation(githubApiUrl);
		if (httpInfo != null) 
			return new JSONObject(httpInfo);
		return null;
	}
	
	/**
	 * It submits an HTTP Request to given URL and returns contents into String
	 * 
	 * @param githubApiUrl GitHub API URL
	 * 
	 * @return String "JSON-formatted" with received information
	 */
	private String getGitHubAPIInformation(String githubApiUrl){
    	HttpClient client = HttpClientBuilder.create().build();

		String pageObj = null;
		HttpEntity pageEntity = null;
		int status = -1;

		try {
			HttpGet getPageRequest;
			getPageRequest = new HttpGet(githubApiUrl);

			HttpResponse getPageResponse = client.execute(getPageRequest);
			pageEntity = getPageResponse.getEntity();

			pageObj = IOUtils.toString(pageEntity.getContent());

			status = getPageResponse.getStatusLine().getStatusCode();
		
			if (status != RETURN_CODE_OK) {
				pageObj = null;
				log.error("GitHub API (" + githubApiUrl + ") returned " + status);
			}
		} catch (Exception e) {
			log.error("Exception "+ e.getMessage() + " in HTTP request " + githubApiUrl);
		}
		finally {
			if (pageEntity != null) {
				try {
					EntityUtils.consume(pageEntity);
				} catch (IOException e) {}
			}
		}
		return pageObj;
	}
	
	/**
	 * Gets list of Users from given GitHub Server.
	 * 
	 * @return ArrayList<githubOwner> containing User information
	 */
	public ArrayList<GithubOwner> getUsers(){
    	return getOwners(baseGHUrl + GET_USERS_API, false);
	}
	
	/**
	 * Gets list of Organizations from given GitHub Server.
	 * 
	 * @return ArrayList<githubOwner> containing Organization Information
	 */
	public ArrayList<GithubOwner> getOrganizations(){
    	return getOwners(baseGHUrl + GET_ORGS_API, true);
	}
	
	/**
	 * Gets list of Owners from given GitHub Server.
	 * Why we need to set the OWNER TYPE and not to get it from returned JSON?
	 * ...because "/users" API returns that value but "/organizations" don't
	 * 
	 * @param githubURL       GitHub API URL to retrieve info
	 * @param bOrganization   Owner Type to store (User or Organization)
	 * 
	 * @return ArrayList<githubOwner> containing Owner information
	 */
	private ArrayList<GithubOwner> getOwners(String githubURL, boolean bOrgzanization){
		ArrayList<GithubOwner> ownerList = new ArrayList<GithubOwner>();
		JSONArray jsOwnerArray = getGitHubAPIArrayInformation(githubURL);
		
		// If it was OK then get Owner information and store it in a githubOnwer object 
		if (jsOwnerArray != null) {
			JSONObject element;
			GithubOwner ownerItem;
			for (int index = 0; index < jsOwnerArray.length(); index++){
				try {
					element = jsOwnerArray.getJSONObject(index);
					ownerItem = new GithubOwner(
							element.getString(OWNER_LOGIN_KEY), 
							bOrgzanization,
							element.getString(OWNER_REPO_URL_KEY));			
					ownerItem.setDescription(getJSONStringValue(element, OWNER_DESC_KEY));
					// HTML Url is only present in Users, so we need to come up with the
					// Organization URL by ourselves (base_url/{OrgName})
					if (element.has(OWNER_HTML_URL_KEY)) 
						ownerItem.setUrl(element.getString(OWNER_HTML_URL_KEY));
					else 
						ownerItem.setUrl(this.baseGHUrl + ownerItem.getName());
					ownerList.add(ownerItem);
				} catch(Exception e) {
					log.error("Exception "+ e.getMessage() + " handling Owner List " + jsOwnerArray);
				}
			}	
		}
    	return ownerList;
	}
	
	/**
	 * Gets list of Repositories for given Owner from configured GitHub Server.
	 * 
	 * @param owner GithubOwner Object providing Name, type and Repo URL
	 * 
	 * @return ArrayList<GithubRepository> containing Organization Names
	 */
	public ArrayList<GithubRepo> getRepositories(GithubOwner owner){
		ArrayList<GithubRepo> repoList = new ArrayList<GithubRepo>();
		
		// Retrieve repositories information using provided URL
		if (owner.getRepoURL() != null && owner.getRepoURL().trim().length() > 0) {
			JSONArray jsRepoArray = getGitHubAPIArrayInformation(owner.getRepoURL());
			
			if (jsRepoArray != null) {
				JSONObject element = null;
				String tmpURL;
				GithubRepo repo = null;
				for (int index = 0; index < jsRepoArray.length(); index++){
					element = jsRepoArray.getJSONObject(index);
					try {
						repo = new GithubRepo(owner);
						repo.setName(element.getString(REPO_NAME_KEY));
						repo.setDescription(getJSONStringValue(element, REPO_DESC_KEY));
						repo.setHtmlURL(element.getString(REPO_HTML_URL_KEY));
						tmpURL = getJSONStringValue(element, REPO_CONTENT_URL_KEY);
						repo.setContentsURL(tmpURL);
						if (tmpURL != null) {
							// Get the URL to the README file
							tmpURL = tmpURL.replace(CONTENTS_XTRA, README_FILE);
							repo.setReadme(getReadmeFile(tmpURL));
						}
						repo.setLanguage(getJSONStringValue(element, REPO_LANGUAGE_KEY));
						repo.setStargazers(element.getInt(REPO_STARGAZERS_KEY));
						repo.setForks(element.getInt(REPO_FORKS_KEY));
						repo.setDefaultBranch(element.getString(REPO_DEF_BRANCH_KEY));
						try {
							repo.setLastUpdate(dateFormat.parse(element.getString(REPO_UPDATED_KEY)));
						} catch(Exception dateError) {
							log.warn("Exception "+ dateError.getMessage() + " handling Date " + element.getString(REPO_UPDATED_KEY) +
									" for " + repo.getName());
						}
						repoList.add(repo);
					} catch(Exception e) {
						log.error("Exception "+ e.getMessage() + " handling Repository List " + jsRepoArray);
					}
				}	
			}
		}
		else {
			log.error("NO Repo URL for " + owner);
		}
    	return repoList;
	}
	
	/**
	 * Get information from GitHub for a particular README file Object
	 * 
	 * @param readmeURL  URL to API README information
	 *  
	 * @return GitHubReadmeFile Object populate with retrieved information or NULL if it doesn't exist 
	 */
	private GithubReadmeFile getReadmeFile(String readmeURL){ 
		GithubReadmeFile readme = null;
		JSONObject jsRepo = getGitHubAPIObjectInformation(readmeURL);
		if (jsRepo != null) {		
			String displayURL = getJSONStringValue(jsRepo, README_HTML_URL_KEY);
			if (displayURL != null) {
				String rawURL  = getJSONStringValue(jsRepo, README_RAW_URL_KEY);
				int size = 0;
				if (jsRepo.has(README_SIZE_KEY))
					size = jsRepo.getInt(README_SIZE_KEY);
				readme = new GithubReadmeFile(displayURL, rawURL, size);
			}
		}
		return readme;
	}
	
	/**
	 * It handles NULL JSON values
	 * 
	 * @param element Parent JSON Object
	 * @param key     Key to search in
	 * 
	 * @return Value of given key or NULL if no present or JSON.NULL
	 */
	private String getJSONStringValue(JSONObject element, String key){
		String ret = null;
		// If JSON has given key ...
		if (element.has(key)) {
			Object retObj = element.get(key);
			if (retObj != null && !retObj.equals(JSONObject.NULL))
				ret = retObj.toString();
		}
		return ret;
	}
}
