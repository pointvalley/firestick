# Loop Recorder - records your screen (e.g. a Lively wallpaper) or takes a video file,
# and turns it into a seamless 1080p MP4 loop for Projectivy on the Firestick.
param(
  [ValidateSet('record','file')][string]$Mode = 'record',
  [string]$InputPath = '',
  [int]$Seconds = 20,      # how long to record
  [double]$Fade = 1.5,     # crossfade length that hides the loop seam
  [int]$Fps = 30
)
$ErrorActionPreference = 'Stop'

# saves next to this tool: ...\Firestick\Firestick Wallpapers
$outDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'Firestick Wallpapers'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

# --- make sure ffmpeg is installed ---
if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
  Write-Host "ffmpeg isn't installed yet. It's free and needed for recording and looping." -ForegroundColor Yellow
  $ans = Read-Host "Install it now with winget? (Y/N)"
  if ($ans -match '^[Yy]') {
    winget install --id Gyan.FFmpeg -e --accept-source-agreements --accept-package-agreements
    Write-Host "`nInstalled. Close this window and run the tool again so Windows picks up ffmpeg." -ForegroundColor Green
  }
  exit 1
}

function Make-Loop([string]$src, [string]$dest) {
  $L = [double](& ffprobe -v error -show_entries format=duration -of csv=p=0 "$src")
  if ($L -lt (2 * $Fade + 1)) { throw "Clip is only $L seconds - it needs to be at least $(2*$Fade+1) seconds." }
  $C = $Fade.ToString([Globalization.CultureInfo]::InvariantCulture)
  $E = ($L - $Fade).ToString([Globalization.CultureInfo]::InvariantCulture)
  $Ls = $L.ToString([Globalization.CultureInfo]::InvariantCulture)
  # middle of the clip, then the end crossfaded into the start -> plays back with no visible jump
  $fc = "[0:v]fps=$Fps,settb=AVTB,setsar=1,split=3[a][b][c];" +
        "[a]trim=start=${C}:end=$E,setpts=PTS-STARTPTS[mid];" +
        "[b]trim=start=${E}:end=$Ls,setpts=PTS-STARTPTS[tail];" +
        "[c]trim=start=0:end=$C,setpts=PTS-STARTPTS[head];" +
        "[tail][head]xfade=transition=fade:duration=${C}:offset=0[blend];" +
        "[mid][blend]concat=n=2:v=1:a=0,scale=1920:1080:force_original_aspect_ratio=increase:flags=lanczos,crop=1920:1080,format=yuv420p[out]"
  Write-Host "Making the seamless 1080p loop..." -ForegroundColor Cyan
  & ffmpeg -hide_banner -loglevel error -stats -y -i "$src" -filter_complex $fc -map "[out]" -an `
      -c:v libx264 -preset slow -crf 20 -movflags +faststart "$dest"
  if ($LASTEXITCODE -ne 0) { throw "ffmpeg couldn't make the loop." }
  & ffmpeg -hide_banner -loglevel error -y -i "$dest" -frames:v 1 ($dest -replace '\.mp4$', '.png')
}

if ($Mode -eq 'file') {
  if (-not (Test-Path $InputPath)) { throw "Drag a video file onto 'Make Loop From Video.bat'." }
  $name = [IO.Path]::GetFileNameWithoutExtension($InputPath)
  $dest = Join-Path $outDir "$name loop.mp4"
  Make-Loop $InputPath $dest
}
else {
  # physical pixel size of the main monitor (handles Windows display scaling)
  Add-Type -AssemblyName System.Windows.Forms
  Add-Type -Namespace W -Name Dpi -MemberDefinition '[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();'
  [W.Dpi]::SetProcessDPIAware() | Out-Null
  $b = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds

  Write-Host "Before recording:" -ForegroundColor Yellow
  Write-Host "  - Hide desktop icons: right-click desktop > View > untick 'Show desktop icons'"
  Write-Host "  - Auto-hide the taskbar: Settings > Personalization > Taskbar > Taskbar behaviours"
  Write-Host "  - Make sure Lively is playing the wallpaper you want"
  Read-Host "`nPress Enter to start (windows will minimize, recording $Seconds seconds of your main screen)"
  (New-Object -ComObject Shell.Application).MinimizeAll()
  foreach ($i in 3..1) { Start-Sleep 1 }

  $raw = Join-Path $env:TEMP "lively-capture-$stamp.mp4"
  & ffmpeg -hide_banner -loglevel error -stats -y -f gdigrab -framerate $Fps -draw_mouse 0 `
      -offset_x $b.X -offset_y $b.Y -video_size "$($b.Width)x$($b.Height)" -i desktop `
      -t $Seconds -c:v libx264 -preset ultrafast -crf 12 -pix_fmt yuv420p "$raw"
  (New-Object -ComObject Shell.Application).UndoMinimizeALL()
  if ($LASTEXITCODE -ne 0) { throw "Screen recording failed." }

  $dest = Join-Path $outDir "Lively Loop $stamp.mp4"
  Make-Loop $raw $dest
  Remove-Item $raw -ErrorAction SilentlyContinue
}

Write-Host "`nDone! Saved to: $dest" -ForegroundColor Green
Write-Host "(A still PNG of the first frame was saved next to it.)"
Start-Process explorer.exe "/select,`"$dest`""
