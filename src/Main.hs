module Main where

import Control.Monad (when)
import System.IO (stdout)
import System.Exit (die)
import System.Environment (getArgs)
import Data.Word (Word8, Word16)
import Data.Int  (Int16)
import Data.Bits (xor, shiftL, (.|.), (.&.))
import Data.Monoid (mconcat, mempty)
import Data.String (fromString)
import Data.Ratio ( (%) )
import qualified Data.ByteString as B
import qualified Data.ByteString.Char8 as C
import qualified Data.ByteString.Builder as Bld
import qualified Data.Array.IArray as Arr
import Control.Monad.State (get,put,runState,State)

-- State-handling....
type Parser = State B.ByteString

take_bytes :: Int -> Parser [Word8]
take_bytes n = do
  src <- get
  let (prefix, suffix) = B.splitAt n src
  put suffix
  return $ if (B.length prefix == n) then B.unpack prefix else (take n $ repeat (fromIntegral 0))

read_byte :: Parser Word8
read_byte = (take_bytes 1) >>= (return . head)

read_sb16 :: Parser Int16 
read_sb16 = do
  two <- take_bytes 2
  let [a,b] = two 
  return $ ((fromIntegral b) `shiftL` 8) .|. (fromIntegral a)

read_ub16 :: Parser Word16
read_ub16 = read_sb16 >>= (return . fromIntegral)

skip :: Int -> Parser ()
skip n = get >>= put . (B.drop n)

read_f32 :: Parser Float 
read_f32 = do
  four <- take_bytes 4
  let [a,b,c,d] = map fromIntegral four
  return $ mbf32 a b c d 
    where mbf32 :: Integer -> Integer -> Integer -> Integer -> Float 
          mbf32 _ _ _ 0 = 0.0
          mbf32 a b c d = let sign  = if (c .&. 0x80) == 0 then 1 else -1
                              exp   = fromIntegral $ d - 129
                              pow2  = if exp < 0 then 1 % (1 `shiftL` (-exp)) else (1 `shiftL` exp) % 1
                              scand =  a .|. (b `shiftL` 8) .|. ((c .&. 0x7F) `shiftL` 16) 
                          in fromRational $ sign * (1 + (scand % 0x800000)) * pow2

read_f64 :: Parser Double
read_f64 = do
  eight <- take_bytes 8
  let [a,b,c,d,e,f,g,h] = map fromIntegral eight
  return $ mbf64 a b c d e f g h
    where mbf64 :: Integer -> Integer -> Integer -> Integer -> Integer -> Integer -> Integer -> Integer -> Double
          mbf64 _ _ _ _ _ _ _ 0 = 0.0
          mbf64 a b c d e f g h = let sign  = if (g .&. 0x80) == 0 then 1 else -1
                                      exp   = fromIntegral $ h - 129
                                      pow2  = if exp < 0 then 1 % (1 `shiftL` (-exp)) else (1 `shiftL` exp) % 1
                                      scand = a .|. (b `shiftL` 8) .|. (c `shiftL` 16) .|. (d `shiftL` 24) .|.
                                              (e `shiftL` 32) .|. (f `shiftL` 40) .|. ((g .&. 0x7F) `shiftL` 48)
                                  in fromRational $ sign * (1 + (scand % 0x80000000000000)) * pow2

collect :: Monoid a => Parser (Maybe a) -> Parser a
collect p = collect' mempty
  where collect' acc = p >>= maybe (return acc) (collect' . (mappend acc))

grab_char_range :: Parser B.ByteString
grab_char_range  = do
  src <- get
  let (chrs, rest) = B.span (\x -> (x >= 0x20 && x <= 0x7E && x /= 0x3A)) src
  put rest
  return chrs

-- some pre-made ByteStrings useful in `decode_token`
prefixA1 = B.singleton (fromIntegral 0xA1)
prefix8FD9 = B.cons (fromIntegral 0x8F) (B.singleton (fromIntegral 0xD9))
prefixE9 = B.singleton (fromIntegral 0xE9)

