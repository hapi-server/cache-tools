
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 *
 * @author jbf
 */
public class URLInputStreamProvider implements InputStreamProvider {

    private URL url;
    
    public URLInputStreamProvider( URL url ) {
        this.url= url;
    }
    
    @Override
    public InputStream openInputStream() throws IOException {
        return url.openStream();
    }
    
}
