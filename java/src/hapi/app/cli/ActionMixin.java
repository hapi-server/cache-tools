package hapi.app.cli;

import hapi.server.ProxyAttr;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Object which defines the (Picocli) arguments that allow specification of the action to take.
 * <p>
 * The following actions are supported:
 * <ul>
 * <li>Run as 1 time instance, returning the HAPI stream via stdout
 * <li>Launch a HAPI proxy server
 * </ul>
 *
 * @author lopeznr1
 */
public class ActionMixin
{
	// Constants: Error messages
	private static final String ERR_ACTION_NONE_SPECIFIED = "Specify 1 action to be taken: --fetchOnce or --startProxy";
	private static final String ERR_ACTION_TOO_MANY_SPECIFIED = "Only 1 action can be taken. --fetchOnce or --startProxy";

	@Option(names = { "--fetchOnce" }, //
			description = "Launch the hapi-cache as a single instance and return the HAPI stream via stdout.")
	public boolean fetchOnce;

	@Option(names = { "--startProxy" }, paramLabel = "<port> <hapiEndpoint>", //
			parameterConsumer = PicocliParseUtil.ConverterProxyAttr.class, //
			description = "Start a HAPI proxy server on the provided local port number with a proxy to the specified HAPI endpoint.\n")
	public ProxyAttr proxyAttr = null;

	/**
	 * Method to ensure that the action arguments are properly specified.
	 * <p>
	 * Validation consists of ensuring either --fetchOnce or --startProxy is specified.
	 * <p>
	 * On any failure this will throw a {@link ParameterException}.
	 */
	public void validate(CommandLine aCommandLine)
	{
		// Ensure exactly 1 action is provided
		if (fetchOnce == true && proxyAttr != null)
			throw new ParameterException(aCommandLine, ERR_ACTION_TOO_MANY_SPECIFIED);

		if (fetchOnce == false && proxyAttr == null)
			throw new ParameterException(aCommandLine, ERR_ACTION_NONE_SPECIFIED);

		// Nothing else to validate
		return;
	}

}
