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

GitHub

Created a GitHub account called "felixalacampagne" in honour of the soon-to-be-defunct cat hotel.
Created the GitHub repo "FLACtagger".
Created an SSH key via the Git Bash command line.
Added the GitHub repo as a remote "origin" to my bare repo using the URL provided by "SSH clone":

git remote add origin git@github.com:felixalacampagne/FLACtagger.git

Tried to init the GitHub repo. 

git push -u origin --force --all
git push origin --force --tags

It didn't fail, but it didn't do anything, probably because I has previously
added the github repo using the "https" URL. That uploaded files, but with a zillion prompts for
username and password.

So this is a change which will hopefully be push to GitHub without me needed to enter the username and password.

Yay! It seems to have worked.

git push origin master

Something uploaded with no password prompts!! Let's go for two in a row.

Now need to figure out how to get the change I made to JustFLAC available in GitHub. At the moment the
sub-modules appear as references but they don't go anywhere in GitHub when they are clicked.


Jeezus Wept this Git thing SUCKS!
I got my change to JustFLAC accepted into the master project, so no more need for the GitHub fork that I created.
This is an everyday occurrence, right? So replacing my "fork" with the master "fork" should be straight forward.
Fork NO! I tried changing the URL of the submodule via eclipse - no change.
In the end I had to delete the justFLAC source code, delete the JustFLAC entry(s) from .gitmodules and then
delete the JustFLAC directory in .git/modules - it is completely absurd! Now to see if the changes get pushed
to GitHub si it refers to the master project instead of my version.
