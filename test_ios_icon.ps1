Add-Type -AssemblyName System.Drawing

function Generate-IOSIcon([int]$size, [string]$path) {
    # 24-bit RGB bitmap for iOS (no alpha transparency for App Store compliance)
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # 1. Solid Black Background (Fills entire square)
    $brushBlack = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 10, 10, 10))
    $g.FillRectangle($brushBlack, 0, 0, $size, $size)

    $scale = [float]$size / 1024.0

    # 2. Yellow Circle in Center
    $cDia = [float](760.0 * $scale)
    $cX = [float](($size - $cDia) / 2.0)
    $cY = [float](($size - $cDia) / 2.0)
    $brushYellow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 201, 34)) # #FFC922
    $g.FillEllipse($brushYellow, $cX, $cY, $cDia, $cDia)

    # 3. Vertical Black Line dividing the yellow circle
    $divWidth = [Math]::Max(1.5, [float](18.0 * $scale))
    $penDivider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $divWidth)
    $penDivider.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $penDivider.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $lineTop = [float]($cY + (76.0 * $scale))
    $lineBottom = [float]($cY + $cDia - (76.0 * $scale))
    $midX = [float]($size / 2.0)
    $g.DrawLine($penDivider, $midX, $lineTop, $midX, $lineBottom)

    # 4. Two Photo Cards (Left and Right)
    $cardW = [float](236.0 * $scale)
    $cardH = [float](204.0 * $scale)
    $cardR = [float](48.0 * $scale)
    $cardY = [float](($size - $cardH) / 2.0)
    $cPenWidth = [Math]::Max(1.2, [float](17.0 * $scale))
    $penCard = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $cPenWidth)

    $leftCardX = [float](208.0 * $scale)
    $rightCardX = [float]($size - $leftCardX - $cardW)

    function Draw-Card([float]$cCardX, [float]$cCardY, [float]$cW, [float]$cH, [bool]$isMirrored) {
        $pCard = New-Object System.Drawing.Drawing2D.GraphicsPath
        $pCard.AddArc($cCardX, $cCardY, $cardR, $cardR, [float]180, [float]90)
        $pCard.AddArc([float]($cCardX + $cW - $cardR), $cCardY, $cardR, $cardR, [float]270, [float]90)
        $pCard.AddArc([float]($cCardX + $cW - $cardR), [float]($cCardY + $cH - $cardR), $cardR, $cardR, [float]0, [float]90)
        $pCard.AddArc($cCardX, [float]($cCardY + $cH - $cardR), $cardR, $cardR, [float]90, [float]90)
        $pCard.CloseFigure()
        $g.DrawPath($penCard, $pCard)

        # Sun circle
        $sunDia = [float](44.0 * $scale)
        if (-not $isMirrored) {
            $sX = [float]($cCardX + (36.0 * $scale))
        } else {
            $sX = [float]($cCardX + $cW - (36.0 * $scale) - $sunDia)
        }
        $sY = [float]($cCardY + (32.0 * $scale))
        $g.FillEllipse($brushBlack, $sX, $sY, $sunDia, $sunDia)

        # Clip mountains inside the card
        $state = $g.Save()
        $g.SetClip($pCard)

        $bY = [float]($cCardY + $cH + (8.0 * $scale))

        if (-not $isMirrored) {
            # Left card:
            # 1. Small mountain (left)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cCardX + (8.0 * $scale)), $bY, [float]($cCardX + (80.0 * $scale)), [float]($cCardY + (112.0 * $scale)))
            $pM1.AddLine([float]($cCardX + (80.0 * $scale)), [float]($cCardY + (112.0 * $scale)), [float]($cCardX + (148.0 * $scale)), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Tall mountain (right)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cCardX + (96.0 * $scale)), $bY, [float]($cCardX + (168.0 * $scale)), [float]($cCardY + (68.0 * $scale)))
            $pM2.AddLine([float]($cCardX + (168.0 * $scale)), [float]($cCardY + (68.0 * $scale)), [float]($cCardX + $cW + (8.0 * $scale)), $bY)
            $pM2.CloseFigure()
            $g.FillPath($brushBlack, $pM2)
        } else {
            # Right card (mirrored):
            # 1. Tall mountain (left, facing center)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cCardX - (8.0 * $scale)), $bY, [float]($cCardX + $cW - (168.0 * $scale)), [float]($cCardY + (68.0 * $scale)))
            $pM1.AddLine([float]($cCardX + $cW - (168.0 * $scale)), [float]($cCardY + (68.0 * $scale)), [float]($cCardX + $cW - (96.0 * $scale)), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Small mountain (right, facing outer)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cCardX + $cW - (148.0 * $scale)), $bY, [float]($cCardX + $cW - (80.0 * $scale)), [float]($cCardY + (112.0 * $scale)))
            $pM2.AddLine([float]($cCardX + $cW - (80.0 * $scale)), [float]($cCardY + (112.0 * $scale)), [float]($cCardX + $cW - (8.0 * $scale)), $bY)
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
    Write-Host "Generated iOS Icon: $path ($size x $size)"
}

Generate-IOSIcon 1024 "d:\PhotosRemover\assets\remo_ios_1024.png"
