
package hapi.cache;

import java.net.URL;

/**
 * 
 * @author jbf
 */
public record HapiRequest ( 
        URL url, 
        URL host,
        String query,
        String dataset, 
        String start, 
        String stop, 
        String parameters, 
        String format,
        String include) {
    
}
