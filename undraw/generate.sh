#!/bin/bash
INFILE=for_icon.svg
TDIR=../app/src/main/res/
inkscape -z -f $INFILE -e ./presentation.png -h 500 -w 1024
inkscape -z -f $INFILE -e ./big_icon.png -h 512 -w 512
inkscape -z -f $INFILE -e $TDIR/mipmap-xxxhdpi/ic_launcher.png -h 192 -w 192
inkscape -z -f $INFILE -e $TDIR/mipmap-xxhdpi/ic_launcher.png -h 144 -w 144
inkscape -z -f $INFILE -e $TDIR/mipmap-xhdpi/ic_launcher.png -h 96 -w 96
inkscape -z -f $INFILE -e $TDIR/mipmap-hdpi/ic_launcher.png -h 72 -w 72
inkscape -z -f $INFILE -e $TDIR/mipmap-mdpi/ic_launcher.png -h 48 -w 48
inkscape -z -f $INFILE -e $TDIR/mipmap-ldpi/ic_launcher.png -h 36 -w 36
