#!/bin/sh

# Usage: ./gen-icons.sh master.png
# Generates Android launcher icons into mipmap-* folders

SRC="$1"

if [ -z "$SRC" ]; then
  echo "Usage: $0 master.png"
  exit 1
fi

mkdir -p mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi

convert "$SRC" -resize 48x48   mipmap-mdpi/ic_launcher.png
convert "$SRC" -resize 72x72   mipmap-hdpi/ic_launcher.png
convert "$SRC" -resize 96x96   mipmap-xhdpi/ic_launcher.png
convert "$SRC" -resize 144x144 mipmap-xxhdpi/ic_launcher.png
convert "$SRC" -resize 192x192 mipmap-xxxhdpi/ic_launcher.png

echo "Done. Place mipmap-* folders under app/src/main/res/"
