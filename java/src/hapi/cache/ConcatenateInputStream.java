
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;

public class ConcatenateInputStream extends InputStream {

    private final InputStreamProvider[] streams;
    private int currentStreamIndex = 0;
    private InputStream currentStream= null;

    public ConcatenateInputStream(InputStreamProvider... streams) {
        this.streams = streams;
    }

    @Override
    public int read() throws IOException {
        if ( currentStream==null ) {
            currentStream= streams[currentStreamIndex].openInputStream();
        }
        while (currentStreamIndex < streams.length) {
            int b = currentStream.read();
            if (b != -1) {
                return b;
            }
            currentStream.close();
            currentStreamIndex++;
            if ( currentStreamIndex < streams.length ) {
                currentStream= streams[currentStreamIndex].openInputStream();
            }
        }
        return -1;
    }

    @Override
    public void close() throws IOException {

    }
}