package hapi.cache;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;

import hapi.DataFormat;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Object which defines the (Picocli) arguments that allow specification of a HAPI data end point or a HAPI server
 * specification.
 * <p>
 * The following arguments are provided: --url, --sever, --dataset, --format, --parameters, --start, --stop
 * 
 * @author lopeznr1
 */
public class ServerMixin
{
	@Option(names = { "--url" }, //
			description = "URL to the HAPI data endpoint. This argument is not compatible with args: " //
					+ "{--server, --dataset, --format, --paramaters, --start, --stop}")
	private URL url;

	@Option(names = { "--server" }, //
			description = "URL to the HAPI server.")
	private URL server;

	@Option(names = { "--dataset" }, //
			description = "Target dataset from the HAPI server.")
	private String dataset;

	@Option(names = { "--format" }, //
			description = "HAPI stream serialization format. Values: ${COMPLETION-CANDIDATES}")
	private DataFormat format;

	@Option(names = { "--parameters" }, paramLabel = "<parm>", arity = "1..*", //
			description = "Target parameters from the HAPI dataset.")
	private String parameters;

	@Option(names = { "--start" }, paramLabel = "<time>", //
			description = "The start time (of the HAPI data).")
	private LocalDateTime start;

	@Option(names = { "--stop" }, paramLabel = "<time>", //
			description = "The stop time (of the HAPI data).\n")
	private LocalDateTime stop;

	/**
	 * Method to ensure that HAPI server specification arguments are properly specified.
	 * <p>
	 * On any failure this will throw a {@link ParameterException}.
	 */
	public void validateHapiServerSpecification(CommandLine aCommandLine)
	{
		// Gather the HAPI server args that have been collected
		var serverArgL = new ArrayList<String>();
		if (server != null)
			serverArgL.add("--server");
		if (dataset != null)
			serverArgL.add("--dataset");
		if (format != null)
			serverArgL.add("--format");
		if (parameters != null)
			serverArgL.add("--parametrs");
		if (start != null)
			serverArgL.add("--start");
		if (stop != null)
			serverArgL.add("--stop");

		// Validate that only 1 of following is:
		// [1] The HAPI URL endpoint via --url
		// or
		// [2] The HAPI server specification via: --server and a combination of [--dataset, --format, --parameters,
		// --start, --stop]
		if (url == null && serverArgL.size() == 0)
			throw new ParameterException(aCommandLine, "Invalid HAPI server specification! " //
					+ "Please specify either: --url or --server");

		// Validate that the URL endpoint is not specified with other server arguments
		if (url != null && serverArgL.size() > 0)
		{
			var errMsg = "Invalid HAPI server specification! Argument --url has been specified with ";
			if (serverArgL.size() == 1)
				errMsg += serverArgL.get(0) + ". These arguments are not compatible.";
			else
				errMsg += "[" + String.join(", ", serverArgL) + "]. These arguments are not compatible.";
			throw new ParameterException(aCommandLine, errMsg);
		}

	}

}
