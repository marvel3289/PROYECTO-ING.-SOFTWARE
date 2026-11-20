#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src -name '*.java' -print > .fuentes-java.txt
javac -encoding UTF-8 -d out @.fuentes-java.txt
rm -f .fuentes-java.txt
java -Dfile.encoding=UTF-8 -cp out biblioteca.web.WebServidor
