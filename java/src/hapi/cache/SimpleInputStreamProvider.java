
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author jbf
 */
public class SimpleInputStreamProvider implements InputStreamProvider {

    InputStream ins;
    
    public SimpleInputStreamProvider( InputStream ins ) {
        this.ins= ins;
    }
    
    @Override
    public InputStream openInputStream() throws IOException {
        return ins;
    }
    
}
