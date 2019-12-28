# BASCAT

A C program to decode tokenized GWBASIC/BASICA files.  There are a few of them
around the internet, and I like to be able to look at the code without running
gwbasic in a VM.

If you look around github, I have made versions in several languages (rust, c#, scala, etc)
for fun.  Obviously, the C version is the fastest.  It memory-maps the file, which
I don't do in the managed-language versions.

This is the `win32/UWP` branch in the repo... the version on the master branch
is unixy.




