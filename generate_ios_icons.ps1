Add-Type -AssemblyName System.Drawing

$assetsDir = "d:\PhotosRemover\PhotosRemover-iOS\PhotosRemover\Assets.xcassets"
$appIconDir = "$assetsDir\AppIcon.appiconset"

if (-not (Test-Path $appIconDir)) {
    New-Item -ItemType Directory -Path $appIconDir -Force | Out-Null
}

# 1. Root Assets.xcassets/Contents.json
$rootContentsJson = @'
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
'@
Set-Content -Path "$assetsDir\Contents.json" -Value $rootContentsJson -Encoding UTF8

# 2. Function to draw pixel-perfect Remo AppIcon for iOS
function Render-IOSAppIcon([int]$w, [int]$h, [string]$outFile) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # 1. Solid Black Background (No transparency for App Store compliance)
    $brushBlack = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 10, 10, 10))
    $g.FillRectangle($brushBlack, 0, 0, $w, $h)

    $scale = [float]$w / 1024.0

    # 2. Yellow Circle in Center
    $cDia = [float](760.0 * $scale)
    $cX = [float](($w - $cDia) / 2.0)
    $cY = [float](($h - $cDia) / 2.0)
    $brushYellow = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 201, 34)) # #FFC922
    $g.FillEllipse($brushYellow, $cX, $cY, $cDia, $cDia)

    # 3. Vertical Black Line dividing the yellow circle
    $divWidth = [Math]::Max(1.0, [float](18.0 * $scale))
    $penDivider = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $divWidth)
    $penDivider.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $penDivider.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $lineTop = [float]($cY + (76.0 * $scale))
    $lineBottom = [float]($cY + $cDia - (76.0 * $scale))
    $midX = [float]($w / 2.0)
    $g.DrawLine($penDivider, $midX, $lineTop, $midX, $lineBottom)

    # 4. Two Photo Cards (Left and Right)
    $cardW = [float](236.0 * $scale)
    $cardH = [float](204.0 * $scale)
    $cardR = [float](48.0 * $scale)
    $cardY = [float](($h - $cardH) / 2.0)
    $cPenWidth = [Math]::Max(1.0, [float](17.0 * $scale))
    $penCard = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 10, 10, 10), $cPenWidth)

    $leftCardX = [float](208.0 * $scale)
    $rightCardX = [float]($w - $leftCardX - $cardW)

    function Draw-Card([float]$cCardX, [float]$cCardY, [float]$cW, [float]$cH, [bool]$isMirrored) {
        $pCard = New-Object System.Drawing.Drawing2D.GraphicsPath
        $pCard.AddArc($cCardX, $cCardY, $cardR, $cardR, [float]180, [float]90)
        $pCard.AddArc([float]($cCardX + $cW - $cardR), $cCardY, $cardR, $cardR, [float]270, [float]90)
        $pCard.AddArc([float]($cCardX + $cW - $cardR), [float]($cCardY + $cH - $cardR), $cardR, $cardR, [float]0, [float]90)
        $pCard.AddArc($cCardX, [float]($cCardY + $cH - $cardR), $cardR, $cardR, [float]90, [float]90)
        $pCard.CloseFigure()
        $g.DrawPath($penCard, $pCard)

        # Sun circle
        $sunDia = [Math]::Max(2.0, [float](44.0 * $scale))
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

    $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "Generated iOS icon: $outFile ($w x $h)"
}

