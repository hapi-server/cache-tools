
package hapi.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * <li> trim parameters when a superset of the data is found (parameter superset)
 * <li> trim data in time when cache granule contains more than the data requested.
 * <li> break up long requests into multiple cache files (granularizing)
 * <li> detect when one cache entry can be used to implement another (redundancy)
 * </ul>
 * And new features like:
 * <ul>
 * <li>freshness controls 
 * </ul>
 * Here is a list of issues to handle/check:
 * <ul>
 * <li>301/302 redirects (http->https) needs handling
 * <li>Don't cache error responses
 * </ul>
 * @author jbf
 */
public class HapiCache2024 {
    // Attributes
    private final CacheDirective cacheDirective;

    /** Standard Constructor */
    public HapiCache2024(CacheDirective aCacheDirective) {
        cacheDirective = aCacheDirective;
    }

    private HapiRequest parseHapiRequest( URL tmpUrl ) throws MalformedURLException {
        URL url= new URL( tmpUrl.getProtocol(), tmpUrl.getHost(), tmpUrl.getFile() );
        int ihapi= tmpUrl.getFile().lastIndexOf( "hapi" );
        URL host= new URL( tmpUrl.getProtocol(), tmpUrl.getHost(), tmpUrl.getFile().substring(0,ihapi+4) );
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
        return new HapiRequest(url, host, query, dataset, start, stop, parameters, format);
    }
    
    /**
     * return the dataset name made into a filesystem-safe address.  This is so that
     * file names are legible but also for security.  We could hash all the external-source
     * names, but then the cache would be opaque.
     * TODO: this really needs a good bit of attention and documentation, since other
     * cache readers will need this logic as well.
     * @param dataset
     * @return 
     */
    private String fileSystemSafeDataSetName( String dataset ) {
        return dataset.replaceAll(" ","+").replaceAll("\\.\\.+",".");
    }
    
    private Map<String,String> paramSplit( String params ) {
        String[] ss= params.split("&",-2);
        Map<String,String> result= new LinkedHashMap<>();
        for ( String s: ss ) {
            int i= s.indexOf("=");
            String n= s.substring(0,i);
            String v= s.substring(i+1);
            result.put(n,v);
        }
        return result;
    }
    
    private String paramJoin( Map<String,String> params ) {
        StringBuilder s= new StringBuilder();
        for ( Entry<String,String> e: params.entrySet() ) {
            s.append("&");
            s.append(e.getKey()).append("=").append(e.getValue());
        }
        return s.substring(1);
    }
        
