#!/usr/bin/env bash
# Downloads the source archives of the third-party libraries in the plugin archive, checks each against the SHA-256 in
# the list, and writes SHA256SUMS beside them, ready to attach to the release of the plugin.
#
# Usage: prepare-third-party-sources.sh <list> <folder>
# The list has one archive per line: file name, address and SHA-256, separated by tabs. The folder is emptied first.
set -euo pipefail

if [ "$#" -ne 2 ] || [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 <list> <folder>" >&2
    exit 2
fi
list=$1
folder=$2

if command -v sha256sum >/dev/null 2>&1; then
    checksum_of() { sha256sum "$1" | cut -d ' ' -f 1; }
else
    checksum_of() { shasum -a 256 "$1" | cut -d ' ' -f 1; }
fi

rm -rf "$folder"
mkdir -p "$folder"

failed=0
while IFS=$'\t' read -r file_name address expected_checksum || [ -n "$file_name" ]; do
    [ -n "$file_name" ] || continue
    if ! curl -fsSL --retry 3 -o "$folder/$file_name" "$address"; then
        echo "Could not download $file_name from $address" >&2
        rm -f "$folder/$file_name"
        failed=1
        continue
    fi
    actual_checksum=$(checksum_of "$folder/$file_name")
    if [ "$actual_checksum" != "$expected_checksum" ]; then
        echo "$file_name from $address has SHA-256 $actual_checksum, and the list says $expected_checksum" >&2
        rm -f "$folder/$file_name"
        failed=1
        continue
    fi
    printf '%s  %s\n' "$actual_checksum" "$file_name" >> "$folder/SHA256SUMS"
done < "$list"

exit "$failed"
