
COMMAND=python hapi-cache-write.py

all:
	make clean
	-@ pkill -f "python -m http.server 5555"
	make hapi_util
	make test-File-1a
	make test-File-1b
	make test-File-1c
	make test-File-1d
	make test-URL-1a
	make test-URL-1b
	make test-Stdin-1a
	make test-Stdin-1b
	make test-GzStdin-1a
	make test-GzStdin-1b
	make test-GzFile-1a
	make test-File-GzStdout-1a	
	@ echo ""
	@ echo "All tests passed."
	@ echo ""

clean:
	rm -rf /tmp/hapi-cache
	rm -f log/*

hapi_util:
	python hapi_util.py

# First day does not start at 00:00:00.000000000 or equivalent.
# Only one daily file written.
test-File-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--info input/hapi-cache-$@.json \
		--file input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-File-1a except --info and --file files gzipped.
test-GzFile-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--info input/hapi-cache-$@.json \
		--file input/hapi-cache-$@.txt.gz \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@


# Same as test-File-1a except first line starts at 00:00:00.000000000 or equivalent
# Two daily files written.
test-File-1b:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--info input/hapi-cache-$@.json \
		--file input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-File-1b except gzip files written
test-File-GzStdout-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--info input/hapi-cache-$@.json \
		--file input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		--gzip
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-File-1a except input file contains header and data
test-File-1c:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--file input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-File-1b except input file contains header and data
test-File-1d:
	@ mkdir -p /tmp/hapi-cache/$@
	$(COMMAND) \
		--file input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-File-1a except URL given for --info and --url given instead
# of --file
test-URL-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	python -m http.server 5555 & sleep 1
	$(COMMAND) \
		--info http://localhost:5555/input/hapi-cache-$@.json \
		--url http://localhost:5555/input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@
	pkill -f "python -m http.server 5555"

# Same as test-URL-1a except no --info given (because it is in data file)
test-URL-1b:
	@ mkdir -p /tmp/hapi-cache/$@
	python -m http.server 5555 & sleep 1
	$(COMMAND) \
		--url http://localhost:5555/input/hapi-cache-$@.txt \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@
	pkill -f "python -m http.server 5555"

# Same as test-File-1a except URL given for --info and --url given instead
# of --file.
test-Stdin-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	cat input/hapi-cache-$@.txt | $(COMMAND) \
		--info input/hapi-cache-$@.json \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-Stdin-1a except no --info given (because it is in data file)
test-Stdin-1b:
	@ mkdir -p /tmp/hapi-cache/$@
	cat input/hapi-cache-$@.txt | $(COMMAND) \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

# Same as test-Stdin-1a except input is gzipped
test-GzStdin-1a:
	@ mkdir -p /tmp/hapi-cache/$@
	cat input/hapi-cache-$@.txt | gzip | $(COMMAND) \
		--info input/hapi-cache-$@.json \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

test-GzStdin-1b:
	@ mkdir -p /tmp/hapi-cache/$@
	cat input/hapi-cache-$@.txt | gzip | $(COMMAND) \
		--log log/hapi-cache-$@.log \
		--dir /tmp/hapi-cache/$@ \
		> /tmp/hapi-cache/$@/stdout
	cat log/hapi-cache-$@.log
	diff -r hapi-cache/$@ /tmp/hapi-cache/$@

