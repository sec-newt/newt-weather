#!/usr/bin/env bash
# Rebuilds app/src/main/assets/city-coordinates.csv from the Census Bureau's places
# gazetteer (cities, towns, villages and census-designated places; public domain). Run it
# when a new year's file is out; the app looks city names up in this table, so nothing
# about your location goes online.
# Usage: scripts/update-city-table.sh 2025
set -euo pipefail

year="${1:?Usage: $0 <gazetteer year, e.g. 2025>}"
url="https://www2.census.gov/geo/docs/maps-data/data/gazetteer/${year}_Gazetteer/${year}_Gaz_place_national.zip"
out="app/src/main/assets/city-coordinates.csv"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

curl -sSfL "$url" -o "$tmp/place.zip"
unzip -q "$tmp/place.zip" -d "$tmp"
mkdir -p "$(dirname "$out")"
{
  echo "# name|state|latitude|longitude — Census Bureau ${year} places gazetteer (public domain)."
  echo "# Rebuild with scripts/update-city-table.sh. NOAA accepts at most 4 decimal places."
  # Columns: USPS|GEOID|GEOIDFQ|ANSICODE|NAME|LSAD|FUNCSTAT|ALAND|AWATER|ALAND_SQMI|AWATER_SQMI|INTPTLAT|INTPTLONG
  # "Paris city" → "Paris": the kind of place isn't part of the name people type.
  # When two places share a name in one state, the larger one is kept.
  tail -n +2 "$tmp"/*_Gaz_place_national.txt | tr -d '\r' \
    | awk -F'|' '{
        name = $5
        sub(/ \(balance\)$/, "", name)
        sub(/ (city|town|village|borough|CDP|comunidad|zona urbana|municipality|corporation|unified government|consolidated government|metropolitan government|metro government|urban county)$/, "", name)
        gsub(/ /, "", $13)
        printf "%s|%s|%s|%.4f|%.4f\n", $8, name, $1, $12, $13
      }' \
    | sort -t'|' -k1,1nr \
    | awk -F'|' '!seen[$2 "|" $3]++ { print $2 "|" $3 "|" $4 "|" $5 }' \
    | LC_ALL=C sort -t'|' -k2,2 -k1,1
} > "$out"
echo "Wrote $(grep -vc '^#' "$out") places to $out"
