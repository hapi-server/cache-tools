
package hapi.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.codehaus.jettison.json.JSONObject;

/**
 * Suppose the cache file has Time,A,B,C and you only want Time,B.  This
 * will subset the Binary stream.
 * @author jbf
 */
public class ParameterSubsetBinaryDataInputStream extends InputStream {

    byte[] nextRec= null;
    int[] fields;
    int nfields;
    InputStream ins;
    
    /**
     * position within the record.  Note this does not support UTF-8 extensions! TODO: support this.
     */
    int recChar=-1;
    
    /**
     * we need to support $Y-$j as well as $Y-$m-$d for comparisons.
     */
    Charset charset= Charset.forName("US-ASCII"); //TODO: UTF-8
    
    /**
     * 
     * @param info the original info response, for all parameters
     * @param fields fields of the original record to transmit
     * @param ins 
     */
    public ParameterSubsetBinaryDataInputStream( JSONObject info, int[] fields, InputStream ins ) {
        this.fields= fields;
        this.nfields= fields.length;
        this.ins= ins;
        nextRec= new byte[ HapiUtil.bytesPerRec(info) ];
    }

    /**
     * read the next record within the start and stop times.
     * @return
     * @throws IOException 
     */
    private byte[] readNextRec() throws IOException {
        int bytesRead=0;
        while ( bytesRead<nextRec.length ) {
            int b= ins.read(nextRec,bytesRead,nextRec.length-bytesRead);
            if ( b==-1 ) return null;
            bytesRead+= b;
        }
        return nextRec;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return read( b, 0, b.length );        
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
            return len;
        } else {
            int ll= Math.min( len, nextRec.length-recChar );
            System.arraycopy( nextRec, recChar, b, off, ll );
            recChar= recChar+ll;
            if ( recChar==nextRec.length ) {
                nextRec= readNextRec();
                recChar= 0;
            }
            return ll;
        }        
    }
    
    @Override
    public int read() throws IOException {
        if ( recChar==nextRec.length ) {
            nextRec= readNextRec();
            recChar= 0;
        }
        if ( nextRec==null ) {
            return -1;
        } else {
            byte ch= nextRec[recChar];
            recChar++;
            return ch;
        }
    }
    
}
