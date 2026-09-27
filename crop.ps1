Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\Admin\.gemini\antigravity-ide\brain\39c71f86-3f69-4723-9662-adea795f7e06\.user_uploaded\media_1790110813848.png"
$src = [System.Drawing.Image]::FromFile($srcPath)
Write-Host "Width: $($src.Width), Height: $($src.Height)"

# Crop the icon (it is located on the left)
$cropW = 200
$cropH = 200
$crop = New-Object System.Drawing.Bitmap($cropW, $cropH)
$g = [System.Drawing.Graphics]::FromImage($crop)
$g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, $cropW, $cropH)), (New-Object System.Drawing.Rectangle(20, 10, 180, 180)), [System.Drawing.GraphicsUnit]::Pixel)

$outDir = "d:\PhotosRemover\assets"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$crop.Save("$outDir\remo_ref_cropped.png", [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$crop.Dispose()
$src.Dispose()
Write-Host "Saved to $outDir\remo_ref_cropped.png"
