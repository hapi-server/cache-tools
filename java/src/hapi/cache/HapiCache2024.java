
package hapi.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import org.hapiserver.TimeUtil;

/**
 * Implementation of the HAPI Cache as of 2024.  This is the similar
 * to the original specification, but also:
 * <ul>
 * <li> multi-parameter data gets are stored together, and stitching is no longer done
 * <li> check for direct hits: same start, stop, and parameters.
 * </ul>
 * This is quite simple right now, lacking original features like:
 * <ul>
 * <li> trim in time when a superset of the data is found (time superset)
 * <li> trim parameters when a superset of the data is found (parameter superset)
 * <li> break up long requests into multiple cache files (granularizing)
 * <li> detect when one cache entry can be used to implement another (redundancy)
 * </ul>
 * And new features like:
 * <ul>
 * <li>freshness controls 
 * </ul>
 * @author jbf
 */
public class HapiCache2024 {
    private static HapiCache2024 instance= new HapiCache2024();
    
    File base= new File("/home/jbf/hapi"); //TODO: this will change
    
    public static HapiCache2024 instance() {
        return instance;
    }

    private HapiRequest parseHapiRequest( URL tmpUrl ) throws MalformedURLException {
        URL url= new URL( tmpUrl.getProtocol(), tmpUrl.getHost(), tmpUrl.getFile() );
        String start=null,stop=null,dataset=null,parameters=null,format=null;
        String query= tmpUrl.getQuery();
        if ( query!=null ) {
            String[] ss= query.split("&");
            for ( String s: ss ) {
                String n,v=null;
                int i= s.indexOf("=");
                if ( i==-1 ) {
                    n= s;
                } else {
                    n= s.substring(0,i);
                    v= s.substring(i+1);
                }
                switch ( n ) {
                    case "start", "time.min" -> start= v;
                    case "stop", "time.max" -> stop= v;
                    case "dataset", "id" -> dataset= v;
                    case "parameters" -> parameters= v;
                    case "format" -> format= v;
                    default -> throw new IllegalArgumentException("unsupported argument: "+s);
                }
            }
        }
        return new HapiRequest(url, query, dataset, start, stop, parameters, format);
    }
    
    /**
     * return the dataset name made into a filesystem-safe address.
     * @param dataset
     * @return 
     */
    private String fileSystemSafeDataSetName( String dataset ) {
        return dataset.replaceAll(" ","+");
    }
    
    private static String reformatTime( String t ) {
        try {
            int[] ii= TimeUtil.parseISO8601Time(t);
            return String.format("%04d%02d%02dT%02d%02d%02dZ", ii[0], ii[1], ii[2], ii[3], ii[4], ii[5] );
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return the relative path within the cache for the URL.
     * TODO: this is under-implemented. For example this needs to 
     * support Windows.
     * @param url
     * @param exact if true, then return the exact timerange, otherwise return the file containing.
     * @return 
     */
    private String pathForUrl( HapiRequest request, boolean exact ) throws ParseException {
        String sep= File.separator;
        String host= request.url().getHost();
        if ( request.url().getPort()!=-1 ) {
            host = host + ":" +request.url().getPort();
        }
        host = request.url().getProtocol() + sep + host;
        String path= request.url().getPath();
        if ( path.endsWith("info") ) {
            return host + sep + path + sep
                    + fileSystemSafeDataSetName(request.dataset())
                    + ".json";
        } else if ( path.endsWith("data") ) {
            int[] istart= TimeUtil.parseISO8601Time( request.start() );
            int[] istop= TimeUtil.parseISO8601Time( request.stop() );
            String start= String.format("%04d%02d%02dT%02d%02d%02dZ", 
                istart[0], istart[1], istart[2], istart[3], istart[4], istart[5] );
            String stop= String.format("%04d%02d%02dT%02d%02d%02dZ", 
                istop[0], istop[1], istop[2], istop[3], istop[4], istop[5] );
            int[] diff= TimeUtil.subtract(istop, istart);

            String format= request.format()==null ? "csv" : request.format();
            String params= request.parameters()==null ? "" : request.parameters();
            
            String year_month= String.format( "%04d"+sep+"%02d", istart[0], istart[1] );
            
            if ( (!exact) || ( diff[0]==0 && diff[1]==0 && start.endsWith("000000Z") && stop.endsWith("000000Z") ) ) {
                return host + sep + path + sep
                        + fileSystemSafeDataSetName(request.dataset()) + 
                        sep + year_month + sep + start.substring(0,8) + params + "." + format;
            } else {
                if ( istart[0]==istop[0] && istart[1]==istop[1] ) {
                    if ( params.length()>0 ) {
                        return host + sep + path + sep
                            + fileSystemSafeDataSetName(request.dataset()) + 
                            sep + year_month + sep + start + "_" + stop + "_" + params + "." + format;                
                    } else {
                        return host + sep + path + sep
                            + fileSystemSafeDataSetName(request.dataset()) + 
                            sep + year_month + sep + start + "_" + stop + "." + format;
                    }
                } else {
                    if ( params.length()>0 ) {
                        return host + sep + path + sep
                            + fileSystemSafeDataSetName(request.dataset()) + 
                            sep + start + "_" + stop + "_" + params + "." + format;                
                    } else {
                        return host + sep + path + sep
                            + fileSystemSafeDataSetName(request.dataset()) + 
                            sep + start + "_" + stop + "." + format;
                    }
                }
            }
            
        } else {
            return host + sep + path + ".json";
        }
    }
    
    InputStream getInputStream(URL tmpUrl) throws IOException {
        try {
            HapiRequest request= parseHapiRequest(tmpUrl);
            String path= pathForUrl(request,true);
            File cacheFile= new File( base +  File.separator + path );
            if ( cacheFile.exists() ) {
                return new FileInputStream(cacheFile);
            } else {
                String path2= pathForUrl(request,false);
                if ( path2.equals(path) ) {
                    FileOutputStream fout= new FileOutputStream(cacheFile);
                    return new TeeInputStream(tmpUrl.openStream(),fout);
                } else {
                    File cacheFile2= new File( base +  File.separator + path2 );
                    if ( cacheFile2.exists() ) {
                        String start= request.start();
                        String stop= request.stop();
                        return new TimeSubsetCsvDataInputStream( start, stop, new FileInputStream(cacheFile2) );
                    } else {
                        FileOutputStream fout= new FileOutputStream(cacheFile);
                        return new TeeInputStream(tmpUrl.openStream(),fout);
                    }
                }
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
