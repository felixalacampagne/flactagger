FLACtagger Readme

I created FLACtagger because I could not find a way to easily export the lyrics from my
FLAC files and then re-import them having made changes. Editing the lyrics directly in
my chosen FLAC processing programs, ie. foobar2k and mp3tag, is a particularly painful experience
especially when large numbers of files are concerned, ie. more than 1.

'mp3tag' provides a mechanism to export tags using a customizable format but provides no way, AFAIK,
to re-import the tags. However mp3tag provided the inspiration for FLACtagger, which was originally
only going to provide the re-import support I needed, as it allowed me to create an XML format file
which could then be processed by my own application. Once I'd got the import working 
providing the export was relatively simple. On the way I discovered a way to generate digests of the
raw audio in FLACtagger which match the digests created by ffmpeg. I also discovered that most FLAC files
also contain the same raw audio digest embedded in them. This provides a handy way of checking that the audio
hasn't been changed by the various tagging operations applied to the files between creation, tagging and 
the final version, which for me just happens to be ALAC, and also ensuring that backups on optical
media haven't become corrupt.

FLACtagger relies on;
- the "jaudiotagger" library which can be found here: https://bitbucket.org/ijabz/jaudiotagger.git
- the "JustFLAC" library which can be found here: https://github.com/drogatkin/JustFLAC.git
- the "jNA" library found here: https://github.com/java-native-access/jna
  To keep FLACtagger a standalone (aka executable) jar the jna.jar and jna-platform.jar archives 
  should be unzipped to the lib/bin directory. The content of lib/bin is then added to the FLACtagger 
  archive. This does not appear to affect the dynamic creation/loading of the JNI DLL used by JNA.

When I set up the FLACtagger GitHub project I was asked about the license to be applied. I can't find this
information anywhere, and can't find licenses for the dependencies either.

BTW: This is a text file, not a message digest, so why does it need to have the ".md" extension?
Curse: GitHub lets me edit this file - and then it does't let me save the changes (aaaggghhh!!!!)

Git mess again:
local repo on dev machine
pushes to
remote repo on NAS
pushes to
gitbub

Changes are made on dev machine and saved in the local repo.
Local repo is backed up to the intermediate NAS repo when network is available.
When something is working it is published from NAS repo to Github.

This should be trivial but every single time I try to do it git forks it up!
The NAS repo was originally a bare repo, but none of the git tools I've tried work
with a bare repo making the damn thing completely useless as a backup because there
is no way to usefully visulaise what is in it. So I converted to a normal repo. Now
each time I push something from the local repo to the NAS repo it claims that working
NAS repo working copy has changes. When I tried using Github Desktop it constantly 
overwrites the changes in the NAS repo with the old files from the NAS repo working copy,
instead of push the NAS repo to GitHub (this is probably as the labels given to things in
GHD are completely incomprehensible). Anyway I kind of get the impression that before doing
anything with the NAS repo I need to force the working copy to contain the updates made in
the repo as this does not happen automatically (probably incredibly naive of me to expect 
git to do anything like this automatically, I suppose I'm too used to the luxury of
using Clearcase where changes to unmodified files are visible immediately).

Now the local 'git push' doesn't do anything!!! Surely it should push the currently
select branch? 
 
