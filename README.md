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
- since we are going back to the future it now relies on JAXB which has become an external dependency
  
  To keep FLACtagger a standalone (aka executable) jar the dependent libraries are included in the FLACtagger
  jar file when it is built using the ant script 'mkjar.xml'. 
  
  The dependent libraries can be downloaded with maven and the 'pom.xml' and specifying 
  'dependency:copy-dependencies' as the goal. I haven't found a way to use maven to get the jar files
  for the FLAC and audiotagger libraries so the sources are included in the project as git sub-modules.
  This is not ideal as git sub-modules suck. Eventually I guess the project will need to be converted to
  maven where these libraries can exist as sub-projects in parallel to the FLAGtagger project... or something...
  but that is for another day.

When I set up the FLACtagger GitHub project I was asked about the license to be applied. I can't find this
information anywhere, and can't find licenses for the dependencies either.

History
06-Dec-2022 Renamed default branch to 'main' for consistency with other projects. On local repos GH suggests 
running the following commands:

git branch -m master main  
git fetch origin  
git branch -u origin/main main  
git remote set-head origin -a  

21-Feb-2022 Hadn't had a reason to change FLACtagger for a few years then I tried to use it on
my latest laptop and nothing. No errors and no response. Luckily this laptop had a development environment
installed so I clone the FLACtagger repo and started to debug. Of course it didn't work, missing libraries
etc. bla bla bla. Eventually got it to compile and there's nothing wrong when run from the debugger. Run
the newly generated jar from Explorer, same lack of behaviour. Waste more time figuring out how to connect
to running Java program for remote debugging... Lo and behold there is an exception in the constructor of
FLACtagger itself, which is not caught (why would there be an exception??) and since it is in a separate thread
it doesn't cause the app to exit, and for some reason the exception doesn't appear on the command line. The
exception is ClassNotFound - but no indication of which class. Waste more time adding some logging and eventually
figure out that the JAXB classes are causing the exception but only from the command line, not in the IDE, WTF!?!?

Eventually realise that the difference is Java8 in the IDE but the laptop has Java11 installed for normal use - so
what, why ClassNotFound??? More wasted time later - Google informs me that JAXB is no longer part of Java11. Yes,
the geniuses in the Java committee have decided to continuously improve us all the way back to the last millenium
when JAXB had to be downloaded separately from the standard JRE... WTAF? OK, can't beat them, there are too many
them and I just want my previously perfectly functioning program to work again. 
Where to get JAXB from now? 
In comes the dreaded Maven - all Googling points to Maven, no installer for JAXB or anything. 
I did manage to concoct some maven magic to download the JAXB libraries to a local dir.
This is when I discovered that the continuous improvers have been at it again and the
JAXB pakage names have been randomly changed from the standard 'javax'. So now the code generated
by XJC included with Java 8 is wrong. More time wasted getting the XJC ant script to take XJC from and to use the 
new JAXB libraries. Luckily the XJC with the new JAXB libraries seems to keep the same arguments and generates code 
with the new package names. Waste more time changing package names in my code and now it compiles and hey, ho, 
the resulting Jar appears to run with Java 11. It doesn't
run with Java 8 due to bytecode version difference or some such shirt. Waste more time getting Java 11 compiler to
output Java 8 compatible bytecode.

During the search for the new JAXB libs I realised that the external libraries I use were available via maven so I
had the genius idea of converting the project to use maven and removing the git submodules for the external stuff,
which just cause no end of trouble. Well, much wasted time later I managed to spew forth bucket loads of maven magic 
shirt and create a version of the project which uses maven to build. Needless to say there were many gotchas on the
way which caused hours and hours of wasted time, eg. how to get maven to generate the JAXB code, using the right
package names, and putting the code somewhere sensible, ie. in the source directories, not in the output directories,
how to get the timestamp version file generated, using a sensible time, ie. the current time shown by my 
system time, not some theoretical time which I couldn't give a shirt about, how to get the runnable jar file
generated (actually this was one of the better experiences!!). All seemingly simple things, but all requiring days
worth of spare time to get working. But for now it does work. I even managed to download onto a second system and
get it to build - although there was some difficulty getting the JAXB with the correct package names - don't really
know what happened to make it work properly, but it coincided with problem getting git to update the files so
they matched the repo content - why is it necessary to manually remove files that have been removed from the repo, 
surely having that done automatically is the point of using git. [Much later... tried to get project working on another 
system and ran headlong into the JAXB package name problem again. I'm pretty sure it was fixed by forcing use of Java 11
to run the maven build. Probably running maven with Java 8 results in the XJC which is part of Java 8 being picked
up instead of the external version.]

So one week later I was able to finally use my new (unchanged functionality) version of FLACtagger to tag some
FLACs on a machine with Java11. Thank you, Java committee grassholes for gratuitously breaking my code and wasting my time.
