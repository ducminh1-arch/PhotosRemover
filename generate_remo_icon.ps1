Add-Type -AssemblyName System.Drawing

function Draw-RemoIcon([int]$size, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $scale = [float]$size / 512.0

    # 1. Outer Squircle: Black with subtle white outline
    $m = [float](14.0 * $scale)
    $w = [float]($size - (2.0 * $m))
    $r = [float](110.0 * $scale)

    $pathSquircle = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pathSquircle.AddArc($m, $m, $r, $r, [float]180, [float]90)
    $pathSquircle.AddArc([float]($m + $w - $r), $m, $r, $r, [float]270, [float]90)
    $pathSquircle.AddArc([float]($m + $w - $r), [float]($m + $w - $r), $r, $r, [float]0, [float]90)
    $pathSquircle.AddArc($m, [float]($m + $w - $r), $r, $r, [float]90, [float]90)
    $pathSquircle.CloseFigure()

    $brushBlack = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 10, 10, 10))
    $g.FillPath($brushBlack, $pathSquircle)

    $borderWidth = [Math]::Max(1.0, [float](5.0 * $scale))
    $penWhiteBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(235, 255, 255, 255), $borderWidth)
    $g.DrawPath($penWhiteBorder, $pathSquircle)

    # 2. Yellow Circle in Center
    $cDia = [float](380.0 * $scale)
    $cX = [float](($size - $cDia) / 2.0)
    $cY = [float](($size - $cDia) / 2.0)
    $brushYellow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 201, 34)) # #FFC922
    $g.FillEllipse($brushYellow, $cX, $cY, $cDia, $cDia)

    # 3. Vertical Black Line dividing the yellow circle
    $divWidth = [Math]::Max(1.5, [float](9.0 * $scale))
    $penDivider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $divWidth)
    $penDivider.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $penDivider.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $lineTop = [float]($cY + (38.0 * $scale))
    $lineBottom = [float]($cY + $cDia - (38.0 * $scale))
    $midX = [float]($size / 2.0)
    $g.DrawLine($penDivider, $midX, $lineTop, $midX, $lineBottom)

    # 4. Two Photo Cards (Left and Right)
    $cardW = [float](118.0 * $scale)
    $cardH = [float](102.0 * $scale)
    $cardR = [float](24.0 * $scale)
    $cardY = [float](($size - $cardH) / 2.0)
    $cPenWidth = [Math]::Max(1.2, [float](8.5 * $scale))
    $penCard = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $cPenWidth)

    $leftCardX = [float](104.0 * $scale)
    $rightCardX = [float]($size - $leftCardX - $cardW)

    function Draw-Card([float]$cX, [float]$cY, [float]$cW, [float]$cH, [bool]$isMirrored) {
        $pCard = New-Object System.Drawing.Drawing2D.GraphicsPath
        $pCard.AddArc($cX, $cY, $cardR, $cardR, [float]180, [float]90)
        $pCard.AddArc([float]($cX + $cW - $cardR), $cY, $cardR, $cardR, [float]270, [float]90)
        $pCard.AddArc([float]($cX + $cW - $cardR), [float]($cY + $cH - $cardR), $cardR, $cardR, [float]0, [float]90)
        $pCard.AddArc($cX, [float]($cY + $cH - $cardR), $cardR, $cardR, [float]90, [float]90)
        $pCard.CloseFigure()
        $g.DrawPath($penCard, $pCard)

        # Sun circle
        $sunDia = [float](22.0 * $scale)
        if (-not $isMirrored) {
            $sX = [float]($cX + (18.0 * $scale))
        } else {
            $sX = [float]($cX + $cW - (18.0 * $scale) - $sunDia)
        }
        $sY = [float]($cY + (16.0 * $scale))
        $g.FillEllipse($brushBlack, $sX, $sY, $sunDia, $sunDia)

        # Clip mountains inside the card
        $state = $g.Save()
        $g.SetClip($pCard)

        $bY = [float]($cY + $cH + (4.0 * $scale))

        if (-not $isMirrored) {
            # Left card:
            # 1. Small mountain (left)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cX + (4.0 * $scale)), $bY, [float]($cX + (40.0 * $scale)), [float]($cY + (56.0 * $scale)))
            $pM1.AddLine([float]($cX + (40.0 * $scale)), [float]($cY + (56.0 * $scale)), [float]($cX + (74.0 * $scale)), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Tall mountain (right)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cX + (48.0 * $scale)), $bY, [float]($cX + (84.0 * $scale)), [float]($cY + (34.0 * $scale)))
            $pM2.AddLine([float]($cX + (84.0 * $scale)), [float]($cY + (34.0 * $scale)), [float]($cX + $cW + (4.0 * $scale)), $bY)
            $pM2.CloseFigure()
            $g.FillPath($brushBlack, $pM2)
        } else {
            # Right card (mirrored):
            # 1. Tall mountain (left, facing center)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cX - (4.0 * $scale)), $bY, [float]($cX + $cW - (84.0 * $scale)), [float]($cY + (34.0 * $scale)))
            $pM1.AddLine([float]($cX + $cW - (84.0 * $scale)), [float]($cY + (34.0 * $scale)), [float]($cX + $cW - (48.0 * $scale)), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Small mountain (right, facing outer)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cX + $cW - (74.0 * $scale)), $bY, [float]($cX + $cW - (40.0 * $scale)), [float]($cY + (56.0 * $scale)))
            $pM2.AddLine([float]($cX + $cW - (40.0 * $scale)), [float]($cY + (56.0 * $scale)), [float]($cX + $cW - (4.0 * $scale)), $bY)
            $pM2.CloseFigure()
            $g.FillPath($brushBlack, $pM2)
        }

        $g.Restore($state)
    }

    Draw-Card $leftCardX $cardY $cardW $cardH $false
    Draw-Card $rightCardX $cardY $cardW $cardH $true

    $dir = [System.IO.Path]::GetDirectoryName($path)
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Success: $path"
}

$resDir = "d:/PhotosRemover/PhotosRemover-Android/app/src/main/res"

# Generate ic_launcher.png
Draw-RemoIcon 48  "$resDir/mipmap-mdpi/ic_launcher.png"
Draw-RemoIcon 72  "$resDir/mipmap-hdpi/ic_launcher.png"
Draw-RemoIcon 96  "$resDir/mipmap-xhdpi/ic_launcher.png"
Draw-RemoIcon 144 "$resDir/mipmap-xxhdpi/ic_launcher.png"
Draw-RemoIcon 192 "$resDir/mipmap-xxxhdpi/ic_launcher.png"

# Also copy to ic_launcher_round.png in case any OEM launcher checks roundIcon
Draw-RemoIcon 48  "$resDir/mipmap-mdpi/ic_launcher_round.png"
Draw-RemoIcon 72  "$resDir/mipmap-hdpi/ic_launcher_round.png"
Draw-RemoIcon 96  "$resDir/mipmap-xhdpi/ic_launcher_round.png"
Draw-RemoIcon 144 "$resDir/mipmap-xxhdpi/ic_launcher_round.png"
Draw-RemoIcon 192 "$resDir/mipmap-xxxhdpi/ic_launcher_round.png"

# Hi-res for Play Store & Assets
Draw-RemoIcon 512 "d:/PhotosRemover/assets/remo_icon_512.png"
