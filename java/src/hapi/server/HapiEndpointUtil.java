package hapi.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import hapi.LogicError;
import hapi.SpecVersion;

/**
 * Collection of utility methods to support working with a HAPI fetch endpoint.
 *
 * @author lopeznr1
 */
public class HapiEndpointUtil
{
	// Constants: Error messages relating to a HAPI fetch endpoint
	//
	// Note these messages need to be normalized via: normErr(...)
	private static final String ERR_FETCH_NEEDS_HAPI_PATH = "A /hapi/ subpath was not specified.";
	private static final String ERR_FETCH_NEEDS_QUERY = "HAPI fetch endpoints must have the query specified. No query was provided.";
	private static final String ERR_FETCH_NEEDS_QUERY_NON_EMPTY = "Empty query provided for HAPI Endpoint. A valid query must be provided.";
	private static final String ERR_FETCH_QUERY_DATASET_NONE = "The HAPI '$Dataset' query was not specified. The '$Dataset' must be specified.";
	private static final String ERR_FETCH_QUERY_DATASET_TOO_MANY = "Multiple HAPI '$Dataset' queries are specified. Only one '$Dataset' must be specified.";
	private static final String ERR_FETCH_QUERY_TIME_BEG_TOO_MANY = "Too many HAPI '$TimeBeg' queries specified. At most 1 is allowed. Found: ";
	private static final String ERR_FETCH_QUERY_TIME_END_TOO_MANY = "Too many HAPI '$TimeEnd' queries specified. At most 1 is allowed. Found: ";
	private static final String ERR_FETCH_QUERY_UNKNOWN = "Unknown queries provided. The only HAPI queries are: [$Dataset, $TimeBeg, $TimeEnd, parameters]. The following was provided: ";

	// Constants: Error messages relating to a HAPI top-level endpoint
	private static final String ERR_TOP_LEVEL_NO_URL = "URL was not provided.";
	private static final String ERR_TOP_LEVEL_HAPI_PATH_NONE = "The trailing /hapi/ path was not specified.";
	private static final String ERR_TOP_LEVEL_NO_QUERY = "The top level HAPI URL endpoint should not have a query specified.";

	/**
	 * Utility method that will return the HAPI fetch (endpoint) URL for the specified HAPI request.
	 * <p>
	 * If there are no issues then an empty list will be returned.
	 */
	public static URL formFetch(URL aServer, SpecVersion aSpecVersion, FetchQuery aFetchQuery)
	{
		try
		{
			// Strip the trailing /hapi/ (tag if specified) within the server string
			var serverStr = "" + aServer;
			if (serverStr.endsWith("/") == true)
				serverStr = serverStr.substring(0, serverStr.length() - 1);
			if (serverStr.endsWith("/hapi") == true)
				serverStr = serverStr.substring(0, serverStr.length() - 5);
			var tmpServer = new URL(serverStr);

			// Form the (relative) fetch path to the HAPI endpoint
			var pathStr = tmpServer.getPath();
			pathStr += "/hapi/data";

			// Form the query string component
			var queryStr = "?";
			var datasetTag = "";
			var timeBegTag = "";
			var timeEndTag = "";
			switch (aSpecVersion)
			{
				case Version2:
					datasetTag = "id";
					timeBegTag = "time.min";
					timeEndTag = "time.max";
					break;
				case Version3:
					datasetTag = "dataset";
					timeBegTag = "start";
					timeEndTag = "stop";
					break;
				default:
					throw new LogicError("Unsupported HAPI specification version: " + aSpecVersion);
			}

			queryStr += datasetTag + "=" + aFetchQuery.dataset();
			if (aFetchQuery.timeBeg() != null)
				queryStr += "&" + timeBegTag + "=" + aFetchQuery.timeBeg();
			if (aFetchQuery.timeEnd() != null)
				queryStr += "&" + timeEndTag + "=" + aFetchQuery.timeEnd();
			if (aFetchQuery.parameterL() != null && aFetchQuery.parameterL().size() > 0)
				queryStr += "&parameters=" + String.join(",", aFetchQuery.parameterL());
			if (aFetchQuery.includeHeader() == true)
				queryStr += "&include=header";

			var specStr = pathStr + queryStr;
			return new URL(tmpServer, specStr);
		}
		catch (MalformedURLException aExp)
		{
			throw new LogicError(aExp);
		}

	}

