
package hapi.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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
    private final long lastModifiedRequirement;

    /** 
     * Standard Constructor
     * @param aCacheDirective controls for cache operation
     */
    public HapiCache2024(CacheDirective aCacheDirective) {
        cacheDirective = aCacheDirective;
        Duration d= cacheDirective.getStaleAfterAsDuration();
        if ( d!=null ) {
            lastModifiedRequirement =  Instant.now().toEpochMilli() 
                - d.getSeconds()*1000 - d.getNano()/1000000;
        } else {
            lastModifiedRequirement = Long.MIN_VALUE;
        }
    }

    private HapiRequest parseHapiRequest( URL tmpUrl ) throws MalformedURLException {
        URL url= new URL( tmpUrl.getProtocol(), tmpUrl.getHost(), tmpUrl.getFile() );
        int ihapi= tmpUrl.getFile().lastIndexOf( "hapi" );
        URL host= new URL( tmpUrl.getProtocol(), tmpUrl.getHost(), tmpUrl.getFile().substring(0,ihapi+4) );
        String start=null,stop=null,dataset=null,parameters=null,format="csv",include=null;
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
                    case "include" -> include= v;
                    default -> throw new IllegalArgumentException("unsupported argument: "+s);
                }
            }
        }
        return new HapiRequest(url, host, query, dataset, start, stop, parameters, format, include);
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
     * @param exactTime find the file using the exact time, not a superset.
     * @param exactParams find the file using the exact parameters, not a superset.
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
     * return an InputStream for the URL.  This might come from the remote site or from a locally cached file.  This
     * routine is called once per HAPI call, so multiple days will be supported with ContatenateInputStream, for example.
     * @param tmpUrl the HAPI call
     * @return
     * @throws IOException 
     */
    protected InputStream getInputStream( URL tmpUrl ) throws IOException {
        HapiRequest request= parseHapiRequest(tmpUrl);
        String path= request.url().getPath();
        
        if ( path.endsWith("data") ) {
            String format= request.format();
            switch (format) {
                case "csv":
                    return getInputStreamCSV(tmpUrl);
                case "binary":
                    return getInputStreamBinary(tmpUrl);
                default:
                    throw new IllegalArgumentException("unsupported format exception: "+request.format());
            }
            
        } else if ( path.endsWith("info") ) {
            try {
                CacheHit hit= pathForUrl( request, false, false );
                assert ( hit.files.length!=1 );
                File base = cacheDirective.rootCacheDir();
                File cacheFile= new File( base +  File.separator + hit.files[0] );
                if ( cacheFile.exists() && hit.files.length==1 && cacheFile.lastModified()>lastModifiedRequirement ) {
                    return new FileInputStream(cacheFile);
                } else {
                    maybeMkdirsForFile(cacheFile);
                    return new TeeInputStreamProvider( new URLInputStreamProvider(tmpUrl),cacheFile ).openInputStream();
                }
            } catch ( ParseException ex ) {
                throw new IllegalArgumentException(ex);
            }
        } else if ( path.endsWith("catalog") || path.endsWith("capabilities") ||  path.endsWith("about") ) {
            try {
                CacheHit hit= pathForUrl( request, false, false );
                assert ( hit.files.length!=1 );
                File base = cacheDirective.rootCacheDir();
                File cacheFile= new File( base +  File.separator + hit.files[0] );
                if ( cacheFile.exists() && hit.files.length==1 && cacheFile.lastModified()>lastModifiedRequirement ) {
                    return new FileInputStream(cacheFile);
                } else {
                    maybeMkdirsForFile(cacheFile);
                    return new TeeInputStreamProvider( new URLInputStreamProvider(tmpUrl),cacheFile ).openInputStream();
                }
            } catch ( ParseException ex ) {
                throw new IllegalArgumentException(ex);
            }
        } else {
            throw new IllegalArgumentException("not supported: "+path);
        }
        
    }
    
    /**
     * return the info which should be at the top of the data request, and used to
     * parse a stream.  This may have a subset of the parameters.
     * @param request
     * @return
     * @throws IOException 
     */
    private String infoJsonForData( HapiRequest request ) throws IOException {
        URL infoUrl= infoForData(request);
        InputStream ins= infoUrl.openStream();
        byte[] infoBytes= ins.readAllBytes();
        String infoString= new String( infoBytes, "UTF-8" );
        if ( request.parameters()!=null ) {
            return HapiUtil.subsetParameters( infoString, request.parameters().split(",",-2) );  //time always
        } else {
            return infoString;
        }
    }
    
    /**
     * return the info URL for the data request
     * @param request
     * @return 
     */
    private URL infoForData(HapiRequest request) {
        try {
            return new URL( request.host() + "/info?id="+request.dataset() );
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("unable to form URL");
        }
    }
    
    /**
     * return the InputStream for the URL.  This might be sourced by URL.getInputStream, or
     * maybe from files, or a combination of both.
     * @param tmpUrl
     * @return
     * @throws IOException 
     */
    private InputStream getInputStreamCSV(URL tmpUrl) throws IOException {
        try {
            File base = cacheDirective.rootCacheDir();
            HapiRequest request= parseHapiRequest(tmpUrl);
            String[] parameters;
            if ( request.parameters()==null ) {
                parameters= null;
            } else {
                parameters= request.parameters().split(",");
            }
            CacheHit hit=pathForUrl(request,true,true);
            String path= hit.files[0];
            File cacheFile= new File( base +  File.separator + path );
            if ( cacheFile.exists() && hit.files.length==1 && cacheFile.lastModified()>lastModifiedRequirement ) {
                if ( "header".equals(request.include()) ) {
                    URL headerUrl= infoForData(request);
                    InputStream ins= getInputStream(headerUrl);
                    return new ConcatenateInputStream( 
                        new PrepHeaderInputStreamProvider(parameters,true,ins), new SimpleInputStreamProvider( new FileInputStream(cacheFile) ) );
                } else {
                    return new FileInputStream(cacheFile);
                }
            } else {
                CacheHit hit2=pathForUrl(request,false,true);
                StringBuilder sdataUrl= new StringBuilder( request.host().toString() );
                sdataUrl.append("/data?id=").append(request.dataset())
                    .append("&start=").append(request.start())
                    .append("&stop=").append(request.stop());
                if ( request.parameters()!=null ) {
                    sdataUrl.append("&parameters=").append(request.parameters());
                }
                InputStreamProvider[] ins= new InputStreamProvider[hit2.files.length];;
                URL dataUrl= new URL(sdataUrl.toString());
                if ( hit2.files.length==1 && hit2.subsetTime==false && hit2.subsetParameters==false ) {
                    File cacheFile2= new File( base +  File.separator + hit2.files[0] );
                    if ( cacheFile2.exists() && cacheFile.lastModified()>lastModifiedRequirement ) {
                        maybeMkdirsForFile(cacheFile);
                        ins[0]= new TeeInputStreamProvider( new URLInputStreamProvider(dataUrl),cacheFile2 ); //TODO: huh?
                    } else {
                        maybeMkdirsForFile(cacheFile2);
                        ins[0]= new TeeInputStreamProvider( new URLInputStreamProvider(dataUrl),cacheFile2 );
                    }
                } else {
                    for ( int i=0; i<hit2.files.length; i++ ) {
                        File cacheFile2= new File( base +  File.separator + hit2.files[i] );
                        String start= request.start();
                        String stop= request.stop();
                        if ( cacheFile2.exists() && cacheFile2.lastModified()>lastModifiedRequirement ) {
                            ins[i]= new TimeSubsetCsvDataInputStreamProvider( start, stop, new FileInputStreamProvider(cacheFile2) );
                        } else {
                            maybeMkdirsForFile(cacheFile2);
                            ins[i]= new TimeSubsetCsvDataInputStreamProvider( start, stop, new TeeInputStreamProvider( new URLInputStreamProvider(hit2.urls[i]),cacheFile2) );
                        }
                    }
                }
                
                if ( "header".equals(request.include()) ) {
                    InputStreamProvider[] ins2= new InputStreamProvider[1+hit2.files.length];
                    URL headerUrl= infoForData(request);
                    InputStream headerIns= getInputStream(headerUrl);
                    ins2[0]= new PrepHeaderInputStreamProvider(parameters,true,headerIns);
                    System.arraycopy(ins, 0, ins2, 1, ins.length);
                    ins= ins2;
                }

                if ( ins.length==1 ) {
                    return ins[0].openInputStream();
                } else {
                    return new ConcatenateInputStream( ins );
                }
                    
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * return the InputStream for the URL.  This might be sourced by URL.getInputStream, or
     * maybe from files, or a combination of both.
     * @param tmpUrl
     * @return
     * @throws IOException 
     */
    private InputStream getInputStreamBinary(URL tmpUrl) throws IOException {
        try {
            File base = cacheDirective.rootCacheDir();
            HapiRequest request= parseHapiRequest(tmpUrl);
            String[] parameters;
            if ( request.parameters()==null ) {
                parameters= null;
            } else {
                parameters= request.parameters().split(",");
            }
            CacheHit hit=pathForUrl(request,true,true);
            String path= hit.files[0];
            File cacheFile= new File( base +  File.separator + path );
            if ( cacheFile.exists() && hit.files.length==1 && cacheFile.lastModified()>lastModifiedRequirement ) {
                if ( "header".equals(request.include()) ) {
                    URL headerUrl= infoForData(request);
                    InputStream ins= getInputStream(headerUrl);
                    return new ConcatenateInputStream( 
                        new PrepHeaderInputStreamProvider(parameters,true,ins), new SimpleInputStreamProvider( new FileInputStream(cacheFile) ) );
                } else {
                    return new FileInputStream(cacheFile);
                }
            } else {
                CacheHit hit2=pathForUrl(request,false,true);
                StringBuilder sdataUrl= new StringBuilder( request.host().toString() );
                sdataUrl.append("/data?id=").append(request.dataset())
                    .append("&format=").append(request.format())
                    .append("&start=").append(request.start())
                    .append("&stop=").append(request.stop());
                if ( request.parameters()!=null ) {
                    sdataUrl.append("&parameters=").append(request.parameters());
                }
                InputStreamProvider[] ins= new InputStreamProvider[hit2.files.length];;
                URL dataUrl= new URL(sdataUrl.toString());
                if ( hit2.files.length==1 && hit2.subsetTime==false && hit2.subsetParameters==false ) {
                    File cacheFile2= new File( base +  File.separator + hit2.files[0] );
                    if ( cacheFile2.exists() && cacheFile.lastModified()>lastModifiedRequirement ) {
                        maybeMkdirsForFile(cacheFile);
                        ins[0]= new TeeInputStreamProvider( new URLInputStreamProvider(dataUrl),cacheFile2 ); //TODO: huh?
                    } else {
                        maybeMkdirsForFile(cacheFile2);
                        ins[0]= new TeeInputStreamProvider( new URLInputStreamProvider(dataUrl),cacheFile2 );
                    }
                } else {
                    String infoJson= infoJsonForData(request);
                    JSONObject info;
                    try {
                        info = new JSONObject(infoJson);
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                    for ( int i=0; i<hit2.files.length; i++ ) {
                        File cacheFile2= new File( base +  File.separator + hit2.files[i] );
                        String start= request.start();
                        String stop= request.stop();
                        if ( cacheFile2.exists() && cacheFile2.lastModified()>lastModifiedRequirement ) {
                            ins[i]= new TimeSubsetBinaryDataInputStreamProvider( info, start, stop, new FileInputStreamProvider(cacheFile2) );
                        } else {
                            maybeMkdirsForFile(cacheFile2);
                            ins[i]= new TimeSubsetBinaryDataInputStreamProvider( info, start, stop, new TeeInputStreamProvider( new URLInputStreamProvider(hit2.urls[i]),cacheFile2) );
                        }
                    }
                }
                
                if ( "header".equals(request.include()) ) {
                    InputStreamProvider[] ins2= new InputStreamProvider[1+hit2.files.length];
                    URL headerUrl= infoForData(request);
                    InputStream headerIns= getInputStream(headerUrl);
                    ins2[0]= new PrepHeaderInputStreamProvider(parameters,true,headerIns);
                    System.arraycopy(ins, 0, ins2, 1, ins.length);
                    ins= ins2;
                }

                if ( ins.length==1 ) {
                    return ins[0].openInputStream();
                } else {
                    return new ConcatenateInputStream( ins );
                }
                    
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
}
