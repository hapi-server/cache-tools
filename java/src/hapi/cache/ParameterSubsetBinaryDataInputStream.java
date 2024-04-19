
package hapi.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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
     * return the number of bytes each record of the info will take.
     * @param info
     * @return
     * @throws JSONException 
     */
    protected static int bytesPerRec( JSONObject info ) throws JSONException {
        int recordLengthBytes = 0;

        JSONArray pds= info.getJSONArray("parameters");
        
        for (int i = 0; i < pds.length(); i++) {
            try {
                JSONObject param= pds.getJSONObject(i);
                String type= param.getString("type");
                int length= param.optInt("length",1);
                if ( param.has("size") ) {
                    JSONArray dims= param.getJSONArray("size");
                    for ( int j=0; j<dims.length(); j++ ) {
                        length= length * dims.getInt(j);
                    }
                }
                if ( type.equals("isotime") ) {
                    recordLengthBytes += length;
                } else if ( type.equals("string") ) {
                    recordLengthBytes += length;
                } else if ( type.equals("double") ) {
                    recordLengthBytes += 8 * length;
                } else if ( type.equals("int") ) {
                    recordLengthBytes += 4 * length;
                }
                
            } catch (JSONException ex) {
                Logger.getLogger(ParameterSubsetBinaryDataInputStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return recordLengthBytes;
    }
    
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
        try {
            nextRec= new byte[ bytesPerRec(info) ];
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
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
