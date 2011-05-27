OLDDIR=`pwd`
cd ../../out
find . | grep DroidSSH | xargs rm -r
cd $OLDDIR
