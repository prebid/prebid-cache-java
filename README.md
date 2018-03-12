(work in progress)

# _Prebid Cache Java_
Prebid Cache provides the caching service component for the Prebid Server project.  Currently, the API supports both the GET and POST endpoints.  Prebid Cache Java, as default, provides a Redis implementation for the cache.  However, Prebid Cache is designed to support any cache implementation.  

## Integration Guide
Project configuration is managed through the use of YAML configuration (see resources folder).

### _Requirements_
This section covers the mandatory pre-requisites needed to be able to run the application.

* Windows, Linux, AWS, GCP or MacOS
* JDK 8+
* Maven
* Git
* Redis (or a custom cache implementation)

### _Quick Install_
This section describes how to download, install and run the application.

###### A. Using Maven on MacOS (recommended installation method)

(1). Clone the repo:

```bash
$ git clone https://github.com/prebid/prebid-cache-java.git
```

(2). Start Redis:

```bash
$ nohup redis-server &
```

(3). Start the Maven build

```bash
$ cd prebid-cache-java
$ mvn clean gplus:execute package
...
[INFO] Layout: JAR
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 12.099 s
[INFO] Finished at: 2017-11-13T10:45:51-08:00
[INFO] Final Memory: 52M/456M
[INFO] ------------------------------------------------------------------------
```

(4). Run Spring Boot JAR (_from project root_)

```bash
$ java -jar target/prebid-cache.jar
```

### _Spring Profiles_

This section shows examples of the various runtime environment configuration(s).

(1). Localhost with log4j and management endpoints enabled:

_VM Options:_
```bash
$ java -jar prebid-cache.jar -Dspring.profiles.active=manage,local -Dlog.dir=/app/prebid-cache-java/log/
```

(2). Production with log4j and management endpoints disabled:

_VM Options:_
```bash
$ java -jar prebid-cache.jar -Dspring.profiles.active=prod -Dlog.dir=/app/prebid-cache-java/log/
```

### _Cache Configuration_
It is possible to override the default YAML configuration by supplying a custom configuration.  See example scenario(s) below.

###### Fault Tolerant Redis Sentinel (1 master and 2 slaves)

_application-default.yml:_
```yaml
spring.redis.host: redis_master_host
spring.redis.port: 6379
spring.redis.sentinel.master: master
spring.redis.sentinel.nodes: sentinel_host1:26379,sentinel_host2:26379,sentinel_host3:26379
```    

### _Optional:  Bring Your Own (BYO) Cache Implementation_

Prebid Cache can support any cache implementation, although Redis is provided as default.  Spring injects the cache repository bean instances during context initialization, or application startup.  This section describes how to setup a custom cache repository.

###### A. ReactiveRepository Interface
The custom cache implementation must contain a class that conforms to the _ReactiveRepository_ interface.

_CustomRepositoryImpl_:
```java
public class CustomRepositoryImpl implements ReactiveRepository
{
  Mono<PayloadWrapper> save(final PayloadWrapper wrapper) {
    // You must implement save method
  }

  Mono<PayloadWrapper> findById(final String id) {
    // You must implement findById method
  }
}
```

###### B. Repository Configuration
A configuration object should be passed into the constructor of your custom repository implementation.  At minimum, this configuration object would contain the caching service _host_ and _port_ details.  

 _CustomRepositoryImpl:_
 ```java
 public class CustomRepositoryImpl implements ReactiveRepository
 {
   private final CustomPropertyConfiguration config;

   @Autowired
   public CustomRepositoryImpl(final CustomPropertyConfiguration config) {
       this.config = config;
   }   
 }

 ```

 Here is an example definition of a custom configuration property class.  It is important to replace _'custom'_ with the correct cache implementation name (e.g. redis, memcached, aerospike, etc...).  If Spring already provides a predefined configuration property prefix, please use that instead. 

_CustomPropertyConfiguration_:
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix="spring.custom")
public class CustomPropertyConfiguration
{
    private String host;
    private int port;
}

```

###### C. YAML Configuration and custom Maven goal (gplus:execute)
The Spring cache repository context is defined in this Spring XML configuration (spring-repository-bean.xml).  There is no need to modify this XML configuration because it is automatically generated from a custom maven goal called _gplus:execute_.  However, if a custom cache implementation is needed, then a custom cache YAML configuration needs to be provided.  See example below: 

_src/main/resources/repository.yml_:
```yaml
cache.profiles.active: custom
cache.custom.classname.canonical: org.prebid.cache.repository.custom.CustomRepositoryImpl
cache.custom.property.configuration.classname: CustomPropertyConfiguration
```

### _Metrics_

Currently, there is support for the capturing of metrics using _Graphite_.
For more information, see: https://graphite.readthedocs.io/en/latest/

###### A. Durations (timers) and Rates (meters) 

The metrics registory supports collection on these types:
* _request and request duration_
* _json request_
* _xml request_
* _resource not found error_
* _bad request error_
* _invalid request error_
* _internal server error_


###### B. Graphite YAML Configuration
A carbon-cache daemon needs to be running on the _Graphite_ host for collection to happen.  In addition, the sending of metrics is batched for every minute.  Connection details such as host / port will need to be specified in the configuration.

_src/main/resources/repository.yml_:
```yaml
metrics:
  graphite:
    enabled: true
    host: http://graphite.yourdomain.com
    port: 2003
    prefix: prebid
