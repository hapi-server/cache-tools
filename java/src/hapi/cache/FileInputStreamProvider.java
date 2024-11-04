
package hapi.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author jbf
 */
public class FileInputStreamProvider implements InputStreamProvider {
    private File file;
    public FileInputStreamProvider( File f ) throws FileNotFoundException {
        if ( !f.exists() ) {
            throw new FileNotFoundException("file not found: "+f);
        }
        this.file= f;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }
  
}
