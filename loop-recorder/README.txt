LOOP RECORDER - turn Lively wallpapers into Firestick (Projectivy) video wallpapers

Two ways to make a loop. Both save a 1920x1080 MP4 plus a PNG still into
the Firestick Wallpapers folder (next to this Loop Recorder folder).

1) Record Lively Loop.bat  (double-click)
   Records 20 seconds of your main screen while Lively plays, then blends the
   end into the start so it loops with no jump.
   Before you start: hide desktop icons (right-click desktop > View > untick
   "Show desktop icons") and set the taskbar to auto-hide. Don't move the mouse
   during recording.

2) Make Loop From Video.bat  (drag a video file onto it)
   Best quality. If the Lively wallpaper is a video, use the original file
   instead of recording the screen. Also works for any clip you have.

First run: if ffmpeg (free) isn't installed, the tool offers to install it with
winget. Then close the window and run the tool again.

Settings: edit the .bat to change -Seconds 20 (record length). In
loop-tools.ps1, -Fade 1.5 is the crossfade that hides the loop seam. Use
longer for slow scenes and shorter for fast motion.

Note: many Lively and community wallpapers are made by other people. Keeping a
recording for your own TV is usually fine, but check the creator's licence
before sharing it.
