#Github GSA Feed client
This client feeds Google Search Appliance (GSA) with information coming from a GitHub Enterprise instance.

**Why don't use GSA built-in Web Crawler?**
> Because it crawls provided main URL but it also follows any link within that page content so submitting the main "mygithub.com" URL it will crawl ALL our GitHub instance.

**Why don't have all Github contents in GSA?**
> GitHub Enterprise has its own Web Search engine, so we don't want to have ALL github contents duplicated into GSA.
> In case of hundreds of repositories we're going to saturate GSA index with millions of new references 

** So, what Github information is going to appear in GSA?**
> README.md contents and Organizations & Repository descriptions.


This client gathers information from Github enterprise and it creates and pushes to GSA an XML document that tells the search appliance about the contents that we want to index.

It pushes 2 types of feeds:
* Dynamic contents (metadata-and-url)
* Static contents (incremental)

##Dynamic contents
This feed type only provides a list of URLs and Metadata but _NOT Contents_ because we want GSA to crawl it:
* GSA crawler queues these URLs and fetches the contents from each document listed in the feed.
* Is incremental
* Is re-crawled periodically, based on the crawl settings for your search appliance.

This feed client pushes all README.md URLs in RAW format, _not HTML_, to prevent GSA from crawling additional pages.
See "Metadata" section to know with information is added together with each URL

##Static contents
This feed type provides a list of URLs, Metadata _AND Contents_ because we don't want GSA to crawl it.
That's because URLs to Repositories and Organizations descriptions have a lot of outgoing links to different pages.
* It can be either full or incremental.
* Is only indexed when the feed is received; the content and metadata are analyzed and added to the index. 
* The URLs submitted in a content feed are not crawled by the search appliance. 

This feed client pushes all Organizations and Users descriptions as content.
WARNING!! In order to prevent GSA to crawl these info, we provide a "fake" URL which is used internally as a Document Unique Identifier. 
The actual URL is pushed to GSA as "displayurl", so we use the "fake" one as an ID and the real one as the link to appear with the search result

##Metadata
GSA uses metadata to create "search filters" which are displayed in the results page and helps consumer to narrow search results.
Metadata pushed with the GitHub information:
* **owner**             Repository's owner (Organization or User)
* **ownerType**         Organization or User
* **reponame**          Repository Name
* **repolastupdated**   Date of last Repo update (RFC822 formatted date)
* **language**          Repository language; Java, CSS, C, ...
* **forks**             Number of Repository forks
* **stargazers**        Number of Repository Stargazers (a.k.a Watchers)
* **recordType**        Record Type (User, Org, Repo or File). It's actually used just to display a proper stylesheet

##GSA Feeds Official Documentation
[GSA Feed guide](https://www.google.com/support/enterprise/static/gsa/docs/admin/70/gsa_doc_set/feedsguide/feedsguide.html#1074230)

#Github Enterprise changes
Github instance must allow access to crawl raw "README.md" pages.
Modify "Allow" policy in your https://<MY_GITHUB_ENTERPRISE_URL>/**robots.txt** 
> Allow: /raw/*


#GSA Configuration changes
In order to allow GSA to crawl raw _"README.md"_ you need to include the following into _"Start and Block URLs > Follow Pattern"_
> regexp:https://<MY_GITHUB_ENTERPRISE_URL>/raw/[^/]*/[^/]*/[^/]*/README.md$

In order to allow GSA to _include_ Organizations and Repositories descriptions you need to include the "fake" URL into same "Follow Pattern" box:
> https://<MY_GITHUB_ENTERPRISE_URL>/description/

#Gradle instructions
## Running the application
Set parameter _appArgs_ with "GSA Datasource", "Github Instance" and "GSA Instance": 
> ./gradlew run -PappArgs="['myDatasource', 'https://myGithubInstance.com', 'http://myGSAInstance.com']"

## Creating JAR file
It creates a JAR file under "./build/libs"
> ./gradlew jar

## Creating Distribution ZIP file
It creates a ZIP file under "./build/distributions" containing own JAR file + dependencies + script to launch app.
> ./gradlew distzip


#To Do
* Push "Deleted items" within XML. 

** Solution 1. (Preferred) Move this GSA Feed Client to a GSA Connector 
** Solution 2. Check previous sent XMLs to know what items are new and which ones deleted
** Solution 3. Use persistence (MongoDB, file system, ...) to track what's new and hence ... deleted

* Include Testing
Create test classes and add them to gradle


#License
This is licensed under the Apache License, Version 2.0: 
* http://www.apache.org/licenses/LICENSE-2.0
* https://raw.github.com/king/king.github.io/master/LICENSE-APACHE
