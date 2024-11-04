
package hapi.cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * read from the source input stream, inserting comment (#) characters before
 * each line.  This is needed when inserting the header.  This also does the
 * parameter subset operation on the header.
 * @author jbf
 * @see ParameterSubsetCsvDataInputStream which does a similar modification to the stream
 */
public class PrepHeaderInputStreamProvider implements InputStreamProvider {

    JSONObject header;
    boolean addComment;
    
    /**
     * 
     * @param parameterNames 
     * @param addComment add a hash comment character (#) before the header
     * @param ins
     */
    public PrepHeaderInputStreamProvider( String[] parameterNames, boolean addComment, InputStream ins ) {
        try {
            this.addComment= addComment;
            String json= new String( ins.readAllBytes(), "UTF-8" );
            ins.close();
            json= HapiUtil.subsetParameters( json, parameterNames );
            JSONObject jo= new JSONObject( json );
            header= jo;
        } catch (IOException | JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public InputStream openInputStream() throws IOException {
        try {
            String result= header.toString(4);
            BufferedReader reader= new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes("UTF-8") )));
            StringBuilder sb= new StringBuilder();
            String line= reader.readLine();
            while ( line!=null ) {
                if ( addComment ) {
                    sb.append("# ");
                }
                sb.append(line);
                sb.append("\n");
                line= reader.readLine();
            }
            return new ByteArrayInputStream( sb.toString().getBytes("UTF-8") );
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
