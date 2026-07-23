#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
manifest="$project_dir/io.lumicode.Lumi.yml"
build_dir="$project_dir/build/flatpak"
repo_dir="$project_dir/build/flatpak-repo"
bundle="$project_dir/Lumi.flatpak"

flatpak remote-add --user --if-not-exists \
    flathub https://dl.flathub.org/repo/flathub.flatpakrepo

flatpak-builder \
    --force-clean \
    --disable-rofiles-fuse \
    --user \
    --install-deps-from=flathub \
    --repo="$repo_dir" \
    --install \
    "$build_dir" \
    "$manifest"

flatpak build-bundle \
    "$repo_dir" \
    "$bundle" \
    io.lumicode.Lumi \
    --runtime-repo=https://dl.flathub.org/repo/flathub.flatpakrepo

printf '%s\n' "Built and installed $bundle"
printf '%s\n' "Run it with: flatpak run io.lumicode.Lumi"
