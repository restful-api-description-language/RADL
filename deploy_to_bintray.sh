if [ "$TRAVIS_BRANCH" == "master" ] && [ ! -z "$TRAVIS_TAG" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
  for package in "core" "gradle"; do
    for path in java/$package/build/libs/*; do
      version=$(basename $path | awk -F- '{print $3}' | cut -d. -f1-3)
      curl -T $path -u$USER:$BINTRAY_KEY https://api.bintray.com/content/radl/RADL/radl/radl-$package/$version/$(basename $path)\;bt_package=radl-$package\;bt_version=$version\;publish=1\;override=1
    done
  done
fi