decode_token :: Int -> Parser (Maybe Bld.Builder)
decode_token 0 = return Nothing
decode_token t = do
  src    <- get
  parsed <- case t of
              0x3A | B.isPrefixOf prefixA1 src   -> (skip 1) >> return (tokens Arr.! 43) -- "ELSE"
                   | B.isPrefixOf prefix8FD9 src -> (skip 2) >> return (tokens Arr.! 99) -- "'"
                   | otherwise                   -> return (Bld.word8 0x3A)
              0xB1 | B.isPrefixOf prefixE9 src   -> (skip 1) >> return (tokens Arr.! 59) -- "WHILE"

              0x0B -> read_sb16 >>= return . Bld.int16Dec  -- OCTAL
              0x0C -> read_sb16 >>= return . Bld.int16HexFixed  -- HEX
              0x0E -> read_ub16 >>= return . Bld.word16Dec -- UNS SHORT 
              0x0F -> read_byte >>= return . Bld.word8Dec  -- UNS BYTE
              0x1C -> read_sb16 >>= return . Bld.int16Dec   -- SIGN SHORT
              0x1D -> read_f32  >>= return . Bld.floatDec  -- FLOAT 32
              0x1F -> read_f64  >>= return . Bld.doubleDec  -- FLOAT 64 

              x | (x >= 0x11 && x <= 0x1B) -> return (tokens Arr.! (x - 0x11))
                | (x >= 0x81 && x <= 0xF4) -> return (tokens Arr.! (x - 118))
                | (x >= 0xFD81 && x <= 0xFD8B) -> return (tokens Arr.! (x - 64770))
                | (x >= 0xFE81 && x <= 0xFEA8) -> return (tokens Arr.! (x - 65015))
                | (x >= 0xFF81 && x <= 0xFFA5) -> return (tokens Arr.! (x - 65231))
              otherwise -> return (fromString "<UNK!>")
  return $ Just parsed

read_token_code :: Parser Int
read_token_code = do
  b0    <- read_byte
  if (b0 < 0xfd) 
    then return $ fromIntegral b0 
    else read_byte >>= 
           (\b1 -> return $ ((fromIntegral b0) `shiftL` 8) .|. 
                            (fromIntegral b1))
 
next_token :: Parser (Maybe Bld.Builder)
next_token = do
  chrs <- grab_char_range
  if (B.null chrs) 
   then read_token_code >>= decode_token
   else return (Just $ Bld.byteString chrs)

eol, spacing :: Bld.Builder
spacing = fromString "  "
eol     = Bld.char8 '\n'

parse_line :: Parser (Maybe Bld.Builder)
parse_line = do
  ptr  <- read_ub16
  if (ptr == 0)
    then return Nothing
    else do
         lineno <- read_ub16
         toks   <- collect next_token
         return $ Just (mconcat [Bld.word16Dec lineno, spacing, toks, eol])

parse_lines = collect parse_line

-- File handling 
read_src fname = do
  fileSrc <- B.readFile fname
  case (B.head fileSrc) of
    0xFE      -> return (decrypt_source fileSrc)
    0xFF      -> return fileSrc
    otherwise -> die (fname ++ ": not a valid GWBAS/BASICA file!")

main :: IO ()
main = do
  args <- getArgs
  when (null args) $ die "Usage: bascat <gwbas file>!"
  src   <- read_src (head args)
  Bld.hPutBuilder  stdout $ fst (runState parse_lines (B.tail src))

-- Some support data for the decrypting Iterator
key11, key13 :: Arr.Array Int Word8
key11 = Arr.listArray (0,10) [0x1E,0x1D,0xC4,0x77,0x26,0x97,0xE0,0x74,0x59,0x88,0x7C]
key13 = Arr.listArray (0,12) [0xA9,0x84,0x8D,0xCD,0x75,0x83,0x43,0x63,0x24,0x83,0x19,0xF7,0x9A]

decrypt_source src = snd $ B.mapAccumL decr (-1) src
  where decr :: Int -> Word8 -> (Int, Word8)
        decr (-1) b  = (0, 0xFF)
        decr idx  b  = let idx11 = idx `mod` 11
                           idx13 = idx `mod` 13
                       in (idx+1, 
                           ((b - fromIntegral (11 - idx11)) `xor` (key11 Arr.! idx11) `xor` (key13 Arr.! idx13)) +
                           fromIntegral (13 - idx13))
                     

