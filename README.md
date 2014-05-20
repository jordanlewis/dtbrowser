dtbrowser
=========

Data browser for the DigiTrad folk song database

Run ./scrape.py to download the whole DigiTrad database. This could take a
while and might annoy people trying to use their website. I don't recommend
running it at all!

Run ./parse.py once you've scraped or otherwise acquired all the files in the
mudcat/ directory. It will generate a file called data.json that contains
a jsonified version of all of the songs in the database.

Run lein cljsbuild auto dev to start auto compilation of the
ClojureScript front end, and start a webserver with
python -m SimpleHTTPServer to view it.
