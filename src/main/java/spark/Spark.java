/*
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spark;

import spark.route.HttpMethod;
import spark.route.RouteMatcher;
import spark.route.RouteMatcherFactory;
import spark.webserver.SparkServer;
import spark.webserver.SparkServerFactory;

/**
 * The main building block of a Spark application is a set of routes. A route is
 * made up of three simple pieces:
 * <p/>
 * <ul>
 * <li>A verb (get, post, put, delete, head, trace, connect, options)</li>
 * <li>A path (/hello, /users/:name)</li>
 * <li>A callback ( handle(Request request, Response response) )</li>
 * </ul>
 * <p/>
 * Example:
 * <p/>
 * <pre>
 * {@code
 * Spark.get(new Route("/hello") {
 *    public Object handle(Request request, Response response) {
 *       return "Hello World!";
 *    }
 * });
 * </pre>
 * <p/>
 * <code>
 * <p/>
 * </code>
 *
 * @author Per Wendel
 */
public final class Spark {

    private static final int SPARK_DEFAULT_PORT = 4567;

    private static boolean initialized = false;

    private static SparkServer server;
    private static RouteMatcher routeMatcher;
    private static String ipAddress = "0.0.0.0";
    private static int port = SPARK_DEFAULT_PORT;

    private static String keystoreFile;
    private static String keystorePassword;
    private static String truststoreFile;
    private static String truststorePassword;

    private static String staticFileRoute = null;

    // Hide constructor
    private Spark() {
    }

    /**
     * Set the IP address that Spark should listen on. If not called the default
     * address is '0.0.0.0'. This has to be called before any route mapping is
     * done.
     *
     * @param ipAddress The ipAddress
     */
    public static synchronized void setIpAddress(String ipAddress) {
        if (initialized) {
            throwBeforeRouteMappingException();
        }
        Spark.ipAddress = ipAddress;
    }

    /**
     * Set the port that Spark should listen on. If not called the default port
     * is 4567. This has to be called before any route mapping is done.
     *
     * @param port The port number
     */
    public static synchronized void setPort(int port) {
        if (initialized) {
            throwBeforeRouteMappingException();
        }
        Spark.port = port;
    }

    /**
     * Set the connection to be secure, using the specified keystore and
     * truststore. This has to be called before any route mapping is done. You
     * have to supply a keystore file, truststore file is optional (keystore
     * will be reused).
     * <p/>
     * This method is only relevant when using embedded Jetty servers. It should
     * not be used if you are using Servlets, where you will need to secure the
     * connection in the servlet container
     *
     * @param keystoreFile       The keystore file location as string
     * @param keystorePassword   the password for the keystore
     * @param truststoreFile     the truststore file location as string, leave null to reuse
     *                           keystore
     * @param truststorePassword the trust store password
     */
    public static synchronized void setSecure(String keystoreFile,
                                              String keystorePassword, String truststoreFile,
                                              String truststorePassword) {
        if (initialized) {
            throwBeforeRouteMappingException();
        }

        if (keystoreFile == null) {
            throw new IllegalArgumentException(
                    "Must provide a keystore file to run secured");
        }

        Spark.keystoreFile = keystoreFile;
        Spark.keystorePassword = keystorePassword;
        Spark.truststoreFile = truststoreFile;
        Spark.truststorePassword = truststorePassword;
    }

    /**
     * Assign a route in classpath for static file. <b>Careful : this method
     * must be called before all others method.</b>
     *
     * @param route the route in classpath.
     */
    public static synchronized void staticFileRoute(String route) {
        if (initialized) {
            throwBeforeRouteMappingException();
        }
        staticFileRoute = route;
    }

    /**
     * Map the route for HTTP GET requests
     *
     * @param route The route
     */
    public static synchronized void get(Route route) {
        addRoute(HttpMethod.get.name(), route);
    }

    /**
     * Map the route for HTTP POST requests
     *
     * @param route The route
     */
    public static synchronized void post(Route route) {
        addRoute(HttpMethod.post.name(), route);
    }

    /**
     * Map the route for HTTP PUT requests
     *
     * @param route The route
     */
    public static synchronized void put(Route route) {
        addRoute(HttpMethod.put.name(), route);
    }

    /**
     * Map the route for HTTP PATCH requests
     *
     * @param route The route
     */
    public static synchronized void patch(Route route) {
        addRoute(HttpMethod.patch.name(), route);
    }

