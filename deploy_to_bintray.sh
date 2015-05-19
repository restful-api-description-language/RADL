if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
  for package in "core" "gradle"; do
    for path in java/$package/build/libs/*; do
      curl -T $path -u$USER:$BINTRAY_KEY https://api.bintray.com/content/radl/RADL/$(basename $path)\;bt_package=radl-$package\;bt_version=$(basename $path | awk -F- '{print $3}' | cut -d. -f1-3)\;publish=1\;override=1
    done
  done
fi