tokens :: Arr.Array Int Bld.Builder
tokens = Arr.listArray (0, 214) $ map fromString [ 
    --  0x11 - 0x1B 
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",

    --- *** BREAK *** ---
    --  0x81 - 0x90 
    "END", "FOR", "NEXT", "DATA", "INPUT", "DIM", "READ", "LET",
    "GOTO", "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM", "STOP",
    --  0x91 - 0xA0 
    "PRINT", "CLEAR", "LIST", "NEW", "ON", "WAIT", "DEF", "POKE",
    "CONT", "<0x9A!>", "<0x9B!>", "OUT", "LPRINT", "LLIST", "<0x9F!>", "WIDTH",
    --  0xA1 - 0xB0 
    "ELSE", "TRON", "TROFF", "SWAP", "ERASE", "EDIT", "ERROR", "RESUME",
    "DELETE", "AUTO", "RENUM", "DEFSTR", "DEFINT", "DEFSNG", "DEFDBL", "LINE",
    --  0xB1 - 0xC0 
    "WHILE", "WEND", "CALL", "<0xB4!>", "<0xB5!>", "<0xB6!>", "WRITE", "OPTION",
    "RANDOMIZE", "OPEN", "CLOSE", "LOAD", "MERGE", "SAVE", "COLOR", "CLS",
    --  0xC1 - 0xD0 
    "MOTOR", "BSAVE", "BLOAD", "SOUND", "BEEP", "PSET", "PRESET", "SCREEN",
    "KEY", "LOCATE", "<0xCB!>", "TO", "THEN", "TAB(", "STEP", "USR",
    --  0xD1 - 0xE0 
    "FN", "SPC(", "NOT", "ERL", "ERR", "STRING$", "USING", "INSTR",
    "'", "VARPTR", "CSRLIN", "POINT", "OFF", "INKEY$", "<0xDF!>", "<0xE0!>",
    --  0xE1 - 0xF0 
    "<0xE1!>", "<0xE2!>", "<0xE3!>", "<0xE4!>", "<0xE5!>", ">", "=", "<",
    "+", "-", "*", "/", "^", "AND", "OR", "XOR",
    --  0xF1 - 0xf4 
    "EQV", "IMP", "MOD", "\\",

    --- *** BREAK *** ---
    --  0xFD81 - 0xFD8B 
    "CVI", "CVS", "CVD", "MKI$", "MKS$", "MKD$", "<0xFD87!>", "<0xFD88!>",
    "<0xFD89!>", "<0xFD8A!>", "EXTERR",

    --- *** BREAK *** ---
    --  0xFE81 - 0xFE90 
    "FILES", "FIELD", "SYSTEM", "NAME", "LSET", "RSET", "KILL", "PUT",
    "GET", "RESET", "COMMON", "CHAIN", "DATE$", "TIME$", "PAINT", "COM",
    --  0xFE91 - 0xFEA0 
    "CIRCLE", "DRAW", "PLAY", "TIMER", "ERDEV", "IOCTL", "CHDIR", "MKDIR",
    "RMDIR", "SHELL", "ENVIRON", "VIEW", "WINDOW", "PMAP", "PALETTE", "LCOPY",
    --  0xFEA1 - 0xFEA8 
    "CALLS", "<0xFEA2!>", "<0xFEA3!>", "NOISE", "PCOPY", "TERM", "LOCK", "UNLOCK",

    --- *** BREAK *** ---
    --  0xFF81 - 0xFE90 
    "LEFT$", "RIGHT$", "MID$", "SGN", "INT", "ABS", "SQR", "RND",
    "SIN", "LOG", "EXP", "COS", "TAN", "ATN", "FRE", "INP",
    --  0xFF91 - 0xFEA0 
    "POS", "LEN", "STR$", "VAL", "ASC", "CHR$", "PEEK", "SPACE$",
    "OCT$", "HEX$", "LPOS", "CINT", "CSNG", "CDBL", "FIX", "PEN",
    --  0xFFA1 - 0xFFA5 
    "STICK", "STRIG", "EOF", "LOC", "LOF"
   ]

