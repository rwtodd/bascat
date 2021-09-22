SOURCES=main.c

all: bascat bascat.1.gz

bascat: $(SOURCES) config.h
	$(CC) $(CFLAGS) -o $@ $(filter %.c, $^)

config.h: config.h.in GNUmakefile
	sed -e 's:@VERSION@:$(VERSION):g; s:@DC_PATH@:$(DC_PATH):g' < $< > $@

bascat.1.gz: bascat.1.in GNUmakefile
	sed -e 's:@VERSION@:$(VERSION):g' < $< | gzip -9 > $@

indent: $(SOURCES)
	indent $(filter %.c, $^) --no-tabs

clean:
	-rm -f bascat config.h bascat.1.gz

check: bascat
	./bascat "$(BASE)/tests/NEPTUNE.gwbas" | diff --from-file="$(BASE)/tests/NEPTUNE.txt" -

install:
	mkdir -p "$(PREFIX)/bin" "$(PREFIX)/share/man/man1"
	cp bascat "$(PREFIX)/bin"
	cp bascat.1.gz "$(PREFIX)/share/man/man1"

.PHONY: all check indent clean install
