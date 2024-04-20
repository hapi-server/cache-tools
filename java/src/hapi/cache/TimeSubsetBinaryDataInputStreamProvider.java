
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hapiserver.TimeUtil;

/**
 * Suppose the cache file has data from 00:00 to 24:00, and you want
 * data from 10:00 to 12:00.  This will subset a HAPI Binary in time.
 * @author jbf
 */
public class TimeSubsetBinaryDataInputStreamProvider implements InputStreamProvider {

    String start;
    String stop;
    InputStreamProvider ins;
    int totalBytesRead;
    JSONObject info;
    
    public TimeSubsetBinaryDataInputStreamProvider( JSONObject info, String start, String stop, InputStreamProvider ins ) {
        this.start= start;
        this.stop= stop;
        this.ins= ins;
        this.totalBytesRead= 0;
        this.info= info;
    }
    @Override
    public InputStream openInputStream() throws IOException {
        try {
            return new TimeSubsetBinaryDataInputStream(info,start, stop, ins.openInputStream() );
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class TimeSubsetBinaryDataInputStream extends InputStream {
        InputStream ins;
        byte[] nextRec= null;

        /**
         * position within the record.  Note this does not support UTF-8 extensions! TODO: support this.
         */
        int recChar=-1;

        private byte[] start;
        private byte[] stop;
        
        private JSONObject info;
        private int timeLength;

        /**
         * we need to support $Y-$j as well as $Y-$m-$d for comparisons.
         */
        private boolean doReformatTime=true;
        Charset charset= Charset.forName("US-ASCII"); //TODO: UTF-8

        public TimeSubsetBinaryDataInputStream( JSONObject info, String start, String stop, InputStream ins ) throws JSONException {
            this.start= start.getBytes();
            this.stop= stop.getBytes();
            this.ins= ins;
            this.info= info;
            timeLength= info.getJSONArray("parameters").getJSONObject(0).getInt("length");
        }

        /**
         * read the next record within the start and stop times.
         * @return
         * @throws IOException 
         */
        private byte[] readNextRecAny() throws IOException {
            if ( nextRec==null ) {
                int reclength= HapiUtil.bytesPerRec(this.info);
                nextRec= new byte[reclength];
            }
            int bytesRead=0;
            while ( bytesRead<nextRec.length ) {
                int b= ins.read(nextRec,bytesRead,nextRec.length-bytesRead);
                if ( b==-1 ) return null;
                bytesRead+= b;
            }
            return nextRec;
        }
    
        int compare( byte[] nextRec, byte[] time ) {
            int n= Math.min( nextRec.length, time.length );
            for ( int i=0; i<n; i++ ) {
                int diff= nextRec[i] - time[i];
                if ( diff!=0 ) {
                    return diff;
                }
            }
            return 0;
        }
        
        /**
         * read the next record within the start and stop times.
         * @return the record, or null if there is not another
         * record available.
         * @throws IOException 
         */
        private byte[] readNextRec() throws IOException {
            nextRec= readNextRecAny();
            if ( nextRec==null ) return null;

            boolean isRecord= nextRec[0]=='1' || nextRec[0]=='2'; // 1999 or 2000 or ...
            if ( isRecord ) {
                if ( doReformatTime ) {
                    int i= timeLength;
                    String atime= new String( nextRec, 0, i );
                    start= TimeUtil.reformatIsoTime( atime, new String(start,"UTF-8") ).getBytes();
                    stop= TimeUtil.reformatIsoTime( atime, new String(stop,"UTF-8") ).getBytes();
                    doReformatTime= false;
                }
                while ( nextRec!=null && compare( nextRec, start )<0 ) {
                    nextRec= readNextRecAny();
                    if ( nextRec==null ) return null;
                }
                if ( compare( nextRec, stop )>=0 ) {
                    return null;
                }
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
            if ( len < nextRec.length-recChar ) {
                System.arraycopy( nextRec, recChar, b, off, len );
                recChar= recChar + b.length;
                totalBytesRead+=len;
                return len;
            } else {
                int ll= Math.min( len, nextRec.length-recChar );
                System.arraycopy( nextRec, recChar, b, off, ll );
                recChar= recChar+ll;
                if ( recChar==nextRec.length ) {
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
            int ch= ins.read();
            while ( ch!=-1 ) { // empty the input, since it might be reading from a URL and Teeing to the cache.
                ch= ins.read();
            }
            ins.close();
        }
        
    }
    
}
