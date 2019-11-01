rem Hopefully this should push the NAS repo to Github.
rem Can't use GitHub Desktop, the labels are too confusing and I
rem seem to always end up with the NAS repo containing the old
rem version of the files
pushd N:\Documents\git\repository\java.flactagger.git
"C:\Program Files\Git\bin\git" checkout --force
"C:\Program Files\Git\bin\git" push
popd