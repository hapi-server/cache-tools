
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * allow the input stream to be sent off to a file.
 * @author jbf
 */
public class TeeInputStream extends InputStream {

    private final OutputStream out;
    private final InputStream ins;
    
    public TeeInputStream( InputStream in, OutputStream out ) {
        this.ins= in;
        this.out= out;
        
    }
    
    @Override
    public int read() throws IOException {
        int i= ins.read();
        out.write(i);
        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int bytesRead= ins.read(b);
        if ( bytesRead>0 ) out.write(b,0,bytesRead);
        return bytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead= ins.read(b,off,len);
        if ( bytesRead>0 ) out.write(b,off,bytesRead);
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        ins.close();
        out.close();
    }
    
    
}