```

### _Logging_
Since logging is environment specific, log configuration and settings should be defined in the dev, qa, and prod profile sections of application.yml. 

###### A. Dev Environment

_src/main/resources/application.yml_:
```yaml
# dev
spring.profiles: dev
logging.level.root: debug
logging.config: classpath:log4j2-dev.xml
```

_src/main/resources/log4j-dev.xml_:
```xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- Don't forget to set system property
-DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
     to make all loggers asynchronous. -->

<Configuration status="WARN" monitorInterval="30">
    <Properties>
        <Property name="fileName">${sys:log.dir}/spring-boot-service.log</Property>
        <Property name="fileNamePattern">
            ${sys:log.dir}/%d{yyyy-MM-dd}-%i.log
        </Property>
        <Property name="logPattern">
            %d{yyyy-MM-dd HH:mm:ss.SSS} %5p ${hostName} --- [%15.15t] %-40.40c{1.} : %m%n%ex
        </Property>
    </Properties>
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT" follow="true">
            <PatternLayout pattern="${logPattern}"/>
        </Console>
        <!-- Rolling File Appender -->
        <RollingFile name="FileAppender" fileName="${fileName}" filePattern="$fileNamePattern">
            <PatternLayout>
                <Pattern>${logPattern}</Pattern>
            </PatternLayout>
            <Policies>
                <SizeBasedTriggeringPolicy size="100MB" />
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>
    <Loggers>
        <AsyncLogger name="org.prebid.cache" level="info" additivity="false">
            <AppenderRef ref="FileAppender"/>
        </AsyncLogger>

        <Root level="info">
            <AppenderRef ref="ConsoleAppender" />
        </Root>
    </Loggers>
