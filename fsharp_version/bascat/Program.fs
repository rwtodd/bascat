(* BASCAT -- print out a tokenized GW-BASIC progrem.
 * Copyright 2016 Richard Todd.  License is GPL ... see LICENSE file
 * in the repo
 *)

(* ************************************************************** *)
(* Prepare for a time when we implement a decrypter
 * for protected file (like the Go and Scala implementations do) 
 *)
let get_decrypter (fl : System.IO.BinaryReader) =
    match fl.ReadByte() with
    | 0xFFuy      -> id
    | 0xFEuy      -> failwith "Decryption not supported!"
    | _           -> failwith "Bad First Byte!"

(* ************************************************************** *)
(* Multibyte binary reading functions *)
let read_16 rdr =
  let b1 = rdr()
  let b2 = sbyte(rdr())
  int(b2) <<< 8 ||| int(b1)

let read_u16 rdr = (read_16 rdr) &&& 0xFFFF

let read_f32 rdr =
  let bs:byte[] = Array.init 4 (fun _ -> rdr())
  (* convert MBF to IEEE *)  
  if bs.[3] = 0uy
  then 0.0f
  else
     let sign = int (bs.[2]) &&& 0x80
     let exp  = (int(bs.[3]) - 2) &&& 0xFF
     bs.[3] <- byte( sign ||| (exp >>> 1) )
     bs.[2] <- byte((exp <<< 7) ||| (int(bs.[2]) &&& 0x7F))
     if not System.BitConverter.IsLittleEndian then System.Array.Reverse(bs)
     System.BitConverter.ToSingle(bs,0)

let read_f64 rdr = 
  let bs:byte[] = Array.init 8 (fun _ -> rdr())
  (* convert MBF to IEEE *)
  if bs.[7] = 0uy 
  then 0.0
  else
     let iees:byte[] = Array.zeroCreate 8 
     let sign = int(bs.[6]) &&& 0x80
     let exp  = (int(bs.[3]) - 128 - 1 + 1023) &&& 0xFFFF
     iees.[7] <- byte(sign ||| (exp>>>4))
     iees.[6] <- byte(exp<<<4)
     for idx in 6..-1..1 do 
        bs.[idx] <- byte(  (bs.[idx] <<< 1) ||| (bs.[idx-1] >>> 7) )
        iees.[idx] <- byte( iees.[idx] ||| (bs.[idx] >>> 4) )
        iees.[idx-1] <- byte(  iees.[idx-1] ||| (bs.[idx] <<< 4) )
     bs.[0] <- bs.[0] <<< 1
     iees.[0] <- iees.[0] ||| (bs.[0] >>> 4)
     if not System.BitConverter.IsLittleEndian then System.Array.Reverse(iees)
     System.BitConverter.ToDouble(iees,0)

(* ************************************************************** *)
(* Helpers for the different token types we'll create *)     
let numToken (n:int)      = (-1, n.ToString())
let litToken (str:string) = (int str.[0], str)
let charToken (chr:int)  = (chr, (char chr).ToString())
let opToken (n:int) = (n, match opcodes.ops.TryFind(n) with
                                   | Some(str) -> str
                                   | None      -> "OP" + n.ToString()
                      )
let f32Token (n:float32) = (-1,n.ToString())
let f64Token (n:float)   = (-1,n.ToString())

(* ************************************************************** *)
(* Decodes the next token in the stream *)     
let next_token rdr = 
   match int( rdr() ) with 
     | 0 -> (0,"")
     | x when (x >= 0x20 && x <= 0x7E) -> charToken x          (* ASCII *)
     | x when (x >= 0x11 && x <= 0x1B) -> numToken (x - 0x11)  (* small num *)
     | 0x0E -> numToken (read_u16 rdr) (* line number *)
     | 0x0B -> numToken (read_16 rdr)  (* fixme -- octal format *)
     | 0x0C -> numToken (read_16 rdr)  (* fixme-- hex format *)
     | 0x1C -> numToken (read_16 rdr)  (* 2-byte int *)
     | 0x0F -> numToken (int( rdr() )) (* 1-byte int *)
     | 0x1D -> f32Token (read_f32 rdr) (* 4-byte float *)
     | 0x1F -> f64Token (read_f64 rdr) (* 8-byte float *)
     | x when (x >= 0xFD && x <= 0xFF) -> opToken ((x<<<8) ||| int(rdr()))
     | x -> opToken (int x) 

(* ************************************************************** *)
(* Reads a line of tokens, including the line number. If the 
 * pointer to the next line is 0, we are done with the file.
 *)     
let read_line rdr =
   if (read_u16 rdr) = 0 
        then []
        else let lineno = numToken (read_u16 rdr)
             let all_toks = Seq.initInfinite (fun _ -> next_token rdr) 
                            |> Seq.takeWhile (fun (t,_) -> t <> 0)  
                            |> Seq.toList
             lineno :: litToken "  " ::  all_toks

(* ************************************************************** *)
(* Certain combinations of tokens should be simplified for printout.
 * For example, 0xB1,0xE9 is literally "WHILE+", but should be printed
 * as "WHILE". 
 *)
let rec filter_line ln =  
  match ln with
   | []                                       -> None   
   | (0x3A,_) :: (0xA1,_) :: rest             -> Some(("ELSE",rest))
   | (0x3A,_) :: (0x8F,_) :: (0xD9,_) :: rest -> Some(("'",rest))
   | (0xB1,_) :: (0xE9,_) :: rest             -> Some(("WHILE",rest))
   | (_,s) :: rest                            -> Some((s,rest))

let write_line ln = for str in ln do printf "%s" str
                    printfn ""

[<EntryPoint>]
let main argv = 
    use fs = System.IO.File.OpenRead(argv.[0])
    use br = new System.IO.BinaryReader(fs)
    let rdr = br.ReadByte >> (get_decrypter br)
    let lines = Seq.initInfinite (fun _ -> read_line rdr) |> Seq.takeWhile (fun x -> not x.IsEmpty)
    for ln in lines do (List.unfold filter_line ln) |> write_line
    0 // return an integer exit code
