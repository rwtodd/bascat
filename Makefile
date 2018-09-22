CFLAGS=-std=c11 -march=native -O3
bascat: main.c
	gcc $(CFLAGS) -o bascat main.c
