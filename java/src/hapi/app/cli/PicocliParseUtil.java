package hapi.app.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;

import hapi.SpecVersion;
import hapi.server.HapiEndpointUtil;
import hapi.server.ProxyAttr;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.TypeConversionException;

/**
 * Collection of utility classes / methods to support the command line processing using the Picocli library.
 *
 * @author lopeznr1
 */
public class PicocliParseUtil
{
	/**
	 * Implementation of {@link IParameterConsumer} for generating a {@link ProxyAttr}.
	 *
	 * @author lopeznr1
	 */
	public static class ConverterProxyAttr implements IParameterConsumer
	{
		// Constants: Error messages
		private static final String ERR_INCORRECT_ARGS = "--startProxy: Requires exactly 2 parameters.";
		private static final String ERR_PORT_IS_NOT_INTEGER = "--startProxy: 1st parameter requires an integer port number.";
		private static final String ERR_PORT_IS_NOT_POSITIVE = "--startProxy: 1st parameter requires a positive port number.";
		private static final String ERR_URL_IS_NOT_HAPI_ENDPOINT = "--startProxy: 2nd parametr is not a valid HAPI endpoint. URL must end with \"/hapi\".";
		private static final String ERR_URL_IS_NOT_VALID = "--startProxy: 2nd parameter is not a valid (HAPI endpoint) URL.";

		@Override
		public void consumeParameters(Stack<String> aArgS, ArgSpec aArgSpec, CommandSpec aCommandSpec)
		{
			var port = -1;
			var remoteEndpoint = (URL) null;

			var numArgsUsed = 0;
			while (aArgS.isEmpty() == false)
			{
				// Retrieve the next parameter, bail once we run into a (different) argument
				var tmpArgStr = aArgS.peek();
				if (aCommandSpec.optionsMap().keySet().contains(tmpArgStr) == true)
					break;
				aArgS.pop();

				numArgsUsed++;
				var errExtraMsg = " Provided: " + tmpArgStr;

				if (numArgsUsed == 1)
				{
					try
					{
						port = Integer.parseInt(tmpArgStr);
					}
					catch (NumberFormatException aExp)
					{
						throw new ParameterException(aCommandSpec.commandLine(), ERR_PORT_IS_NOT_INTEGER + errExtraMsg);
					}

					if (port <= 0)
						throw new ParameterException(aCommandSpec.commandLine(), ERR_PORT_IS_NOT_POSITIVE + errExtraMsg);
				}
				else if (numArgsUsed == 2)
				{
					try
					{
						remoteEndpoint = new URL(tmpArgStr);
					}
					catch (MalformedURLException aExp)
					{
						throw new ParameterException(aCommandSpec.commandLine(), ERR_URL_IS_NOT_VALID + errExtraMsg);
					}

					var issueL = HapiEndpointUtil.getIssuesTopLevel(remoteEndpoint);
					if (issueL.size() > 0)
						throw new ParameterException(aCommandSpec.commandLine(), ERR_URL_IS_NOT_HAPI_ENDPOINT + errExtraMsg);
				}
			}

			// Ensure 2 parameters have been consumed
			var errExtrMsg = " Provided: " + numArgsUsed;
			if (numArgsUsed != 2)
				throw new ParameterException(aCommandSpec.commandLine(), ERR_INCORRECT_ARGS + errExtrMsg);

			aArgSpec.setValue(new ProxyAttr(port, remoteEndpoint));
		}

	}

	/**
	 * Implementation of {@link ITypeConverter} for parsing a {@link SpecVersion}.
	 *
	 * @author lopeznr1
	 */
	public static class ConverterSpecVersion implements ITypeConverter<SpecVersion>
	{
		@Override
		public SpecVersion convert(String aValue)
		{
			try
			{
				var version = Integer.parseInt(aValue);
				if (version == 2)
					return SpecVersion.Version2;
				else if (version == 3)
					return SpecVersion.Version3;
			}
			catch (NumberFormatException aExp)
			{
				; // Exception will be thrown later...
			}

			throw new TypeConversionException("Version must be one of: [2, 3]. Provided: " + aValue);
		}
	}

}
