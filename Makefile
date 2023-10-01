books := 1 2 3 4 5 6
bookZips := $(foreach num,$(books),book$(num).zip)
illuZips := $(foreach num,$(books),illus$(num).zip)

.PHONY: build clean

build: flands.jar $(bookZips) $(illuZips)

flands.jar:
	javac flands/*.java
	jar cfm0 flands.jar MANIFEST.MF flands/*.class flands/resources/*.class flands/resources/*.properties

%.zip:
	zip -jq $@ $(subst .zip,/*,$@)

clean:
	rm -f flands.jar **/**/*.class *.zip
