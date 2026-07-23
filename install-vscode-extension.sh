#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
extension_dir="$project_dir/vscode-lumi"

if ! command -v code >/dev/null 2>&1; then
    printf '%s\n' "VS Code's 'code' command was not found." >&2
    exit 1
fi

cd "$extension_dir"
npx --yes @vscode/vsce package --out "$project_dir/lumi-language.vsix"
code --install-extension "$project_dir/lumi-language.vsix" --force

printf '%s\n' "Installed the Lumi VS Code extension."
printf '%s\n' "Reload VS Code, then open a .lumi file."
