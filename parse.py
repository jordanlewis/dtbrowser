#!/usr/bin/env python

import sys, os, json, re
from bs4 import BeautifulSoup

# used to search for filename line
filename_regex = re.compile("^filename", re.I)
paren_regex = re.compile("[()]")
child_regex = re.compile("^child #(.*)", re.I)
laws_regex = re.compile("^Laws (.*)")

# our final data dictionary, will be written to data.json
data = {}

for filename in os.listdir("mudcat"):
    file = open("mudcat/" + filename)
    # 247th line of all mudcat html is the actual data...
    text = file.readlines()[246].decode(encoding="UTF-8", errors="replace")
    lines = text.split("<BR>")

    # dictionary for the current file
    song = {}
    # just the body bit of the file, no tags
    lyrics = []

    # search for the break between the title/author and lyrics
    found_break = False
    author_lines = []
    for idx, line in enumerate(lines):
        if idx == 0:
            song["title"] = line.strip()
            continue
        elif not found_break:
            if line:
                # this is sometimes the author of the lyrics in parens
                author_lines.append(paren_regex.sub("", line.strip()))
            else:
                found_break = True
            continue
        elif line.startswith("<A "):
            continue
        # run capturing matches
        child_match = child_regex.match(line)
        laws_match = laws_regex.match(line)
        if filename_regex.match(line):
            song["name"] = line.split("[")[1].strip()
        elif line.startswith("@"):
            song["tags"] = [tag.strip() for tag in line.split("@")[1:]]
        elif line.startswith("TUNE FILE: "):
            song["tune"] = line[11:]
        elif child_match:
            song["chld"] = child_match.group(1)
        elif laws_match:
            song["laws"] = laws_match.group(1)
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

