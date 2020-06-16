bin_PROGRAMS=bascat
bascat_SOURCES=main.c
info_TEXINFOS=bascat.texi
dist_man1_MANS=bascat.1
dist_check_SCRIPTS=tests/compare.sh
dist_check_DATA=tests/NEPTUNE.txt tests/NEPTUNE.gwbas
TESTS=$(dist_check_SCRIPTS)

indent: $(bascat_SOURCES)
	indent $(bascat_SOURCES) --no-tabs

.PHONY: indent
