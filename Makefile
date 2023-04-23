BOOKS=1 2 3 4 5 6

.PHONY: build clean

build:
	javac flands/*.java
	jar cfm0 flands.jar MANIFEST.MF flands/*.class flands/resources/*.class flands/resources/*.properties
	for i in $(BOOKS); do \
		zip -jq book$$i.zip book$$i/*; \
		zip -jq illus$$i.zip illus$$i/*; \
	done

clean:
	rm -f flands/*.class flands/resources/*.class *.zip
