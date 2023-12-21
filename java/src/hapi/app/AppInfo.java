package hapi.app;

/**
 * Record that defines the attributes of a specific application.
 * 
 * The following attributes are provided:
 * <ul>
 * <li>name: The formal name of the application.
 * <li>version: The version of the application (as a string)
 * 
 * @author lopeznr1
 */
public record AppInfo(String name, String version) {

}
