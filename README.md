# Web-Crawler

Java program that downloads a source URL\s appearing in the resulting page\s.

User provides the program with:
* The URL to start with
* The maximal amount of different URLs to extract from the page 
* How deep the process should run (depth factor) 
* Boolean flag indicating cross-level uniqueness 

Output - an html file for each URL. Naming convention <depth>/<url>.html (any illegal character being replaced with an underscore)

* Java 8
* Multithreading
* Gradle
