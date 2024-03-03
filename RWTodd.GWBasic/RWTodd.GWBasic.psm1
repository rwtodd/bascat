function ConvertFrom-GWBasic {
    [CmdletBinding(SupportsShouldProcess)]
    param (
        [Parameter(Mandatory, Position = 0, ValueFromPipeline)]
        [Alias('BasFile')]  # Backwards-compatible with the 1.0 series
        [string]$Path
    )
    process {
        if(-not (Test-Path $Path)) {
            Write-Error "Bad Path to GWBASIC file! <$Path>"
            return
        }
        foreach($lp in (Resolve-Path $Path)) {
            if( (Get-Item -LiteralPath $lp).Length -gt 256Kb ) {
                Write-Error "File $lp is too big to be a GWBasic file!"
                continue
            }
            try {
                if($PSCmdlet.ShouldProcess($lp,"Converting GWBasic file to plain text.")) {
                    [byte[]]$contents = Get-Content -AsByteStream -Raw -LiteralPath $lp
                    $bc = [RWTodd.GWBasic.BasCat]::new($contents)
                    $bc.GetAllLines()
                }
            } catch {
                Write-Error "Problem with processing <$lp>: $_"
            }
        }
    }
}
