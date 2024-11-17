How to create a release for *tinyscript*
========================================

Prepare the release
-------------------

Set version number correctly e.g. *0.9.0-SNAPSHOT* or *0.9.0-RELEASE*

1. Define the version number for core project -- this will update the POMs of the parent project and the child projects as well as the OSGi plugin metadata correctly

        mvn tycho-versions:set-version -DnewVersion=0.9.0-SNAPSHOT
        mvn tycho-versions:update-eclipse-metadata

2. Build and install the core project to the maven repo

        mvn clean install

3. Define the version numbers for the two application projects *repl* and *webdemo* and update their dependency on the core project accordingly

        <artifactId>tinyscript-webdemo</artifactId>
	    <groupId>de.mkbauer.tinyscript</groupId>
	    <name>Tinyscript Webdemo</name>
	    <description>Webdemo for Tinyscript</description>
	    <version>0.9.0-SNAPSHOT</version>
        ...
            <dependency>
			    <groupId>de.mkbauer.tinyscript</groupId>
			    <artifactId>de.mkbauer.tinyscript</artifactId>
			    <version>0.9.0-SNAPSHOT</version>
		    </dependency>

4. For *webdemo*: Update the release numbers in `index.html` and `Dockerfile`

5. Build and package the application projects

        cd de.mkbauer.tinyscript.repl
        mvn package

        cd de.mkbauer.tinyscript.webdemo
        mvn package


Publish the release
-------------------

Commit the version to *github*. Then create a release on *github* and upload the files from step 5, use a tag of the form `v0.9.0rc1`


Publish docker images
---------------------

1. Create a docker image for the *webdemo*

        cd de.mkbauer.tinyscript.webdemo

        docker docker build -t mkbauer/tinyscript:0.9.0rc1 .
        docker run -p 8080:8080 mkbauer/tinyscript:0.9.0rc1

2. Publish it on *Docker Hub*

        docker push build mkbauer/tinyscript:0.9.0rc1

        docker tag  <image_id> mkbauer/tinyscript:latest
        docker push mkbauer/tinyscript:latest



        
