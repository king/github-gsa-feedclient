// Copyright (C) king.com Ltd 2015
// https://github.com/king/github-gsa-feedclient
// Author: Josep F. Barranco
// License: Apache 2.0, https://raw.github.com/king/github-gsa-feedclient/master/LICENSE-APACHE

package com.king.ess.gsa;

/**
 * Plain Object to store GitHub README.md values as:
 *  - html URL    URL of README.md file in HTML format
 *  - raw URL     URL of README.md file in RAW formal
 *  - size        Size of README.md file. <0 means not existing
 *  
 * @author King Engineering Systems & Support
 *
 */
public class GithubReadmeFile {

	private String htmlURL;
	private String rawURL;
	private int size = -1;
	
	public GithubReadmeFile(String htmlURL, String rawURL, int size) {
		super();
		this.htmlURL = htmlURL;
		this.rawURL = rawURL;
		this.size = size;
	}

	public String getHtmlURL() {
		return htmlURL;
	}

	public String getRawURL() {
		return rawURL;
	}

	public int getSize() {
		return size;
	}	
}