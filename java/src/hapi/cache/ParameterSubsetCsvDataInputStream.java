
package hapi.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Suppose the cache file has Time,A,B,C and you only want Time,B.  This
 * will subset the CSV stream.
 * @author jbf
 */
public class ParameterSubsetCsvDataInputStream extends InputStream {

    BufferedReader insb;
    String nextRec= null;
    int[] fields;
    int nfields;
    
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
     * @param fields fields of the original record to transmit
     * @param ins 
     */
    public ParameterSubsetCsvDataInputStream( int[] fields, InputStream ins ) {
        this.fields= fields;
        this.nfields= fields.length;
        insb= new BufferedReader(new InputStreamReader(ins));
    }

    /**
     * read the next record within the start and stop times.
     * @return
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
            String[] ss= nextRec.split(",",-2);
            StringBuilder sb= new StringBuilder(ss[0]);
            for ( int i=1; i<nfields; i++ ) {
                sb.append(',');
                sb.append(ss[fields[i]]);
            }
            return sb.toString();
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
        if ( len < nextRec.length()-recChar ) {
            System.arraycopy( nextRec.getBytes( charset ), recChar, b, off, len );
            recChar= recChar + b.length;
            return len;
        } else {
            int ll= Math.min( len, nextRec.length()-recChar );
            System.arraycopy( nextRec.getBytes( charset ), recChar, b, off, ll );
            recChar= recChar+ll;
            if ( recChar==nextRec.length() ) {
                nextRec= readNextRec();
                recChar= 0;
            }
            return ll;
        }        
    }
    
    @Override
    public int read() throws IOException {
        if ( recChar==nextRec.length() ) {
            nextRec= readNextRec();
            recChar= 0;
        }
        if ( nextRec==null ) {
            return -1;
        } else {
            char ch= nextRec.charAt(recChar);
            recChar++;
            return ch;
        }
    }
    
}
