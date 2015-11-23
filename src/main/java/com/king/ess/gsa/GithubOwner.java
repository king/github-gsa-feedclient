// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

/**
 * Plain Object to store GitHub Owner values as:
 *  - Owner Name     (GitHub alias)
 *  - Owner Type     (User or Organization)
 *  - Repo URL       (URL to retrieve all its repositories) 
 *  . url            (Owner HTML Url)
 *  - Description    (Owner description)
 *  - isOrganization (User or Organization)
 *  
 * @author King Engineering Systems & Support
 *
 */
public class GithubOwner {
	
	// GitHub User repositories Owner type
	private static String OWNER_TYPE_USER    = "User";
	private static String OWNER_TYPE_ORG     = "Organization";
	
	private String name;
	private String repoURL;
	private String url;
	private String description;
	private boolean bOrganization;
	
	public GithubOwner(String name, boolean isOrganization, String repoURL) {
		super();
		this.name = name;
		this.repoURL = repoURL;
		this.bOrganization = isOrganization;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return bOrganization?OWNER_TYPE_ORG:OWNER_TYPE_USER;
	}

	public String getRepoURL() {
		return repoURL;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public boolean isOrganization() {
		return bOrganization;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(getType()).append(":").append(getName());
		return sb.toString();
	}
}
