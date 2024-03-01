package hapi.app.cli;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import hapi.DataFormat;
import hapi.SpecVersion;
import hapi.server.FetchQuery;
import hapi.server.HapiEndpointUtil;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Object which defines the (Picocli) arguments that allow specification of a HAPI fetch query.
 * <p>
 * The following exclusive argument groups are supported: [--url] or [--sever, --dataset, --format, --parameters,
 * --start, --stop, --includeHeader]
 *
 * @author lopeznr1
 */
public class FetchQueryMixin
{
	// Constants: Error messages
	private static final String ERR_ARGS_NEEDED_SERVER_OR_URL = "Invalid HAPI server specification! Please specify either: --url or --server";
	private static final String ERR_QUERY_DATASET_NEEDED = "The --dataset argument must be specified with the --server argument";
	private static final String ERR_QUERY_START_NEEDED = "The --start argument must be specified";
	private static final String ERR_QUERY_STOP_NEEDED = "The --stop argument must be specified";

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
	private List<String> parameterL;

	@Option(names = { "--start" }, paramLabel = "<time>", //
			description = "The start time (of the HAPI data).")
	private String timeBegStr;

	@Option(names = { "--stop" }, paramLabel = "<time>", //
			description = "The stop time (of the HAPI data).")
	private String timeEndStr;

	@Option(names = { "--includeHeader" }, //
			description = "Flag that result in the HTTP header being included in the stream.\n")
	private boolean includeHeader;

	/**
	 * Returns the {@link FetchQuery} portion associated with the {@link FetchQueryMixin}.
	 */
	public FetchQuery getFetchQuery()
	{
		return new FetchQuery(dataset, timeBegStr, timeEndStr, parameterL, includeHeader);
	}

	/**
	 * Returns the HAPI datapoint {@link URL}..
	 */
	public URL getHapiUrl(SpecVersion aSpecVersion)
	{
		if (url != null)
			return url;

		// Delegate: Creation of the (fetch) URL (from the args: --server, --dataset --parameter, --start, --stop)
		var tmpFetchQuery = getFetchQuery();
		return HapiEndpointUtil.formFetch(server, aSpecVersion, tmpFetchQuery);
	}

	/**
	 * Method to ensure that HAPI server arguments are properly specified.
	 * <p>
	 * On any failure this will throw a {@link ParameterException}.
	 */
	public void validate(CommandLine aCommandLine, SpecVersion aSpecVersion)
	{
		// Gather the HAPI server args that have been collected
		var serverArgL = new ArrayList<String>();
		if (server != null)
			serverArgL.add("--server");
		if (dataset != null)
			serverArgL.add("--dataset");
		if (format != null)
			serverArgL.add("--format");
		if (parameterL != null)
			serverArgL.add("--parameters");
		if (timeBegStr != null)
			serverArgL.add("--start");
		if (timeEndStr != null)
			serverArgL.add("--stop");

		// Validate that only 1 style of arguments was specified.
		// [1] The HAPI URL endpoint via --url
		// or
		// [2] The HAPI server specification via: --server and a combination of [--dataset, --format, --parameters,
		// --start, --stop]
		if (url == null && serverArgL.size() == 0)
			throw new ParameterException(aCommandLine, ERR_ARGS_NEEDED_SERVER_OR_URL);

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

		// Delegate validation of the URL
		if (url != null)
		{
			var issueL = HapiEndpointUtil.getIssuesFetch(url, aSpecVersion);
			if (issueL.size() == 0)
				return;

			var failMsg = "The provided HAPI (endpoint) URL is not valid. The following are issues:\n\t";
			failMsg += String.join("\n\t - ", issueL);
			throw new ParameterException(aCommandLine, failMsg);
		}

		// Ensure the dataset parameter is specified
		if (dataset == null)
			throw new ParameterException(aCommandLine, ERR_QUERY_DATASET_NEEDED);

		// Ensure the start and stop time have been specified
		if (timeBegStr == null)
			throw new ParameterException(aCommandLine, ERR_QUERY_START_NEEDED);

		if (timeEndStr == null)
			throw new ParameterException(aCommandLine, ERR_QUERY_STOP_NEEDED);

		// TODO: Ensure the start and stop time can be parsed and are valid and are not swapped
		;

		// Synthesize a URL and see if there are any errors
		var hapiUrl = getHapiUrl(aSpecVersion);
		var issueL = HapiEndpointUtil.getIssuesFetch(hapiUrl, aSpecVersion);
		if (issueL.size() == 0)
			return;

		var failMsg = "There are issues with the provided --server and related  arguments.";
		failMsg += " URL generated: " + hapiUrl + ". This URL is not valid. The following are issues:\n\t";
		failMsg += String.join("\n\t - ", issueL);
		throw new ParameterException(aCommandLine, failMsg);
	}

}
