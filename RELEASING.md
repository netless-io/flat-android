Releasing
=========

Cutting a Release
-----------------

1. Update `CHANGELOG.md`.
   > Fix: bugs, logic, etc.
   > New: new api, feature, etc.
   > Update: bump dependencies, uis, features, etc.
2. Set versions:

    ```shell
    export RELEASE_VERSION=X.Y.Z
    export OLD_VERSION_CODE=`grep versionCode app/build.gradle |awk '{ print $2; }'`
    export TARGET_VERSION_CODE=$[OLD_VERSION_CODE+1]
    ```
3. Update versions:
   ```shell
   sed -i "" \
   "s/versionName \".*\"/versionName \"$RELEASE_VERSION\"/g" \
   "app/build.gradle"
   sed -i "" \
   "s/versionCode \([0-9]*\)/versionCode $TARGET_VERSION_CODE/g" \
   "app/build.gradle"
   ```

4. Tag the release and push to GitHub.
   ```shell
   git commit -am "Prepare for release $RELEASE_VERSION"
   git tag -a $RELEASE_VERSION -m "Version $RELEASE_VERSION"
   git push -v origin refs/heads/main:refs/heads/main
   git push origin $RELEASE_VERSION
   ```

   1. Push release app to oss
      ```shell
      oss_push_prod_app Flat-x.x.x.apk
      ```

5. If force update
   ```shell
   oss_push_dev_check_version checkVersion.json
   # then test in dev
   oss_push_prod_check_version checkVersion.json
   # test in prod
   ```

6. Next version change
   ```shell
   export NEXT_VERSION=`echo $RELEASE_VERSION | awk -F '.' '{printf("%d.%d.%d-beta\n", $1, $2, $3 + 1)}'`
   sed -i "" \
   "s/versionName \".*\"/versionName \"$NEXT_VERSION\"/g" \
   "app/build.gradle"
   git commit -am "Prepare next development version."
   git push
   ```