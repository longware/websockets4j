websockets4j Is a simple implementation of the WebSockets protocol.
Currently supports the plain text version of Drafts 75 and 76 of the protocol.
TLS support will be added in future releases.

## Demo Application Video ##
<a href='http://www.youtube.com/watch?feature=player_embedded&v=HqQWozLG0VE' target='_blank'><img src='http://img.youtube.com/vi/HqQWozLG0VE/0.jpg' width='425' height=344 /></a>

## Maven Repository ##

Add to your pom.xml the following repository:

```
<repository>
    <id>com.juancavallotti</id>
    <name>websockets4j</name>
    <url>http://juancavallotti.com/maven2</url>
    <layout>default</layout>
</repository>
```

## Usage ##
This framework is very simple to use, here is an example of how it can be used in a servlets 3 container such as tomcat 7 or glassfish 3:

First, we extend the WebSocketServer so it can be Named and Singleton for CDI:

```
 import com.juancavallotti.websockets4j.WebSocketServer;
 import javax.inject.Named;
 import javax.inject.Singleton;

 @Named
 @Singleton
 public final class Server extends WebSocketServer {

 }
```

Then we add a servlet ContextListener that starts the server when the application is deployed and stops it when the application is undeployed.

```

 @WebListener
 public class WebappListener implements Serializable, ServletContextListener {


    @Inject
    private Server server;

    private static final Logger log = Logger.getLogger(WebappListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!server.isStarted()) {
           server.start();
           server.registerListener("/context",<i>some class implementing {@link WebScoketListner}</i>);
           log.log(Level.INFO, "Started WebSocketServer");
       }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
            server.stop();
            log.log(Level.INFO, "Stopped WebSocketServer");
    }
}

```

And the Javascript code in order to access the service:

```

var socket = new WebSocket("ws://some.host:10123/resource");

```

We can setup event listeners for the socket in javascript:

```
socket.onopen = function() { alert("Opened!") };
socket.onmessage = function (e) { alert("Message: "+e.data); };
socket.onclose = function() { alert("Closed!") };
socket.onerror = function() { alert("Error!") };
```

Port and backlog can be configured setting system properties. Please see javadoc API for further documentation.