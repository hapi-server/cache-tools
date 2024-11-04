package hapi.server;

import java.net.URL;

/**
 * Defines the attributes needed to set up a proxy to a remote HAPI endpoint.
 * <p>
 * The following attributes are defined:
 * <ul>
 * <li>port: The port on which the (proxy) server will bind to
 * <li>endpoint: The (top-level) HAPI endpoint. This URL should end with: /hapi
 * </ul>
 *
 * @author lopeznr1
 */
public record ProxyAttr(int port, URL endpoint)
{
	/**
	 * Returns true if the remoteEndpoint is a valid top-level HAPI endpoint.
	 * <p>
	 * See {@link HapiEndpointUtil#getIssuesTopLevel(URL)}
	 */
	public boolean isValidEndpoint()
	{
		// Delegate: Validation of top-level HAPI endpoint
		var errIssueL = HapiEndpointUtil.getIssuesTopLevel(endpoint);
		if (errIssueL.size() == 0)
			return true;

		return false;
	}

	/**
	 * Returns true if the port > 0.
	 * <p>
	 * This method does NOT check to see if the specified port is available for binding.
	 */
	public boolean isValidPort()
	{
		return port > 0;
	}

}
