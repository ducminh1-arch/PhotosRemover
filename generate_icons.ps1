Add-Type -AssemblyName System.Drawing

function Draw-CleanPixIcon([int]$size, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $scale = [float]$size / 512.0

    # 1. Base Squircle / Rounded Square
    # In standard icon design, 512x512 has ~88px corner radius (about 17-20%)
    $m = [float](10.0 * $scale)
    $w = [float]($size - (2.0 * $m))
    $r = [float](90.0 * $scale)

    $pathSquircle = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pathSquircle.AddArc($m, $m, $r, $r, 180, 90)
    $pathSquircle.AddArc($m + $w - $r, $m, $r, $r, 270, 90)
    $pathSquircle.AddArc($m + $w - $r, $m + $w - $r, $r, $r, 0, 90)
    $pathSquircle.AddArc($m, $m + $w - $r, $r, $r, 90, 90)
    $pathSquircle.CloseFigure()

    # Fill base: Dark Navy #0F172A
    $brushDark = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 15, 23, 42))
    $g.FillPath($brushDark, $pathSquircle)

    # 2. Main Yellow Card (Rounded Square) inside dark container
    $cm = [float](26.0 * $scale)
    $cw = [float]($size - (2.0 * $cm))
    $cr = [float](76.0 * $scale)
    $pathCard = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pathCard.AddArc($cm, $cm, $cr, $cr, 180, 90)
    $pathCard.AddArc($cm + $cw - $cr, $cm, $cr, $cr, 270, 90)
    $pathCard.AddArc($cm + $cw - $cr, $cm + $cw - $cr, $cr, $cr, 0, 90)
    $pathCard.AddArc($cm, $cm + $cw - $cr, $cr, $cr, 90, 90)
    $pathCard.CloseFigure()

    # Fill Yellow: #FFC800
    $brushYellow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 200, 0))
    $g.FillPath($brushYellow, $pathCard)

    # 3. Center Divider (splits into dual duplicate photos)
    $divW = [float](12.0 * $scale)
    $divX = [float](($size / 2.0) - ($divW / 2.0))
    $g.FillRectangle($brushDark, $divX, $cm, $divW, $cw)

    # 4. Photo Viewports (Left and Right)
    $pw = [float](185.0 * $scale)
    $ph = [float](275.0 * $scale)
    $py = [float](80.0 * $scale)
    $pr = [float](36.0 * $scale)

    $brushCream = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 251, 235))
    $brushSun   = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 239, 68, 68))
    $brushM1    = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 16, 185, 129))
    $brushM2    = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 5, 150, 105))
    $brushBrown = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 120, 53, 15))

    $drawPhoto = {
        param([float]$vx)

        $pPath = New-Object System.Drawing.Drawing2D.GraphicsPath
        $pPath.AddArc($vx, $py, $pr, $pr, 180, 90)
        $pPath.AddArc($vx + $pw - $pr, $py, $pr, $pr, 270, 90)
        $pPath.AddArc($vx + $pw - $pr, $py + $ph - $pr, $pr, $pr, 0, 90)
        $pPath.AddArc($vx, $py + $ph - $pr, $pr, $pr, 90, 90)
        $pPath.CloseFigure()
        $g.FillPath($brushCream, $pPath)

        # Sun
        $sunR = [float](28.0 * $scale)
        $sunX = [float]($vx + (34.0 * $scale))
        $sunY = [float]($py + (34.0 * $scale))
        $g.FillEllipse($brushSun, $sunX, $sunY, $sunR, $sunR)

        # Mountains
        $state = $g.Save()
        $g.SetClip($pPath)

        $m1 = [System.Drawing.PointF[]]@(
            (New-Object System.Drawing.PointF $vx, ($py + $ph)),
            (New-Object System.Drawing.PointF ($vx + (70.0 * $scale)), ($py + (150.0 * $scale))),
            (New-Object System.Drawing.PointF ($vx + (150.0 * $scale)), ($py + $ph))
        )
        $g.FillPolygon($brushM1, $m1)

        $m2 = [System.Drawing.PointF[]]@(
            (New-Object System.Drawing.PointF ($vx + (75.0 * $scale)), ($py + $ph)),
            (New-Object System.Drawing.PointF ($vx + (140.0 * $scale)), ($py + (180.0 * $scale))),
            (New-Object System.Drawing.PointF ($vx + $pw), ($py + $ph))
        )
        $g.FillPolygon($brushM2, $m2)

        $g.Restore($state)

        # Ribbon badge
        $rw = [float](140.0 * $scale)
        $rh = [float](36.0 * $scale)
        $rx = [float]($vx + (($pw - $rw) / 2.0))
        $ry = [float]($py + $ph + (22.0 * $scale))
        $rr = [float](12.0 * $scale)
        $rPath = New-Object System.Drawing.Drawing2D.GraphicsPath
        $rPath.AddArc($rx, $ry, $rr, $rr, 180, 90)
        $rPath.AddArc($rx + $rw - $rr, $ry, $rr, $rr, 270, 90)
        $rPath.AddArc($rx + $rw - $rr, $ry + $rh - $rr, $rr, $rr, 0, 90)
        $rPath.AddArc($rx, $ry + $rh - $rr, $rr, $rr, 90, 90)
        $rPath.CloseFigure()
        $g.FillPath($brushBrown, $rPath)
    }

    &$drawPhoto ([float](50.0 * $scale))
    &$drawPhoto ([float](278.0 * $scale))

    # 5. Clean Magic Sparkle (Top-Right)
    $brushGold = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 215, 0))
    $spX = [float]($size - (42.0 * $scale))
    $spY = [float](34.0 * $scale)
    $spSize = [float](22.0 * $scale)
    $spPoints = [System.Drawing.PointF[]]@(
        (New-Object System.Drawing.PointF $spX, ($spY - $spSize)),
        (New-Object System.Drawing.PointF ($spX + ($spSize * 0.3)), ($spY - ($spSize * 0.3))),
        (New-Object System.Drawing.PointF ($spX + $spSize), $spY),
        (New-Object System.Drawing.PointF ($spX + ($spSize * 0.3)), ($spY + ($spSize * 0.3))),
        (New-Object System.Drawing.PointF $spX, ($spY + $spSize)),
        (New-Object System.Drawing.PointF ($spX - ($spSize * 0.3)), ($spY + ($spSize * 0.3))),
        (New-Object System.Drawing.PointF ($spX - $spSize), $spY),
        (New-Object System.Drawing.PointF ($spX - ($spSize * 0.3)), ($spY - ($spSize * 0.3)))
    )
    $g.FillPolygon($brushGold, $spPoints)

    # Save
    $g.Dispose()
    $dir = [System.IO.Path]::GetDirectoryName($path)
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Success: $path"
}

$resDir = "d:/PhotosRemover/PhotosRemover-Android/app/src/main/res"

Draw-CleanPixIcon 48  "$resDir/mipmap-mdpi/ic_launcher.png"
Draw-CleanPixIcon 72  "$resDir/mipmap-hdpi/ic_launcher.png"
Draw-CleanPixIcon 96  "$resDir/mipmap-xhdpi/ic_launcher.png"
Draw-CleanPixIcon 144 "$resDir/mipmap-xxhdpi/ic_launcher.png"
Draw-CleanPixIcon 192 "$resDir/mipmap-xxxhdpi/ic_launcher.png"
Draw-CleanPixIcon 512 "d:/PhotosRemover/assets/icon_512.png"
