CC ::= gcc
CFLAGS ::= -O2 -march=native
PREFIX ::= /usr

srcs ::= main.c

bascat: $(srcs) config.h
	$(CC) $(CFLAGS) -o $@ $(srcs)

indent: $(bascat_SOURCES)
	indent $(srcs) --no-tabs

clean:
	-@rm -f bascat

check:   bascat
	./bascat tests/NEPTUNE.gwbas | diff --from-file=tests/NEPTUNE.txt -

install: bascat
	mkdir -p $(PREFIX)/bin $(PREFIX)/share/man/man1
	cp bascat $(PREFIX)/bin
	cp bascat.1 $(PREFIX)/share/man/man1

.PHONY: indent clean check install
