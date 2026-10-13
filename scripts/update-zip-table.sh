#!/usr/bin/env bash
# Rebuilds app/src/main/assets/zip-coordinates.csv from the Census Bureau's ZIP Code
# Tabulation Area gazetteer (public domain). Run it when a new year's file is out;
# the app looks ZIPs up in this table, so nothing about your location goes online.
# Usage: scripts/update-zip-table.sh 2025
set -euo pipefail

year="${1:?Usage: $0 <gazetteer year, e.g. 2025>}"
url="https://www2.census.gov/geo/docs/maps-data/data/gazetteer/${year}_Gazetteer/${year}_Gaz_zcta_national.zip"
out="app/src/main/assets/zip-coordinates.csv"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

curl -sSfL "$url" -o "$tmp/zcta.zip"
unzip -q "$tmp/zcta.zip" -d "$tmp"
mkdir -p "$(dirname "$out")"
{
  echo "# ZIP,latitude,longitude — Census Bureau ${year} ZCTA gazetteer (public domain)."
  echo "# Rebuild with scripts/update-zip-table.sh. NOAA accepts at most 4 decimal places."
  # Columns: GEOID|GEOIDFQ|ALAND|AWATER|ALAND_SQMI|AWATER_SQMI|INTPTLAT|INTPTLONG
  tail -n +2 "$tmp"/*_Gaz_zcta_national.txt | tr -d '\r' \
    | awk -F'|' '{ gsub(/ /, "", $8); printf "%s,%.4f,%.4f\n", $1, $7, $8 }'
} > "$out"
echo "Wrote $(grep -vc '^#' "$out") ZIPs to $out"
