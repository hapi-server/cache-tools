
```
pip install -e .
python server.py
```

Then open http://localhost:8005/?url=https://cdaweb.gsfc.nasa.gov/hapi/data?id=AC_H3_MFI&parameters=BGSEc&time.min=1999-01-01T00:00:00Z&time.max=1999-01-01T03:00:00.000Z

Code does not yet call the java command line cache program. It only calls `curl` with the url and streams the result.

