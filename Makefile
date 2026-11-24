# Mirage — Java 17, sans bibliothèque
JAVAC ?= javac
JAVA  ?= java
JFLAGS = --release 17 -encoding UTF-8 -Xlint:all,-serial

SRC   := $(shell find src -name '*.java')
TESTS := $(shell find test -name '*.java')

.PHONY: all run test jar clean

all: build/classes/.stamp

build/classes/.stamp: $(SRC)
	@mkdir -p build/classes
	$(JAVAC) $(JFLAGS) -d build/classes $(SRC)
	@touch $@

run: all
	$(JAVA) -cp build/classes mirage.Main $(FILE)

test: all $(TESTS)
	@mkdir -p build/test
	$(JAVAC) $(JFLAGS) -cp build/classes -d build/test $(TESTS)
	$(JAVA) -Dstdout.encoding=UTF-8 -cp build/classes:build/test mirage.Tests

jar: all
	printf 'Main-Class: mirage.Main\n' > build/manifest.txt
	jar cfm build/mirage.jar build/manifest.txt -C build/classes mirage
	@echo "build/mirage.jar"

clean:
	rm -rf build
