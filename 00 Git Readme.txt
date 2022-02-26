GitHub actions

Update GitHub from NAS

Changes have been made on local repos and pushed to the NAS repo and now the changes should be uploaded to GitHub

- Open a 'Git Bash' window in the NAS repo directory
- Check that the SSH keys are installed: ls ~/.ssh
  If not copy them as described below
- Ensure the NAS working directory is in sync with the repo. A 'git status' should return an up to date message.
  If it doesn't then prepare for a world of hassle since git is obviously not intended as a source code management
  system for use by normal people with straight forward thoughts like 'make it like the repo is'. The following 
  seem to work:
  $ git reset --hard
  $ git clean -fxd
  
  Note: all untracked/ignored items should be removed by this: do not do this in an active development directory
  as it will probably remove settings/config files, build directories etc.. For the NAS repo, which is not used
  for development, this is OK.
- check github is still configured as a remote:
  $ git remote -v
    github  git@github.com:felixalacampagne/FLACtagger.git (fetch)
    github  git@github.com:felixalacampagne/FLACtagger.git (push)
  
  if it needs to be reconfigured then:
  $ git remote set-url github  git@github.com:felixalacampagne/FLACtagger.git
- it should now (finally) be possible to push the changes to github:
  $ git push
  fatal: No configured push destination.
  
  WTF was the previous 'remote' command for!!! Just because we've told git there is a remote repo doesn't mean that there aren't yet more
  incantations to utter before it understands what should be blindly obvious from what has already been entered! So use
  the commands the git push tells us to:
  $ git remote add github git@github.com:felixalacampagne/FLACtagger.git
  fatal: remote github already exists.
  
  Now they are just have a forking laugh.
  
  Try removing the github remote and adding it back using the 'add' command. 'git push' still fails,..
  $ git push github
   fatal: The current branch ConvertToMaven has no upstream branch.
   To push the current branch and set the remote as upstream, use
    git push --set-upstream github ConvertToMaven
  
  So do the suggested action - does this have to be done for every new branch! WTF!
  At least this suggestion turns out to be more helpful than the others and finally GitHub appears to have been
  updated with changes in the NAS repo. Unforkingbelievable. 
- For subsequent pushes of the same branch, at least for today, 'git push' appears to behave as expected. At least it
  did when I tried to push the updates to this document made directly on the NAS working directory.


  this is what


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
   Keys, called 'id_rsa*' are in the repos top level directory. To use them they must be copied to
   whereever the idiot unix programs think is the home directory. Easiest way to figure that out is
   to open a 'Git Bash' command line window and do something like
   
   mkdir -p ~/.ssh
   cp id_rsa* ~/.ssh 
   
   Check it work with 
   
   ssh -T git@github.com

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
Fork NO! I tried changing the URL of the submodule via eclipse - no change, then wasted an hour or so Googling and
forking around. In the end I had to delete the local JustFLAC directory, delete the JustFLAC entry(s) 
from .gitmodules and then when eclipse still wouldn't let me add the JustFLAC submodule becuase it already exists and
is not empty (where does it exist, not in the real world, only in the fantasy world of Git!). Eventually I 
deleted a JustFLAC directory in .git/modules and could add a JustFLAC submodule via eclipse, again.
This is all completely absurd! How the hell did this system become the "new standard" - I guess the same way the
110 film format almost killed 35mm film, VHS killed Betamax, mp3 killed CD - the "eat shirt, 10 gazillion flys can't
be wrong" principle... and it's free.
Now to see if the changes get pushed
to GitHub si it refers to the master project instead of my version.
