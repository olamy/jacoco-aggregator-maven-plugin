# jacoco-aggregator-maven-plugin

## Prerequisites

- Java 8 or newer
- Maven 3 or newer

## Usage

This simple Apache Maven Mojo will help you generate aggregated Jacoco from multi modules projects.

This will prevent you creating extra Maven module as a workaround proposed here: https://www.baeldung.com/maven-jacoco-multi-module-project

First this has been proposed as an idea to integrated directly to Jacoco but the PR https://github.com/jacoco/jacoco/pull/1692 
doesn't look to have great success....

```xml
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>io.github.olamy.maven.plugins</groupId>
          <artifactId>jacoco-aggregator-maven-plugin</artifactId>
          <!-- or higher check release notes -->  
          <version>1.0.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
```

Now with you Apache Maven cli generating Jacoco reports just add 
```shell
jacoco-aggregator:report-aggregate-all
```

This will generate a full aggregated data report in `target/site/jacoco-aggregate/` included an aggregated `jacoco.xml` which can be used
with Jenkins `recordCoverage` sa is: 

```groovy
  recordCoverage name: "Coverage ${env.JDK}", id: "coverage-${env.JDK}", 
                tools: [[parser: 'JACOCO',pattern: 'target/site/jacoco-aggregate/jacoco.xml']],
                sourceDirectories: [[path: 'glob:**/src/main/java']]
```