# 3. Generate all standard iOS icon resolutions
$iconConfigs = @(
    # iPhone
    @{ Name = "AppIcon-20x20@2x.png";      W = 40;   H = 40 },
    @{ Name = "AppIcon-20x20@3x.png";      W = 60;   H = 60 },
    @{ Name = "AppIcon-29x29@2x.png";      W = 58;   H = 58 },
    @{ Name = "AppIcon-29x29@3x.png";      W = 87;   H = 87 },
    @{ Name = "AppIcon-40x40@2x.png";      W = 80;   H = 80 },
    @{ Name = "AppIcon-40x40@3x.png";      W = 120;  H = 120 },
    @{ Name = "AppIcon-60x60@2x.png";      W = 120;  H = 120 },
    @{ Name = "AppIcon-60x60@3x.png";      W = 180;  H = 180 },

    # iPad
    @{ Name = "AppIcon-20x20@1x.png";      W = 20;   H = 20 },
    @{ Name = "AppIcon-20x20@2x-ipad.png"; W = 40;   H = 40 },
    @{ Name = "AppIcon-29x29@1x.png";      W = 29;   H = 29 },
    @{ Name = "AppIcon-29x29@2x-ipad.png"; W = 58;   H = 58 },
    @{ Name = "AppIcon-40x40@1x.png";      W = 40;   H = 40 },
    @{ Name = "AppIcon-40x40@2x-ipad.png"; W = 80;   H = 80 },
    @{ Name = "AppIcon-76x76@1x.png";      W = 76;   H = 76 },
    @{ Name = "AppIcon-76x76@2x.png";      W = 152;  H = 152 },
    @{ Name = "AppIcon-83.5x83.5@2x.png";  W = 167;  H = 167 },

    # Universal Single Size & App Store Marketing
    @{ Name = "AppIcon-1024.png";          W = 1024; H = 1024 }
)

foreach ($cfg in $iconConfigs) {
    Render-IOSAppIcon $cfg.W $cfg.H "$appIconDir\$($cfg.Name)"
}

# 4. AppIcon.appiconset/Contents.json
$appIconContentsJson = @'
{
  "images" : [
    {
      "filename" : "AppIcon-20x20@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "20x20"
    },
    {
      "filename" : "AppIcon-20x20@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "20x20"
    },
    {
      "filename" : "AppIcon-29x29@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "29x29"
    },
    {
      "filename" : "AppIcon-29x29@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "29x29"
    },
    {
      "filename" : "AppIcon-40x40@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "40x40"
    },
    {
      "filename" : "AppIcon-40x40@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "40x40"
    },
    {
      "filename" : "AppIcon-60x60@2x.png",
      "idiom" : "iphone",
      "scale" : "2x",
      "size" : "60x60"
    },
    {
      "filename" : "AppIcon-60x60@3x.png",
      "idiom" : "iphone",
      "scale" : "3x",
      "size" : "60x60"
    },
    {
      "filename" : "AppIcon-20x20@1x.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "20x20"
    },
    {
      "filename" : "AppIcon-20x20@2x-ipad.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "20x20"
    },
    {
      "filename" : "AppIcon-29x29@1x.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "29x29"
    },
    {
      "filename" : "AppIcon-29x29@2x-ipad.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "29x29"
    },
    {
      "filename" : "AppIcon-40x40@1x.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "40x40"
    },
    {
      "filename" : "AppIcon-40x40@2x-ipad.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "40x40"
    },
    {
      "filename" : "AppIcon-76x76@1x.png",
      "idiom" : "ipad",
      "scale" : "1x",
      "size" : "76x76"
    },
    {
      "filename" : "AppIcon-76x76@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "76x76"
    },
    {
      "filename" : "AppIcon-83.5x83.5@2x.png",
      "idiom" : "ipad",
      "scale" : "2x",
      "size" : "83.5x83.5"
    },
    {
      "filename" : "AppIcon-1024.png",
      "idiom" : "ios-marketing",
      "scale" : "1x",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
'@
Set-Content -Path "$appIconDir\Contents.json" -Value $appIconContentsJson -Encoding UTF8

# 5. AccentColor.colorset
$accentDir = "$assetsDir\AccentColor.colorset"
if (-not (Test-Path $accentDir)) {
    New-Item -ItemType Directory -Path $accentDir -Force | Out-Null
}
$accentJson = @'
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : {
          "alpha" : "1.000",
          "blue" : "0.000",
          "green" : "0.784",
          "red" : "1.000"
        }
      },
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
'@
Set-Content -Path "$accentDir\Contents.json" -Value $accentJson -Encoding UTF8

Write-Host "Successfully generated iOS Assets.xcassets and AppIcon.appiconset!"
