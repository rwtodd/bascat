using System;
using System.IO;

namespace BasCat
{

    internal static class Program
    {
        static void Main(string[] args)
        {
            if(args.Length != 1)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.BackgroundColor = ConsoleColor.Black;
                Console.Error.WriteLine("USAGE: bascat <gw-basic file>");
                Console.ResetColor();
                Environment.Exit(-1);
            }

            new BasCat(File.ReadAllBytes(args[0])).PrintAllLines(Console.Out);
        }
    }
}
