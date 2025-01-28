## Get logging for free

If you are already using the base-app of spring-app-utils, using the
logging interceptor is a one line addition to your build file. Simply
include this as a dependency:

build.gradle
```
dependencies {
...
    implementation 'org.sciserver.springapp:logging-interceptor:1.0+'
...
}
```

And you will get logging of all requests. They are intercepted prior
to running your controllers and written out prior to sending the
response. Without anything additional, you will get logs with request
parametes, latency and status information, such as:

```
{
  "$type": "SciServer.Logging.Message, SciServer.Logging",
  "MessageId": "96df174b-3980-4c01-aff3-1ef07a4ee2c7",
  "MessageType": "SERVICELOG",
  "Time": "2020-03-04T15:34:07.187Z",
  "Host": "517f405d8cc8",
  "Application": "unspecified",
  "Method": "org.sciserver.springapp.loginterceptor.LoggingInterceptor.afterCompletion",
  "ClientIP": "127.0.0.1",
  "timestamp": 1583336047186,
  "time": 55,
  "attrs": {
    "method": "GET",
    "query": null,
    "uri": "/test",
    "status": "404"
  },
  "counters": {},
  "typeName": "SERVICELOG"
}
```

By default, these do not go anywhere. However you can configure the
logger with your application.properties file or environment variables
to send to RabbitMQ, to a file, or console, or any combination of
such. For example, to send to a file and console you can set:

application.properties
```
logging.file.name: /var/log/application.log
logging.console: true
```

or, environment (e.g. for running in kubernetes)
```
LOGGING_FILE_NAME=/var/log/application.log
LOGGING_CONSOLE=true
```

**NOTE**: Don't miss the LOGGING_APPLICATION setting, this will be a
  primary way you can search for requests from your app in
  DB/ElasticSearch, etc.


To see the full list of settings, look at src/main/java/org/sciserver/springapp/loginterceptor/LoggerConfig.java component.

## Enhancing your logs

While gettign free request logs is nice, we probably want to enrich
the logs in at least some of our controllers. For example, if a user
is uploading a file, we would want to record the file size, or when
making a series of synchronous sub-calls to do work, we might want to
record how long each call takes. This is also simple, first we import
the interceptor Log utility in any file we need (e.g. in controllers):

```
import org.sciserver.springapp.loginterceptor.Log;
```

Then we can get access to the request-scoped log anywhere we
need. There is no need to setup any static members or autowiring,
simply do like (in this example, we set a counter recording upload
file size):


```
Log.get().setCounter("filesize", totalbytes);
```

or, setting a string attribute

```
Log.get().setAttr("someattr", "somevaluewewanttofilteron");
```

and so on. The Log returned by this function is a ServiceLog type, see
https://github.com/sciserver/sciserver-logging-java/blob/master/src/main/java/sciserver/logging/ServiceLog.java
for methods available.

