/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hapi.cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.hapiserver.TimeUtil;

/**
 *
 * @author jbf
 */
public class TimeSubsetCsvDataInputStream extends InputStream {

    InputStream ins;
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
        this.ins= ins;
        this.start= start;
        this.stop= stop;
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
                nextRec=null;
                return null;
            }
            if ( !nextRec.endsWith("\n") ) nextRec= nextRec+"\n";
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
        byte[] buf= new byte[1];
        int bytesRead= read(buf);
        if ( bytesRead==1 ) {
            return buf[0];
        } else {
            return -1;
        }
    }
    
}
