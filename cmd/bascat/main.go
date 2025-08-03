package main

import (
	"fmt"
	"io"
	"os"
	"github.com/rwtodd/bascat/basic"
)

func main() {

	var bytes []byte
	var err error

	switch len(os.Args) {
	case 1:
		bytes, err = io.ReadAll(os.Stdin)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Can't read stdin!: %s\n", err.Error())
			os.Exit(1)
		}
	case 2:
		bytes, err = os.ReadFile(os.Args[1])
		if err != nil {
			fmt.Fprintf(os.Stderr, "Can't read <%s>!: %s\n", os.Args[1], err.Error())
			os.Exit(1)
		}
	default:
		fmt.Fprintf(os.Stderr, "Usage: bascat [file]\n")
		os.Exit(2)
	}

	iter, err := basic.DecodeLines(bytes)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}

	for lno, contents := range iter {
		fmt.Printf("%d  %s\n", lno, contents)
	}
}
