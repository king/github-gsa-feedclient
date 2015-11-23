// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

import java.util.Date;

/**
 * Plain Object to store GitHub Repository values as:
 *  - Owner          GitHub Repository owner object
 *  - Readme         Repository's README.md file
 *  - name           Repo name
 *  - description    Repo description
 *  - language       Repo language as GitHub categorizes it (CSS, Java, C, ...)
 *  - defaultBranch  Default branch name
 *  - htmlURL        URL of Repository in HTML format
 *  - contentsURL    API URL to retrieve repository contents
 *  - lastUpdate     Last updating date of Repo
 *  - forks          Number of forks
 *  - stargazers     Number of stargazers (aka Watchers)
 *  
 * @author King Engineering Systems & Support
 *
 */
public class GithubRepo {

	private GithubOwner owner;
	private GithubReadmeFile readme;
	private String name;
	private String description;
	private String language;
	private String defaultBranch;
	private String htmlURL;
	private String contentsURL;
	private Date   lastUpdate;
	private int    forks;
	private int    stargazers;
	
	public GithubRepo(GithubOwner owner) {
		super();
		this.owner = owner;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public GithubOwner getOwner() {
		return owner;
	}
	
	public String getHtmlURL() {
		return htmlURL;
	}

	public void setHtmlURL(String htmlURL) {
		this.htmlURL = htmlURL;
	}

	public String getContentsURL() {
		return contentsURL;
	}

	public void setContentsURL(String contentsURL) {
		this.contentsURL = contentsURL;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getDefaultBranch() {
		return defaultBranch;
	}

	public void setDefaultBranch(String defaultBranch) {
		this.defaultBranch = defaultBranch;
	}

	public int getForks() {
		return forks;
	}

	public void setForks(int forks) {
		this.forks = forks;
	}

	public int getStargazers() {
		return stargazers;
	}

	public void setStargazers(int stargazers) {
		this.stargazers = stargazers;
	}
	
	public boolean hasReadme(){
		if (this.readme != null && this.readme.getSize() > 0)
			return true;
		return false;
	}
	
	public GithubReadmeFile getReadme() {
		return readme;
	}

	public void setReadme(GithubReadmeFile readme) {
		this.readme = readme;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(getOwner().getName()).append("/").append(getName());
		return sb.toString();
	}
}
