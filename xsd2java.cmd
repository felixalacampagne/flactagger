set JDK_HOME=C:\Development\JDKs\jdk8_64bit
set XJC=%JDK_HOME%/bin/xjc

 
%XJC% lyrics.xsd -b lyrics.xjb -d src
