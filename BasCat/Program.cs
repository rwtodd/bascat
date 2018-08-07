using System;
using System.IO;

namespace BasCat
{

    class Program
    {
        static void Main(string[] args)
        {
            if(args.Length != 1)
            {
                Console.Error.WriteLine("USAGE: bascat <gw-basic file>");
                Environment.Exit(-1);
            }

            new BasCat(File.ReadAllBytes(args[0])).PrintAllLines(Console.Out);            
        }
    }
}
