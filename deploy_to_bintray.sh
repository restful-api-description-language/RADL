if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
  for package in "core" "gradle"; do
    for path in java/$package/build/libs/*; do
      echo curl -T $path -u$USER:key https://api.bintray.com/content/radl/RADL/$(basename $path)\;bt_package=radl-$package\;bt_version=$TRAVIS_TAG\;publish=1\;override=1
    done
  done
fi