    /**
     * for a "data" URL, which has the most degrees of freedom, find a cache file which can be used.
     * @param request
     * @param exactTime
     * @param exactParams
     * @return CacheHit structure showing the cache files to use, which may not necessarily be downloaded yet.
     * @throws ParseException 
     */
    private CacheHit pathForUrlData( HapiRequest request, boolean exactTime, boolean exactParams ) throws ParseException {
        String sep= File.separator;
        String host= request.url().getHost();
        if ( request.url().getPort()!=-1 ) {
            host = host + ":" +request.url().getPort();
        }
        host = request.url().getProtocol() + sep + host;
        String path= request.url().getPath();
        
        int[] istart= TimeUtil.parseISO8601Time( request.start() );
        int[] istop= TimeUtil.parseISO8601Time( request.stop() );
        String start= String.format("%04d%02d%02dT%02d%02d%02dZ", 
            istart[0], istart[1], istart[2], istart[3], istart[4], istart[5] );
        String stop= String.format("%04d%02d%02dT%02d%02d%02dZ", 
            istop[0], istop[1], istop[2], istop[3], istop[4], istop[5] );
        int[] diff= TimeUtil.subtract(istop, istart);

        String format= request.format()==null ? "csv" : request.format();
        String params= request.parameters()==null ? "" : request.parameters();

        params= fileSystemSafeDataSetName(params);
        if ( params.length()>0 ) params= "," + params;

        String year_month= String.format( "%04d"+sep+"%02d", istart[0], istart[1] );

        String basePath= host + sep + path + sep
                     + fileSystemSafeDataSetName(request.dataset()) + 
                     sep + year_month + sep;

        if ( diff[0]==0 && diff[1]==0 && diff[2]==1 && start.endsWith("000000Z") && stop.endsWith("000000Z") ) {
            // it's a one-day file
            CacheHit result= new CacheHit();
            result.files= new String[] { basePath + start.substring(0,8) + params + "." + format };
            result.urls= new URL[] { request.url() };
            result.subsetParameters= false;
            result.subsetTime=false;
            return result;
        } else {
            if ( exactTime ) {
                String file;
                if ( params.length()>0 ) {
                    file= basePath + start + "_" + stop + params + "." + format;                
                } else {
                    file= basePath + start + "_" + stop + "." + format;
                }
                CacheHit result= new CacheHit();
                result.files= new String[] { file };
                result.urls= new URL[] { request.url() };
                result.subsetParameters= false;
                result.subsetTime= false;
                return result;
            } else {
                String start1= String.format("%04d-%02d-%02dT%02d:%02d:%02dZ", 
                    istart[0], istart[1], istart[2], 0, 0, 0 );
                istop= TimeUtil.parseISO8601Time( TimeUtil.ceil(request.stop()) );
                String stop1= 
                    String.format("%04d-%02d-%02dT%02d:%02d:%02dZ", 
                    istop[0], istop[1], istop[2], 0, 0, 0 );
                String[] days= TimeUtil.countOffDays(start1, stop1);
                URL[] urls= new URL[days.length];
                for ( int i=0; i<days.length; i++ ) {
                    String start2= days[i];
                    String stop2= TimeUtil.nextDay(start2);
                    String day= days[i].substring(0,4) + days[i].substring(5,7) + days[i].substring(8,10);
                    days[i]= basePath + day + params + "." + format;  
                    URL url= request.url();
                    Map<String,String> pp= paramSplit(url.getQuery());
                    pp.put( "start", start2 );
                    pp.put( "stop", stop2);
                    try {
                        urls[i]= new URL( request.host() + "/data" + "?" + paramJoin(pp) );
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(HapiCache2024.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                CacheHit result= new CacheHit();
                result.files= days;
                result.urls= urls;
                result.subsetParameters= false;
                result.subsetTime= true;
                return result;
            }
        }
        
    }
    
    private class CacheHit {
        String[] files=null;
        URL[] urls= null;
        boolean subsetTime=false;
        boolean subsetParameters=false;
        boolean addHeader=false;
    }
    
    
    /**
     * return the relative path within the cache for the URL.
     * TODO: this is under-implemented.
     * @param url
     * @param exactTime if true, then return the exact timerange, otherwise return the file containing.
     * @param exactParams if true, then return the path with these exact parameters, otherwise return the file containing.
     * @return a CacheHit.
     */
    private CacheHit pathForUrl( HapiRequest request, boolean exactTime, boolean exactParams ) throws ParseException {
        String sep= File.separator;
        String host= request.url().getHost();
        if ( request.url().getPort()!=-1 ) {
            host = host + ":" +request.url().getPort();
        }
        host = request.url().getProtocol() + sep + host;
        String path= request.url().getPath();
        if ( path.endsWith("info") ) {
            path= host + sep + path + sep
                    + fileSystemSafeDataSetName(request.dataset())
                    + ".json";
        } else if ( path.endsWith("data") ) {
            CacheHit result= pathForUrlData(request, exactTime, exactParams);
            return result;
            
        } else {
            path= host + sep + path + ".json";
        }
        CacheHit result= new CacheHit();
        result.files= new String[] { path };
        result.subsetTime= !exactTime;
        result.subsetParameters= false;
        return result;
    }
    
    /**
     * make a directory for the file if it doesn't exist already.
     * @param cacheFile the file location.
     * @return true when the directory is made or when it exists already.
     * @throws IllegalArgumentException 
     */
    private boolean maybeMkdirsForFile( File cacheFile ) throws IllegalArgumentException {
        if ( cacheFile.getParentFile().exists() ) {
            return true;
        } else {
            if ( !cacheFile.getParentFile().mkdirs() ) {
                throw new IllegalArgumentException("unable to make cache directory: "+cacheFile.getParent());
            }
            return true;
        }
    }
    
    /**
     * return the InputStream for the URL.  This might be sourced by URL.getInputStream, or
     * maybe from files, or a combination of both.
     * @param tmpUrl
     * @return
     * @throws IOException 
     */
    InputStream getInputStream(URL tmpUrl) throws IOException {
        try {
            File base = cacheDirective.rootCacheDir();
            HapiRequest request= parseHapiRequest(tmpUrl);
            CacheHit hit=pathForUrl(request,true,true);
            String path= hit.files[0];
            File cacheFile= new File( base +  File.separator + path );
            if ( cacheFile.exists() && hit.files.length==1 ) {
                return new FileInputStream(cacheFile);
            } else {
                CacheHit hit2=pathForUrl(request,false,true);
                if ( hit2.files.length==1 && hit2.subsetTime==false && hit2.subsetParameters==false ) {
                    File cacheFile2= new File( base +  File.separator + hit2.files[0] );
                    maybeMkdirsForFile(cacheFile);
                    return new TeeInputStreamProvider( new URLInputStreamProvider(tmpUrl),cacheFile2 ).openInputStream();
                } else {
                    InputStreamProvider[] ins= new InputStreamProvider[hit2.files.length];
                    for ( int i=0; i<hit2.files.length; i++ ) {
                        File cacheFile2= new File( base +  File.separator + hit2.files[i] );
                        if ( cacheFile2.exists() ) {
                            String start= request.start();
                            String stop= request.stop();
                            ins[i]= new TimeSubsetCsvDataInputStreamProvider( start, stop, new FileInputStreamProvider(cacheFile2) );
                        } else {
                            maybeMkdirsForFile(cacheFile2);
                            
                            ins[i]= new TeeInputStreamProvider( new URLInputStreamProvider(hit2.urls[i]),cacheFile2);
                        }
                    }
                    
                    return new ConcatenateInputStream( ins );
                    
                }
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
