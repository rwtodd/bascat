package basic

import (
	_ "embed"
	"testing"
)

//go:embed UNP_TEST.BAS
var unprotectedProgram []byte

//go:embed PRT_TEST.BAS
var protectedProgram []byte

var correctAnswer = map[uint16]string{
	5: "G = 0.01", 10: "FOR X = 1 TO 10", 15: "G = G + 0.01", 20: "PRINT \"hi\"",
	25: "PRINT G", 30: "NEXT X",
}

func runTest(bs []byte, t *testing.T) {
	seq, err := DecodeLines(bs)
	if err != nil {
		t.Error(err)
		return
	}

	var lno uint16 = 5
	for l, txt := range seq {
		if l != lno {
			t.Errorf("Bad BASIC line num, expected <%d> got <%d>", lno, l)
			return
		}
		if txt != correctAnswer[lno] {
			t.Errorf("Bad BASIC text, expected <%s> got <%s>", correctAnswer[lno], txt)
			return
		}
		lno += 5
	}
}

func TestNormal(t *testing.T) {
	runTest(unprotectedProgram, t)
}

func TestProtected(t *testing.T) {
	runTest(protectedProgram, t)
}

func TestBadProg(t *testing.T) {
	var badBytes = []byte{14, 15, 16}
	_, err := DecodeLines(badBytes)
	if err == nil {
		t.Error("Decoded didn't notice a bad BASIC file!")
	}
}
