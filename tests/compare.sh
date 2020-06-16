#!/bin/sh

./bascat ${srcdir}/tests/NEPTUNE.gwbas | diff --from-file=${srcdir}/tests/NEPTUNE.txt -
