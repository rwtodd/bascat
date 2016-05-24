/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rwt.bascat;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.util.function.IntSupplier;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;


/**
 * This junk is only needed until takeWhile() lands in JDK9.
 */
class JunkUtils {
    public static <T> Stream<T> takeWhile(Stream<T> inStream, Predicate<T> condition) {
        return StreamSupport.stream(new TakeWhileSpliterator(inStream.spliterator(), condition), false);
    }
}
class TakeWhileSpliterator<T> implements Spliterator<T> {

    private final Spliterator<T> src;
    private final Predicate<T> cond;
    private boolean alive;
    
    TakeWhileSpliterator(Spliterator<T> source, Predicate<T> condition) {
        src = source;
        cond = condition;
        alive = true;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super T> cnsmr) {
        return alive && src.tryAdvance( val -> {
               if(cond.test(val)) {
                   cnsmr.accept(val);
               } else {
                   alive = false;
               }
        });            
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED;
    }
}
/* END OF WORKAROUND JUNK */

/**
 *
 * @author richa
 */
public class Cmd {
    
    private final BinaryReader in;

   private Token nextToken() {
       
      int nxt = in.readu8();
      Token answer = null;
      
      // a couple of ranges we can check first...
      if (nxt >= 0x20 && nxt <= 0x7E) { answer = Token.fromLiteral(nxt); }
      
      // 11 -> 1B are the digits 0 to 10
      else if (nxt >= 0x11 && nxt <= 0x1B) { answer = Token.fromNumber(nxt - 0x11, 10); }
      
      // 2-byte opcodes
      else if (nxt >= 0xFD && nxt <= 0xFF) {  answer = Token.fromOpcode( (nxt<<8) | in.readu8() ); }
 
      else switch(nxt)  {
      // unexpected EOF
      case -1:  throw new RuntimeException("Unexpected EOF!");
         
      // line-number
      case 0x0E:  answer = Token.fromNumber(in.readu16(), 10); break;
      
      // octal number
      case 0x0B:  answer = Token.fromNumber(in.read16(), 8); break;

      // hex number
      case 0x0C:  answer =  Token.fromNumber(in.read16(), 16); break;

      // 2-byte int
      case 0x1C: answer = Token.fromNumber(in.read16(), 10); break;

      // 1-byte int
      case 0x0F: answer =  Token.fromNumber(in.readu8(), 10); break;

      // 4-byte float 
      case 0x1D: answer =  Token.fromFloat(in.readf32()); break;

      // 8-byte float 
      case 0x1F: answer = Token.fromFloat(in.readf64()); break;

      // 1-byte opcodes
      default:  answer =  Token.fromOpcode(nxt); break;
      }
      return answer;
   }
   
   private List<Token> line() {
        List<Token> answer = new java.util.ArrayList<>();
        if(in.readu16() == 0) return answer;   // 0 pointer means EOF
        answer.add(Token.fromNumber(in.readu16(),10)); // line number
        answer.add(Token.fromLiteral("  "));           // a couple spaces
 
        final Stream<Token> toks = Stream.generate(this::nextToken);
        JunkUtils.takeWhile(toks, t -> t.opcode != 0).forEachOrdered(answer::add);
 
        return answer; 
   }
   
//     @annotation.tailrec
//  private def printLine(in: List[Token]):Unit = {
//    val (str,lst) = in match { 
//      case Nil => System.out.println(""); return
//      case Token(0x3A,_) :: Token(0xA1,out) :: rest                  => (out,rest)
//      case Token(0x3A,_) :: Token(0x8F,_) :: Token(0xD9,out) :: rest => (out,rest)
//      case Token(0xB1,out) :: Token(0xE9,_) :: rest                  => (out,rest)
//      case Token(_,out) :: rest                                      => (out,rest)
//    }
//
//    System.out.print(str)
//    printLine(lst)    
//  }

   
   private void printLine(List<Token> lst) { 
      int idx = 0;
      final int sz = lst.size();
      while(idx < sz) {
         Token cur = lst.get(idx);
         // there are a couple cases where we need to peek ahead and possibly output an alternate token...
         if(cur.opcode == 0x3A) {
            if((idx+1 < sz) && (lst.get(idx+1).opcode == 0xA1)) {
                 cur = lst.get(++idx);
            } else if((idx+2 < sz) &&
                      (lst.get(idx+1).opcode == 0x8F) &&
                      (lst.get(idx+2).opcode == 0xD9)
                     ) {
                idx += 2; 
                cur = lst.get(idx);
            }
         } else if( (cur.opcode == 0xB1)  &&
                    (idx+1 < sz) &&
                    (lst.get(idx+1).opcode == 0xE9)
                  ){ 
             idx++; /* skip the next one */ 
         }
                     
         System.out.print(cur.description);
         idx++;
      }
      
      System.out.println("");
   }
   
   private void cat() {
       final Stream<List<Token>> lines = Stream.generate(this::line);
       JunkUtils.takeWhile(lines, l -> !l.isEmpty()).forEachOrdered(this::printLine);
    }
    
    private Cmd(IntSupplier is) {
        in = new BinaryReader(is);
    }
    
    /**
     * Creates an IntSupplier from an InputStream. The main thing it
     * accomplishes is swallowing exceptions and returning EOF (-1) instead.
     * @param is the input stream to read from
     * @return an IntSupplier version of 'is'
     */
    private static IntSupplier noExceptionReader(InputStream is) {
        return () -> { 
            try {
                return is.read();
            } catch(IOException e) { return -1; } 
        };
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
     if(args.length < 1) {
         System.err.println("Usage: bascat filename");
         return;
     }
     
     try(InputStream fin = new java.io.FileInputStream(args[0])) {
       BufferedInputStream bin = new BufferedInputStream(fin);
       IntSupplier readFunc = Cmd.noExceptionReader(bin);
       switch(readFunc.getAsInt()) {
           case 0xFF:  /* do nothing */
                       break;
           case 0xFE:  readFunc = new Unprotector(readFunc);
                       break;
           default:    throw new Exception("Bad 1st Byte!");                        
       }
       new Cmd(readFunc).cat();
     } catch(Exception e) {
        System.err.println(e);
     }

    }
    
}

