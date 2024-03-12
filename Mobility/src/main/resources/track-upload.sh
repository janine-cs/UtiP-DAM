#!/bin/bash

source /opt/utils/config.sh
set -e

track_file=$(ls -p /data/sites/*/realtime/tracks/ | grep "^site[0-9]*_[0-9]")

for track in $track_file
do
  echo $track
  site=${track%%"_"*}
  site=$(echo $site | sed 's/site//g')

  dataset=$(cat /opt/utils/site-mapping.csv | grep $site | cut -d, -f2)
  echo $dataset 

  url='https://'$server_name':'$https_port'/utip-dam/mobility/'$dataset

  echo $url
  path=/data/sites/site$site/realtime/tracks/$track

  curl -X POST "${url}" --form file=@"${path}"
done