    /**
     * Map the route for HTTP DELETE requests
     *
     * @param route The route
     */
    public static synchronized void delete(Route route) {
        addRoute(HttpMethod.delete.name(), route);
    }

    /**
     * Map the route for HTTP HEAD requests
     *
     * @param route The route
     */
    public static synchronized void head(Route route) {
        addRoute(HttpMethod.head.name(), route);
    }

    /**
     * Map the route for HTTP TRACE requests
     *
     * @param route The route
     */
    public static synchronized void trace(Route route) {
        addRoute(HttpMethod.trace.name(), route);
    }

    /**
     * Map the route for HTTP CONNECT requests
     *
     * @param route The route
     */
    public static synchronized void connect(Route route) {
        addRoute(HttpMethod.connect.name(), route);
    }

    /**
     * Map the route for HTTP OPTIONS requests
     *
     * @param route The route
     */
    public static synchronized void options(Route route) {
        addRoute(HttpMethod.options.name(), route);
    }

    /**
     * Maps a filter to be executed before any matching routes
     *
     * @param filter The filter
     */
    public static synchronized void before(Filter filter) {
        addFilter(HttpMethod.before.name(), filter);
    }

    /**
     * Maps a filter to be executed after any matching routes
     *
     * @param filter The filter
     */
    public static synchronized void after(Filter filter) {
        addFilter(HttpMethod.after.name(), filter);
    }

    static synchronized void runFromServlet() {
        if (!initialized) {
            routeMatcher = RouteMatcherFactory.get();
            initialized = true;
        }
    }

    // WARNING, used for jUnit testing only!!!
    static synchronized void clearRoutes() {
        routeMatcher.clearRoutes();
    }

    // Used for jUnit testing!
    static synchronized void stop() {
        if (server != null) {
            server.stop();
        }
        initialized = false;
    }

    private static void addRoute(String httpMethod, Route route) {
        init();
        routeMatcher.parseValidateAddRoute(httpMethod + " '" + route.getPath()
                + "'", route);
    }

    private static void addFilter(String httpMethod, Filter filter) {
        init();
        routeMatcher.parseValidateAddRoute(httpMethod + " '" + filter.getPath()
                + "'", filter);
    }

    private static synchronized void init() {
        if (!initialized) {
            routeMatcher = RouteMatcherFactory.get();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    server = SparkServerFactory.create(staticFileRoute != null);
                    server.ignite(
                            ipAddress,
                            port,
                            keystoreFile,
                            keystorePassword,
                            truststoreFile,
                            truststorePassword,
                            staticFileRoute);
                }
            }).start();
            initialized = true;
        }
    }

    private static void throwBeforeRouteMappingException() {
        throw new IllegalStateException(
                "This must be done before route mapping has begun");
    }
    
    /*
     * TODO: discover new TODOs.
     * 
     * 
     * TODO: Make available as maven dependency, upload on repo etc... 
     * TODO: Add *, splat possibility 
     * TODO: Add validation of routes, invalid characters and stuff, also validate parameters, check static, ONGOING
     * 
     * TODO: Javadoc
     * 
     * TODO: Create maven archetype, "ONGOING" 
     * TODO: Add cache-control helpers
     * 
     * advanced TODO list: 
     * TODO: Add regexp URIs
     * 
     * Ongoing
     * 
     * Done 
     * TODO: Routes are matched in the order they are defined. The rirst route that matches the request is invoked. ??? 
     * TODO: Before method for filters...check sinatra page 
     * TODO: Setting Headers 
     * TODO: Do we want get-prefixes for all *getters* or do we want a more ruby like approach???
     * (Maybe have two choices?) 
     * TODO: Setting Body, Status Code 
     * TODO: Add possibility to set content type on return, DONE 
     * TODO: Add possibility to access HttpServletContext in method impl, DONE 
     * TODO: Redirect func in web context, DONE 
     * TODO: Refactor, extract interfaces, DONE 
     * TODO: Figure out a nice name, DONE - SPARK 
     * TODO: Add /uri/{param} possibility, DONE 
     * TODO: Tweak log4j config, DONE 
     * TODO: Query string in web context, DONE 
     * TODO: Add URI-param fetching from webcontext ie. ?param=value&param2=...etc, AND headers, DONE
     * TODO: sessions? (use session servlet context?) DONE
     */
}
