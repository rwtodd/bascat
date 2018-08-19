using System;
using System.IO;
using System.Threading.Tasks;

namespace BasCat
{

    internal static class Program
    {
        static async Task Main(string[] args)
        {
            if(args.Length != 1)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.BackgroundColor = ConsoleColor.Black;
                Console.Error.WriteLine("USAGE: bascat <gw-basic file>");
                Console.ResetColor();
                Environment.Exit(-1);
            }

            await new BasCat(File.ReadAllBytes(args[0])).PrintAllLinesAsync(Console.Out);
        }
    }
}