	/**
	 * Utility method that will return the list of issues associated with a HAPI fetch (URL) endpoint.
	 * <p>
	 * If there are no issues then an empty list will be returned.
	 */
	public static List<String> getIssuesFetch(URL aUrl, SpecVersion aSpecVersion)
	{
		var retFailL = new ArrayList<String>();

		// Ensure we have a valid path
		// - Note we only check that a '/hapi/' (sub)path is specified.
		var pathStr = aUrl.getPath();
		if (pathStr.contains("/hapi/") == false)
			retFailL.add(ERR_FETCH_NEEDS_HAPI_PATH);

		// Ensure we have a valid query string
		var queryStr = aUrl.getQuery();
		if (queryStr == null)
			retFailL.add(ERR_FETCH_NEEDS_QUERY);
		else if (queryStr.isBlank() == true)
			retFailL.add(ERR_FETCH_NEEDS_QUERY_NON_EMPTY);
		else
		{
			var cntId = 0;
			var cntTimeMin = 0;
			var cntTimeMax = 0;
			; // var cntParms = 0;
			; // var cntIgnored = 0;
			var unknownL = new ArrayList<String>();
			var tokenArr = queryStr.split("&");
			if (aSpecVersion == SpecVersion.Version2)
			{
				for (var aToken : tokenArr)
				{
					if (aToken.startsWith("id=") == true)
						cntId++;
					else if (aToken.startsWith("time.min=") == true)
						cntTimeMin++;
					else if (aToken.startsWith("time.max=") == true)
						cntTimeMax++;
					else if (aToken.startsWith("parameters=") == true)
						; // cntParms++;
					else if (aToken.startsWith("include=") == true)
						; // cntIgnored++;
					else
						unknownL.add(aToken.split("=")[0]);
				}
			}
			else if (aSpecVersion == SpecVersion.Version3)
			{
				for (var aToken : tokenArr)
				{
					if (aToken.startsWith("dataset=") == true)
						cntId++;
					else if (aToken.startsWith("start=") == true)
						cntTimeMin++;
					else if (aToken.startsWith("stop=") == true)
						cntTimeMax++;
					else if (aToken.startsWith("parameters=") == true)
						; // cntParms++;
					else if (aToken.startsWith("include=") == true)
						; // cntIgnored++;
					else
						unknownL.add(aToken.split("=")[0]);
				}
			}
			else
				throw new LogicError("Unsupported SpecVersion: " + aSpecVersion);

			if (cntId == 0)
				retFailL.add(normErr(ERR_FETCH_QUERY_DATASET_NONE, aSpecVersion));
			if (cntId > 1)
				retFailL.add(normErr(ERR_FETCH_QUERY_DATASET_TOO_MANY, aSpecVersion));
			if (cntTimeMin > 1)
				retFailL.add(normErr(ERR_FETCH_QUERY_TIME_BEG_TOO_MANY, aSpecVersion) + cntTimeMin);
			if (cntTimeMax > 1)
				retFailL.add(normErr(ERR_FETCH_QUERY_TIME_END_TOO_MANY, aSpecVersion) + cntTimeMax);
			if (unknownL.size() > 0)
				retFailL.add(normErr(ERR_FETCH_QUERY_UNKNOWN, aSpecVersion) + String.join(", ", unknownL));
		}

		return retFailL;
	}

	/**
	 * Utility method that will return the list of issues associated with a HAPI top-level (URL) endpoint.
	 * <p>
	 * If there are no issues then an empty list will be returned.
	 * <p>
	 * The criteria for a valid HAPI endpoint is:
	 * <ul>
	 * <li>A valid URL (not null)
	 * <li>The URL has a path that ends with /hapi or /hapi/
	 * <li>There are no query component on the URL
	 * </ul>
	 */
	public static List<String> getIssuesTopLevel(URL aUrl)
	{
		var retFailL = new ArrayList<String>();

		if (aUrl == null)
			retFailL.add(ERR_TOP_LEVEL_NO_URL);

		// Ensure the path component ends with /hapi or /hapi/
		var pathStr = aUrl.getPath();
		if (pathStr.endsWith("/hapi/") == false && pathStr.endsWith("/hapi") == false)
			retFailL.add(ERR_TOP_LEVEL_HAPI_PATH_NONE);

		// Ensure there is no query component
		if (aUrl.getQuery() != null)
			retFailL.add(ERR_TOP_LEVEL_NO_QUERY);

		return retFailL;
	}

	/**
	 * Helper method that takes an error message and returns the corresponding error message normalized to the providede
	 * {@link SpecVersion}.
	 */
	private static String normErr(String aErrMsg, SpecVersion aSpecVersion)
	{
		// Normalize to support HAPI specification version 2
		if (aSpecVersion == SpecVersion.Version2)
		{
			// Normalize to Version3
			var retErrMsg = aErrMsg;
			retErrMsg = retErrMsg.replaceAll("\\$Dataset", "id");
			retErrMsg = retErrMsg.replaceAll("\\$TimeBeg", "time.min");
			retErrMsg = retErrMsg.replaceAll("\\$TimeEnd", "time.max");
			return retErrMsg;
		}

		// Normalize to support HAPI specification version 3
		else if (aSpecVersion == SpecVersion.Version3)
		{
			var retErrMsg = aErrMsg;
			retErrMsg = retErrMsg.replaceAll("\\$Dataset", "dataset");
			retErrMsg = retErrMsg.replaceAll("\\$TimeBeg", "start");
			retErrMsg = retErrMsg.replaceAll("\\$TimeEnd", "stop");
			return retErrMsg;
		}

		throw new LogicError("Unsupported SpecVersion: " + aSpecVersion);

	}

}
