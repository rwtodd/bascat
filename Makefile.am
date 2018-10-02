SUBDIRS=. tests

bin_PROGRAMS=bascat
bascat_SOURCES=main.c
info_TEXINFOS=bascat.texi

indent: $(bascat_SOURCES)
	indent $(bascat_SOURCES) --no-tabs

.PHONY: indent
