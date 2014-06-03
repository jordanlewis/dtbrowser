#!/usr/bin/env python

import sys, os, json, re
from bs4 import BeautifulSoup

# used to search for filename line
filename_regex = re.compile("^filename", re.I)
paren_regex = re.compile("[()]")
child_regex = re.compile("^child #(.*)", re.I)
laws_regex = re.compile("^Laws (.*)")
roud_regex = re.compile("^Roud(?:-| #| R)(.*)")
recorded_by_regex = re.compile("^recorded by (.*)", re.I)

# our final data dictionary, will be written to data.json
data = {}

for filename in os.listdir("mudcat"):
    file = open("mudcat/" + filename)
    # 247th line of all mudcat html is the actual data...
    text = file.readlines()[246].decode(encoding="UTF-8", errors="replace")
    text = text.strip()
    if not text:
        print filename
        continue

    lines = text.split("<BR>")

    # dictionary for the current file
    song = {}
    # just the body bit of the file, no tags
    lyrics = []

    # search for the break between the title/author and lyrics
    found_title = False
    found_break = False
    author_lines = []
    for line in lines:
        if not found_title:
            if not line:
                continue
            found_title = True
            song["title"] = " ".join(w.capitalize() for w in line.strip().split())
        elif not found_break:
            if line:
                # this is sometimes the author of the lyrics in parens
                author_lines.append(paren_regex.sub("", line.strip()))
            else:
                found_break = True
        elif line.startswith("<A "):
            continue
        elif line.startswith("@"):
            song["tags"] = " ".join([tag.strip() for tag in line.split("@")[1:]])
        elif line.startswith("DT #"):
            continue
        elif line.startswith("TUNE FILE: "):
            song["tune"] = line[11:]
        elif filename_regex.match(line):
            song["name"] = line.split("[")[1].strip()
        else:
            # run capturing matches
            child_match = child_regex.match(line)
            laws_match = laws_regex.match(line)
            recorded_by_match = recorded_by_regex.match(line)
            roud_match = roud_regex.match(line)
            if child_match:
                song["chld"] = child_match.group(1)
            elif laws_match:
                song["laws"] = laws_match.group(1)
            elif recorded_by_match:
                song["rec"] = recorded_by_match.group(1)
            elif roud_match:
                song["roud"] = roud_match.group(1)
            else:
                lyrics.append(line)
    song["txt"] = lyrics
    if author_lines:
        song["a"] = " ".join(author_lines)

    # filenames look like song_xxx.html, grab the xxx
    data[int(filename[5:-5])] = song

out = open("data.json", "w")
json.dump(data, out, indent=0, sort_keys=True)
out.close()

