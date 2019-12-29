CC ::= gcc
CFLAGS ::= -O2 -march=native
PREFIX ::= /usr

srcs ::= main.c

bascat: $(srcs) config.h
	$(CC) $(CFLAGS) -o $@ $(srcs)

indent: $(bascat_SOURCES)
	indent $(srcs) --no-tabs

clean:
	rm bascat

check:   bascat
	./bascat tests/NEPTUNE.gwbas | diff --from-file=tests/NEPTUNE.txt -

install: bascat
	cp bascat $(PREFIX)/bin

.PHONY: indent clean check install
