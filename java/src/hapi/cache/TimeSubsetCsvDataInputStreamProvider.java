
package hapi.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.hapiserver.TimeUtil;

/**
 * Suppose the cache file has data from 00:00 to 24:00, and you want
 * data from 10:00 to 12:00.  This will subset a HAPI CSV in time.
 * @author jbf
 */
public class TimeSubsetCsvDataInputStreamProvider implements InputStreamProvider {

    String start;
    String stop;
    InputStreamProvider ins;
    int totalBytesRead;
    
    public TimeSubsetCsvDataInputStreamProvider( String start, String stop, InputStreamProvider ins ) {
        this.start= start;
        this.stop= stop;
        this.ins= ins;
        this.totalBytesRead= 0;
    }
    @Override
    public InputStream openInputStream() throws IOException {
        return new TimeSubsetCsvDataInputStream(start, stop, ins.openInputStream() );
    }

    private class TimeSubsetCsvDataInputStream extends InputStream {
        BufferedReader insb;
        String nextRec= null;

        /**
         * position within the record.  Note this does not support UTF-8 extensions! TODO: support this.
         */
        int recChar=-1;

        private String start;
        private String stop;

        /**
         * we need to support $Y-$j as well as $Y-$m-$d for comparisons.
         */
        private boolean doReformatTime=true;
        Charset charset= Charset.forName("US-ASCII"); //TODO: UTF-8

        public TimeSubsetCsvDataInputStream( String start, String stop, InputStream ins ) {
            this.start= start;
            this.stop= stop;
            insb= new BufferedReader(new InputStreamReader(ins));
        }

        /**
         * read the next record within the start and stop times.
         * @return the record, or null if there is not another
         * record available.
         * @throws IOException 
         */
        private String readNextRec() throws IOException {
            nextRec= insb.readLine();
            if ( nextRec==null ) return null;
            nextRec= nextRec+"\n"; // bah copy TODO: inefficiencies 
            if ( nextRec.length()==1 ) {
                return nextRec;
            }
            boolean isRecord= nextRec.charAt(0)=='1' || nextRec.charAt(0)=='2';
            if ( isRecord ) {
                if ( doReformatTime ) {
                    int i= nextRec.indexOf(",");
                    start= TimeUtil.reformatIsoTime( nextRec.substring(0,i), start );
                    stop= TimeUtil.reformatIsoTime( nextRec.substring(0,i), stop );
                    doReformatTime= false;
                }
                while ( nextRec!=null && nextRec.compareTo(start)<0 ) {
                    nextRec= insb.readLine();
                    if ( nextRec==null ) return null;
                }
                if ( nextRec.substring(0,stop.length()).compareTo(stop)>=0 ) {
                    return null;
                }
                if ( !nextRec.endsWith("\n") ) nextRec= nextRec+"\n";
            }
            return nextRec;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int bytesRead= read( b, 0, b.length );        
            //totalBytesRead+=bytesRead;
            return bytesRead;
        }


        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if ( nextRec==null ) {
                nextRec= readNextRec();
                recChar= 0;
            }
            if ( nextRec==null ) {
                return -1;
            }
            if ( len < nextRec.length()-recChar ) {
                System.arraycopy( nextRec.getBytes( charset ), recChar, b, off, len );
                recChar= recChar + b.length;
                totalBytesRead+=len;
                return len;
            } else {
                int ll= Math.min( len, nextRec.length()-recChar );
                System.arraycopy( nextRec.getBytes( charset ), recChar, b, off, ll );
                recChar= recChar+ll;
                if ( recChar==nextRec.length() ) {
                    nextRec= readNextRec();
                    recChar= 0;
                }
                totalBytesRead+=ll;
                return ll;
            }        
        }

        @Override
        public int read() throws IOException {
            byte[] buf= new byte[1];
            int bytesRead= read(buf);
            if ( bytesRead==1 ) {
                totalBytesRead+=bytesRead;
                return buf[0];
            } else {
                return -1;
            }
        }

        @Override
        public void close() throws IOException {
            String s= insb.readLine();
            while ( s!=null ) { // empty the input, since it might be reading from a URL and Teeing to the cache.
                s= insb.readLine();
            }
            insb.close();
        }
        
    }
    
}
