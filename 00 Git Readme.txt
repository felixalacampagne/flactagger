Git actions to create sub-modules for jaudiotagger and JustFLAC sources

Determine the github "pull request" urls:
   https://bitbucket.org/ijabz/jaudiotagger.git   (SSH didn't work)
   https://github.com/drogatkin/JustFLAC.git
   
Open a "Git Bash" window onto the project directory, ie. FLACtagger

Create the sub-modules in sub-directories with suitable names
   $ git submodule add https://bitbucket.org/ijabz/jaudiotagger.git jaudiotagger
   $ git submodule add https://github.com/drogatkin/JustFLAC.git justflac
   $ git commit -am 'Added jaudiotagger and JustFLAC as submodules'
   $ git push origin master


