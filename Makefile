booknumbers := 1 2 3 4 5 6
books := $(foreach num,$(booknumbers),book$(num).zip)
illus := $(foreach num,$(booknumbers),illus$(num).zip)

.PHONY: build clean

build: flands.jar $(books) $(illus)

flands.jar:
	javac flands/*.java
	jar cfm0 flands.jar MANIFEST.MF flands/*.class flands/resources/*.class flands/resources/*.properties

%.zip:
	zip -jq $@ $(subst .zip,/*,$@)

clean:
	rm -f flands.jar /*.class flands/resources/*.class *.zip
