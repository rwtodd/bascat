package main

import (
	"bufio"
	"os"
        "io"
        "fmt"
)

const (
	first_byte  = 0xFF // first byte of the file (unencrypted)
	first_bcrpt = 0xFE // first byte of the file (encrypted)
	last_byte   = 0x1A // last byte of the file

	end_of_program = 0 // indicates there are no more lines
	end_of_line    = 0 // indicates the end of a line
)

type byteDripper interface {
	ReadByte() (c byte, err error)
}


func decoder(in byteDripper) (bd byteDripper, err error) {
	// check the first byte
	b, err := in.ReadByte()
        switch b {
        case first_byte:
             bd = in
	case first_bcrpt:
             bd = in // FIXME make a decryptor
        default:
 		fmt.Printf("This file is not a tokenized BAS file!")

        }
        return
}


func read_uint16(in byteDripper) (out uint16, err error) {
      b1, err := in.ReadByte()
      b2, err2 := in.ReadByte()

      out = (uint16(b2) << 8) | uint16(b1)
      if err == nil {
         err = err2
      }  
      return
}

func read_lineno(in byteDripper) (uint16, error) {
      // check the "next line" pointer for 0, return EOF if
      // we find it.
      ptr, err := read_uint16(in)
      if ptr == 0 {
          return ptr, io.EOF
      }  else if err != nil {
          return ptr, err
      }

      // return the line number...
      return read_uint16(in)
}

func cat(in byteDripper) {
      for {
         line, err := read_lineno(in)
         if err == nil {
            fmt.Printf("%d\n", line)
         }  else {
            break
         }
      }
}

func main() {
	infl, err := os.Open(os.Args[1])
	if err != nil {
		fmt.Printf("Can't open <%s>!: %s\n", os.Args[1], err.Error())
		return
	}
	defer infl.Close()

	br := bufio.NewReader(infl)
	input, err := decoder(br)
        if err != nil {
           return
        }

        cat(input)
}
