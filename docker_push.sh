#!/usr/bin/env bash
set -e

get_script_dir () {
     SOURCE="${BASH_SOURCE[0]}"

     while [ -h "$SOURCE" ]; do
          DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
          SOURCE="$( readlink "$SOURCE" )"
          [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
     done
     cd -P "$( dirname "$SOURCE" )"
     pwd
}

export WORKSPACE=$(get_script_dir)

if pgrep -lf sshuttle > /dev/null ; then
  echo "sshuttle detected. Please close this program as it messes with networking and prevents builds inside Docker to work"
  exit 1
fi

if groups $USER | grep &>/dev/null '\bdocker\b'; then
  CAPTAIN="captain"
else
  CAPTAIN="sudo captain"
fi

$CAPTAIN push --branch-tags=false --commit-tags=true portal-backend
eval "echo $(cat $WORKSPACE/docker/runner/slack.json)" > $WORKSPACE/target/slack.json
curl -k -X POST --data-urlencode payload@$WORKSPACE/target/slack.json https://hbps1.chuv.ch/slack/dev-activity
rm -f $WORKSPACE/target/slack.json
