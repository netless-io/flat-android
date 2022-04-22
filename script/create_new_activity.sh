#!/bin/sh
# Only Build For Mac OS

if [ $# != 2 ] ; then
    echo "USAGE: $0 <PackageName> <ActivityName>"
    echo "As: $0 login PhoneLogin"
    exit 1;
fi

PACKAGE_NAME=$1
ACTIVITY_NAME=$2

cp -rf ./script/activity "app/src/main/java/io/agora/flat/ui/activity/$1"

cd "app/src/main/java/io/agora/flat/ui/activity/$1"

# Replace Content Hocker
sed -i "" "s/{PACKAGE_NAME}/$PACKAGE_NAME/g" `grep {PACKAGE_NAME} -rl`
sed -i "" "s/{ACTIVITY_NAME}/$ACTIVITY_NAME/g" `grep {ACTIVITY_NAME} -rl`

# Replace Template Names
for i in $(ls -chr)
do
    NEWNAME=$(echo $i | sed "s/Template/$ACTIVITY_NAME/g")
    mv $i $NEWNAME
done

exit 0
