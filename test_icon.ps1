Add-Type -AssemblyName System.Drawing

function Generate-TestIcon {
    $size = 512
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    # 1. Outer Squircle: Black with white outline
    $m = [float]14.0
    $w = [float]($size - (2.0 * $m))
    $r = [float]110.0

    $pathSquircle = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pathSquircle.AddArc($m, $m, $r, $r, [float]180, [float]90)
    $pathSquircle.AddArc([float]($m + $w - $r), $m, $r, $r, [float]270, [float]90)
    $pathSquircle.AddArc([float]($m + $w - $r), [float]($m + $w - $r), $r, $r, [float]0, [float]90)
    $pathSquircle.AddArc($m, [float]($m + $w - $r), $r, $r, [float]90, [float]90)
    $pathSquircle.CloseFigure()

    $brushBlack = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 10, 10, 10))
    $g.FillPath($brushBlack, $pathSquircle)

    $penWhiteBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(235, 255, 255, 255), [float]5.0)
    $g.DrawPath($penWhiteBorder, $pathSquircle)

    # 2. Yellow Circle in Center
    $cDia = [float]380.0
    $cX = [float](($size - $cDia) / 2.0)
    $cY = [float](($size - $cDia) / 2.0)
    $brushYellow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 201, 34)) # #FFC922
    $g.FillEllipse($brushYellow, $cX, $cY, $cDia, $cDia)

    # 3. Vertical Black Line dividing the yellow circle
    $penDivider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), [float]9.0)
    $penDivider.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $penDivider.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $lineTop = [float]($cY + 38.0)
    $lineBottom = [float]($cY + $cDia - 38.0)
    $midX = [float]($size / 2.0)
    $g.DrawLine($penDivider, $midX, $lineTop, $midX, $lineBottom)

    # 4. Two Photo Cards (Left and Right)
    $cardW = [float]118.0
    $cardH = [float]102.0
    $cardR = [float]24.0
    $cardY = [float](($size - $cardH) / 2.0)
    $penCard = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), [float]8.5)

    $leftCardX = [float]104.0
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
        $sunDia = [float]22.0
        if (-not $isMirrored) {
            $sX = [float]($cX + 18.0)
        } else {
            $sX = [float]($cX + $cW - 18.0 - $sunDia)
        }
        $sY = [float]($cY + 16.0)
        $g.FillEllipse($brushBlack, $sX, $sY, $sunDia, $sunDia)

        # Clip mountains inside the card
        $state = $g.Save()
        $g.SetClip($pCard)

        $bY = [float]($cY + $cH + 4.0) # Extend past bottom so baseline is clipped cleanly

        if (-not $isMirrored) {
            # Left card:
            # 1. Small mountain (left)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cX + 4.0), $bY, [float]($cX + 40.0), [float]($cY + 56.0))
            $pM1.AddLine([float]($cX + 40.0), [float]($cY + 56.0), [float]($cX + 74.0), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Tall mountain (right)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cX + 48.0), $bY, [float]($cX + 84.0), [float]($cY + 34.0))
            $pM2.AddLine([float]($cX + 84.0), [float]($cY + 34.0), [float]($cX + $cW + 4.0), $bY)
            $pM2.CloseFigure()
            $g.FillPath($brushBlack, $pM2)
        } else {
            # Right card (mirrored):
            # 1. Tall mountain (left, facing center)
            $pM1 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM1.AddLine([float]($cX - 4.0), $bY, [float]($cX + $cW - 84.0), [float]($cY + 34.0))
            $pM1.AddLine([float]($cX + $cW - 84.0), [float]($cY + 34.0), [float]($cX + $cW - 48.0), $bY)
            $pM1.CloseFigure()
            $g.FillPath($brushBlack, $pM1)

            # 2. Small mountain (right, facing outer)
            $pM2 = New-Object System.Drawing.Drawing2D.GraphicsPath
            $pM2.AddLine([float]($cX + $cW - 74.0), $bY, [float]($cX + $cW - 40.0), [float]($cY + 56.0))
            $pM2.AddLine([float]($cX + $cW - 40.0), [float]($cY + 56.0), [float]($cX + $cW - 4.0), $bY)
            $pM2.CloseFigure()
            $g.FillPath($brushBlack, $pM2)
        }

        $g.Restore($state)
    }

    Draw-Card $leftCardX $cardY $cardW $cardH $false
    Draw-Card $rightCardX $cardY $cardW $cardH $true

    $out = "d:\PhotosRemover\assets\test_icon_preview.png"
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Generated successfully to $out"
}

Generate-TestIcon
