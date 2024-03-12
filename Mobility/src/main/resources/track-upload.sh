#!/bin/bash

source /opt/utils/config.sh
set -e

root=/opt/utils
zip_file=$(ls -p /data/sites/*/tracks/ | grep "^site[0-9]*_[0-9]")

for f in $zip_file
do
  site=${f%%"_"*}
  site=$(echo $site | sed 's/site//g')

  parent=/data/sites/site$site/tracks
  echo $parent/$f
  unzip $parent/$f
  dataset=$(cat /opt/utils/site-mapping.csv | grep $site | cut -d, -f2)
  echo $dataset

  url='https://localhost:8081/utip-dam/mobility/upload/1'

  echo $url

  track_file=$(ls $root/data/sites/site$site/tracks/ | xargs -n 1 basename)

  for t in $track_file
  do
    track_file_path=$root/data/sites/site$site/tracks/$t
    curl -X POST "${url}" --form file=@"${track_file_path}"
    rm $track_file_path
  done

done

