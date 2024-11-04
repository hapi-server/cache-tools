# cache-tools
A collection of tools for reading, writing, and managing a cache of HAPI responses according to the [proposed cache specification](https://github.com/hapi-server/data-specification/wiki/cache-specification)

# Notes on the Java version
Jeremy has started transferring cache code from client-java to the Java repository.  This is not complete, and the code is not intended
to be used beyond initial development.  In particular:

* long requests need to be broken up into multiple requests
* No subsetting of cache hits is coded yet, though this will need to be done
* No attempt at cache freshness is coded yet.  It's either there or not there.

This is being coded to the somewhat undocumented cache spec we've been talking about over the past month.  One mentionable change is
that parameters are no longer stored individually.  This was problemmatic anyway and this will be easier to implement.  