</Configuration>
```

### _Services (DevOps)_
This section contains instructions on how to run the app as a service in various different ways.  DevOps should find this section relevant and useful.  By default, the packaged JAR is setup to be fully executable.

###### A. Executable JAR

_pom.xml_:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    ...
    <configuration>
        <executable>true</executable>
        <embeddedLaunchScript>src/scripts/custom_launch.script</embeddedLaunchScript>
    </configuration>
</plugin>
```   
Note: A custom launch script is needed as a fix due to a Spring Boot issue (#12188):
https://github.com/spring-projects/spring-boot/issues/12188

To launch the JAR from the command line (UNIX/Linux):   
```bash
$ ./prebid-cache.jar
```

For security reasons, it is recommended to run the service as a non-root user:
```bash
$ sudo useradd prebid-cache
$ sudo passwd prebid-cache
$ sudo chown prebid-cache:prebid-cache prebid-cache.jar
$ sudo chmod 500 prebid-cache.jar
```

###### B. System V Init

Symbolic link JAR to init.d:
```bash
$ sudo ln -s /app/prebid-cache.jar /etc/init.d/prebid-cache
```

```bash
$ sudo service prebid-cache start   # start service
$ sudo service prebid-cache status  # check status
$ sudo service prebid-cache stop    # stop service
$ sudo service prebid-cache restart # restart service
```

Following these steps will allow for:
* Non-root users to run the service
* PID management (/var/run/)
* Console logs (/var/log/)

###### C. Systemd

/etc/systemd/system/prebid-cache.service:
```bash
[Unit]
Description=Prebid Cache Service
After=syslog.target

[Service]
User=prebid-cache
ExecStart=/app/prebid-cache.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target

```
Now you can manage this service with systemctl:
```bash
$ systemctl start prebid-cache.service  # start service
$ systemctl stop prebid-cache.service   # stop service
$ systemctl status prebid-cache.service # check status
```
For more details please refer to man pages for systemctl.

###### D. AWS - Amazon Web Services
This section describes how to run the app in an Elastic Beanstalk environment with ElastiCache.

##### Prepare Artifact:

(1). Go to the project root:
```bash
$ cd prebid-cache-java
```

(2). Update codebase :
```bash
$ git pull
```

(3). Rebuild sources with Maven
```bash
$ mvn clean gplus:execute package
```

(4). Create folder in work directory:
```bash
$ mkdir aws-prebid-cache
```

(5). Copy jar file from prebid-cache-java/target to created directory:
```bash
$ cp prebid-cache-java/target/prebid-cache.jar aws-prebid-cache
```

(6). Create Procfile in aws-prebid-cache directory:
```bash 
$ sudo nano Procfile 
web: java -jar -Dspring.profiles.active=aws prebid-cache.jar
```

(7). Zip aws-prebid-cache directory
```bash
$ zip -r eb-prebid-cache-new.zip eb-prebid-cache-new
```
Artifact is ready for deploy to Elastic Beanstalk.
For more information, see https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html.

##### Elastic Beanstalk:

(1). Login in to the AWS console, https://console.aws.amazon.com/elasticbeanstalk/.

(2). Click on Create new application.

(3). Specify Application name and description and click Next.

(4). Click on Create web server.

(5). Choose platform as Java and environment type that you need (we will use singleinstance) and click Next.

(6). Upload artifact for deployment (zip file with jar and Procfile).

(7). Specify environment name, url and description and click next.

(8). Choose Create this environment inside a VPC and click next.

(9). Specify instance type (we will use t2.micro) and EC2 key pair, if you want login in to instance via ssh.

(10). Select subnet for EC2 instance and click next.

(11). Press Launch button on review page for launching environment and application. 

##### ElastiCache w/Redis:

(1). After cluster configuration is complete, we will need to login, https://console.aws.amazon.com/elasticbeanstalk/. 

(2). Go to the environment with application and press configuration.

(3). Choose software configuration and add SPRING_REDIS_HOST system property with host of the ElastiCache redis cluster.

To learn more about how to create an Elastic Cache cluster and the configuration with Elastic Beanstalk, see https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.ElastiCache.html.



### _Sample_

###### A. Request
```json
{
  "puts": [
    {
      "type": "json",
      "value": {
        "adm": "<script type=\"text/javascript\">\n      rubicon_cb = Math.random(); rubicon_rurl = document.referrer; if(top.location==document.location){rubicon_rurl = document.location;} rubicon_rurl = escape(rubicon_rurl);\n      window.rubicon_ad = \"4073548\" + \".\" + \"js\";\n      window.rubicon_creative = \"4458534\" + \".\" + \"js\";\n    </script>\n<div style=\"width: 0; height: 0; overflow: hidden;\"><img border=\"0\" width=\"1\" height=\"1\" src=\"http://beacon-us-iad2.rubiconproject.com/beacon/d/f5b45196-4d05-4e42-8190-264d993c3515?accountId=1001&siteId=113932&zoneId=535510&e=6A1E40E384DA563BD667C48E6BE5FF2436D13A174DE937CDDAAF548B67FBAEBDC779E539A868F72E270E87E31888912083DA7E4E4D5AF24E782EF9778EE5B34E9F0ADFB8523971184242CC624DE62CD4BB342D372FA82497B63ADB685D502967FCB404AD24048D03AFEA50BAA8A987A017B93F2D2A1C5933B4A7786F3B6CF76724F5207A2458AD77E82A954C1004678A\" alt=\"\" /></div>\n\n\n<a href=\"http://optimized-by.rubiconproject.com/t/1001/113932/535510-15.4073548.4458534?url=http%3A%2F%2Frubiconproject.com\" target=\"_blank\"><img src=\"https://secure-assets.rubiconproject.com/campaigns/1001/50/59/48/1476242257campaign_file_q06ab2.png\" border=\"0\" alt=\"\" /></a><div style=\"height:0px;width:0px;overflow:hidden\"><script>(function(){document.write('<iframe src=\"https://tap2-cdn.rubiconproject.com/partner/scripts/rubicon/emily.html?pc=1001/113932&geo=na&co=us\" frameborder=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"NO\" width=\"0\" height=\"0\" style=\"height:0px;width:0px\"></iframe>');})();</script></div>\n",
        "width": 300,
        "height": 250
      },
      "key" : "a8db2208-d085-444c-9721-c1161d7f09ce",
      "expiry" : 800
    },
    {"type" : "xml", "value":"<xml>\r\n  <creativeCode>\r\n    <![CDATA[\r\n      <html><\/html>\r\n      ]]>\r\n  <\/creativeCode>\r\n<\/xml>"}
  ]
}
```
###### B. Response
```json
{
    "responses": [
        {
            "uuid": "a8db2208-d085-444c-9721-c1161d7f09ce"
        },
        {
            "uuid": "6d9dda96-39ca-4203-b840-6e5c18b474b5"
        }
    ]
}
```

### _Staying Up-To-Date_ ###
For using the latest version of prebid cache, perform next steps:

(1). Go to the project root:
```bash
$ cd prebid-cache-java
```

(2). Update codebase:
```bash
$ git pull
```

(3). Rebuild sources with Maven
```bash
$ mvn clean gplus:execute package
```

If there are any questions, issues, or concerns, please submit them to https://github.com/prebid/prebid-cache-java/issues/.
