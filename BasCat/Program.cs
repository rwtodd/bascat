using System;
using System.IO;

namespace BasCat
{

    class Program
    {
        static void Main(string[] args)
        {
            new BasCat(File.ReadAllBytes(args[0])).PrintAllLines(Console.Out);            
        }
    }
}
