# EDITABLE SECTION-----------------------
CFLAGS?=-O2 -g
PREFIX?=/usr/local
BINDIR?=$(PREFIX)/bin
MANDIR?=$(PREFIX)/share/man/man1

# READ-ONLY SECTION----------------------
.PHONY: clean install

bascat: main.c
	$(CC) $(CFLAGS) -o $@ $< 

install: bascat bascat.1
	mkdir -p $(BINDIR)
	mkdir -p $(MANDIR)
	cp bascat $(BINDIR)
	cp bascat.1 $(MANDIR)

clean:
	-rm bascat
