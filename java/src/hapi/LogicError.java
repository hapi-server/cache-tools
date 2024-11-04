package hapi;

/**
 * Custom exception specific to the hapi-cache application.
 *
 * @author lopeznr1
 */
public class LogicError extends RuntimeException
{
	/** Standard Constructor */
	public LogicError(String aMessage, Exception aCause)
	{
		super(aMessage, aCause);
	}

	/** Standard Constructor */
	public LogicError(String aMessage)
	{
		super(aMessage);
	}

	/** Standard Constructor */
	public LogicError(Exception aCause)
	{
		super(aCause);
	}

}
