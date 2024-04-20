
package hapi.cache;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * utility methods for interacting with HAPI servers
 * @author jbf
 */
public class HapiUtil {

    /**
     * return the number of bytes each record of the info will take.
     * @param info
     * @return
     * @throws IllegalArgumentException if info is not parseable
     */
    public static int bytesPerRec(JSONObject info) throws IllegalArgumentException {
        
        int recordLengthBytes = 0;
        JSONArray pds;
        try {
            pds = info.getJSONArray("parameters");
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
        for (int i = 0; i < pds.length(); i++) {
            try {
                JSONObject param = pds.getJSONObject(i);
                String type = param.getString("type");  //TODO: verify exception if type is not found.
                int length = param.optInt("length", 1);
                if (param.has("size")) {
                    JSONArray dims = param.getJSONArray("size");
                    for (int j = 0; j < dims.length(); j++) {
                        length = length * dims.getInt(j);
                    }
                }
                switch (type) {
                    case "isotime" -> recordLengthBytes += length;
                    case "string" -> recordLengthBytes += length;
                    case "double" -> recordLengthBytes += 8 * length;
                    case "int" -> recordLengthBytes += 4 * length;
                    default -> {
                    }
                }
            } catch (JSONException ex) {
                Logger.getLogger(ParameterSubsetBinaryDataInputStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return recordLengthBytes;
    }
    
}
