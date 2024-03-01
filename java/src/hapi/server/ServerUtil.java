package hapi.server;

/**
 * Collection of utility methods to support running hapi-cache as a server.
 *
 * @author lopeznr1
 */
public class ServerUtil
{
	/**
	 * Utility method to start a proxy "hapi-cache" server with the specified {@link ProxyAttr}.
	 * <p>
	 * This proxy server will fulfill client requests for HAPI data using a combination of the local cache and the
	 * provided (remote) HAPI endpoint.
	 *
	 * @param aProxyAttr
	 *    Defines the attributes associated with the proxy server.
	 */
	public static void startProxy(ProxyAttr aProxyAttr)
	{
		// Log the details of the proxy server being started
		System.out.println("The HAPI cache-tools proxy server is starting...");
		System.out.println("     Binding to port: " + aProxyAttr.port());
		System.out.println("   Proxy destination: " + aProxyAttr.endpoint());

		// TODO: This is incomplete... Consider the following:
		// - Rolling your own HTTP* server
		// - Using Spark (Java) Framework...
		// - Other frameworks ???
		System.out.println("\nThe above would be nice but there is no logic... Bailing...\n\n");
	}

}
