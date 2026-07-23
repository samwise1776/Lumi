#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# Keep the command outside Snap's versioned XDG_DATA_HOME. Otherwise installing
# from VS Code can create a launcher that breaks after VS Code updates.
data_dir="$HOME/.local/share"
bin_dir="$HOME/.local/bin"
build_dir="$project_dir/build"

mkdir -p "$build_dir/classes" "$data_dir/lumi" "$data_dir/mime/packages"
mkdir -p "$data_dir/icons/hicolor/scalable/mimetypes"
mkdir -p "$data_dir/applications" "$bin_dir"

javac -d "$build_dir/classes" "$project_dir/src/Lumi.java"
jar --create --file "$data_dir/lumi/lumi.jar" --main-class Lumi \
    -C "$build_dir/classes" .

cp "$project_dir/linux/application-x-lumi.xml" \
   "$data_dir/mime/packages/application-x-lumi.xml"
cp "$project_dir/assets/lumi-file.svg" \
   "$data_dir/icons/hicolor/scalable/mimetypes/application-x-lumi.svg"
cp "$project_dir/linux/lumi.desktop" \
   "$data_dir/applications/lumi.desktop"

launcher="$bin_dir/lumi"
{
    printf '%s\n' '#!/bin/sh'
    printf 'exec java -jar "%s/lumi/lumi.jar" "$@"\n' "$data_dir"
} > "$launcher"
chmod +x "$launcher"

update-mime-database "$data_dir/mime"
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$data_dir/applications"
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$data_dir/icons/hicolor" >/dev/null 2>&1 || true
fi

xdg-mime default lumi.desktop application/x-lumi

printf '%s\n' "Installed Lumi and registered the .lumi file icon."
printf '%s\n' "If 'lumi' is not found, add $bin_dir to PATH."
