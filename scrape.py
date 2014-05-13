#!/bin/bash

mkdir mudcat
curl "http://mudcat.org/@displaysong.cfm?SongID=[1-10365]" -o "mudcat/song_#1.html"
