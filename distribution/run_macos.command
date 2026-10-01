#!/bin/zsh
cd "$(dirname "$0")"

java -Xmx4G -XstartOnFirstThread -jar bin/RHREfresh.jar

echo 'Press any key to continue...'; read -k1 -s
