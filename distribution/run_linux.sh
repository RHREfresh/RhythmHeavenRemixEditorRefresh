#!/usr/bin/env bash
cd "$(dirname "$0")"

java -Xmx4G -jar bin/RHREfresh.jar "$@"

read -n1 -p "Press any key to continue..."

