
using System.IO;
using System.Management.Automation;

namespace RWTodd.GWBasic;

[Cmdlet(VerbsCommon.Get, "GWBasicContent", SupportsShouldProcess = true)]
public class GetGWBasicContentCommand : PSCmdlet
{
    [Parameter(Mandatory = true, Position = 0, ValueFromPipeline = true, ValueFromPipelineByPropertyName = true)]
    [Alias("BasFile")]
    public string[] Path { get; set; } = Array.Empty<string>();

    protected override void ProcessRecord()
    {
        foreach (var p in Path)
        {
            try
            {
                var resolvedPaths = SessionState.Path.GetResolvedProviderPathFromPSPath(p, out var provider);
                foreach (var lp in resolvedPaths)
                {
                    if (!File.Exists(lp))
                    {
                        var ex = new FileNotFoundException($"Bad Path to GWBASIC file! <{lp}>");
                        WriteError(new ErrorRecord(ex, "FileNotFound", ErrorCategory.ObjectNotFound, lp));
                        continue;
                    }

                    var fileInfo = new FileInfo(lp);
                    if (fileInfo.Length > 256 * 1024)
                    {
                        var ex = new InvalidDataException($"File {lp} is too big to be a GWBasic file!");
                        WriteError(new ErrorRecord(ex, "FileTooLarge", ErrorCategory.InvalidData, lp));
                        continue;
                    }

                    if (ShouldProcess(lp, "Converting GWBasic file to plain text."))
                    {
                        var contents = File.ReadAllBytes(lp);
                        var bc = new BasCat(contents);
                        var lines = bc.GetAllLines();

                        // Write each line out
                        foreach (var line in lines)
                        {
                            WriteObject(line);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                WriteError(new ErrorRecord(ex, "ProcessingError", ErrorCategory.NotSpecified, p));
            }
        }
    }
}

