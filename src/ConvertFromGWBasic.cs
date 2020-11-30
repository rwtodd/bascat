using System;
using System.IO;
using System.Management.Automation;
using System.Management.Automation.Runspaces;

namespace RWTodd.GWBasic
{
    [Cmdlet(VerbsData.ConvertFrom,"GWBasic")]
    [OutputType(typeof(String))]
    public class ConvertFromGWBasic : PSCmdlet
    {
        [Parameter(
            Mandatory = true,
            Position = 0,
            ValueFromPipeline = true,
            ValueFromPipelineByPropertyName = true)]
        public FileInfo BasFile { get; set; }


        // This method gets called once for each cmdlet in the pipeline when the pipeline starts executing
        protected override void BeginProcessing()
        {
        }

        // This method will be called for each input received from the pipeline to this cmdlet; if no input is received, this method is not called
        protected override void ProcessRecord()
        {
            WriteVerbose($"Opening ${BasFile.Name}");
            
            if(BasFile.Length > 262144L) {
                WriteError(new ErrorRecord(new ArgumentException("File is too large!"), "TOOLARGE", ErrorCategory.LimitsExceeded, BasFile));
                return;
            }

            foreach(string line in new BasCat(File.ReadAllBytes(BasFile.FullName)).GetAllLines() ) {
                WriteObject(line,false);
            }

        }

        // This method will be called once at the end of pipeline execution; if no input is received, this method is not called
        protected override void EndProcessing()
        {
        }
    }
}
