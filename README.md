# Lumi

Lumi is a small programming language written in Java. It supports variables,
arithmetic, formatted printing, conditionals, constructors, methods, sleeping,
file creation, user input, and Swing interfaces with frames, labels, buttons,
text boxes, panels, positioning, and keyboard actions.

## Example

```lumi
name = "Ray"
answer = 10 + 5 * 2

if name == "Ray" then
    print "Hello *name*"
    print answer
else
    print "Unknown user"
end
```

## Install the Flatpak

Download `Lumi.flatpak` from the latest GitHub release, then run:

```bash
flatpak install --user Lumi.flatpak
flatpak run io.lumicode.Lumi
```

Run a particular program with:

```bash
flatpak run io.lumicode.Lumi program.lumi
```

## Windows, macOS, and Unix

The latest GitHub release contains self-contained downloads for each platform:

- `Lumi-windows-x64.zip` — extract it and run `Lumi.exe`
- `Lumi-macos.zip` — extract it and open `Lumi.app`
- `Lumi-unix-x64.tar.gz` — extract it and run `Lumi/bin/Lumi`

These packages include a private Java runtime, so users do not need to install
Java separately. The macOS build is currently unsigned, so macOS may ask the
user to confirm opening an application downloaded from the internet.

## Build from source

Requirements:

- Java 17 or newer for the standard command
- Flatpak and Flatpak Builder for the Flatpak
- VS Code and Node.js to package the optional editor extension

Install the standard Linux command:

```bash
./install-linux.sh
lumi examples/features.lumi
```

Build and locally install the Flatpak:

```bash
./build-flatpak.sh
```

Install the VS Code extension:

```bash
./install-vscode-extension.sh
```

## Syntax examples

See [`examples/features.lumi`](examples/features.lumi) and
[`examples/gui-and-if.lumi`](examples/gui-and-if.lumi). The complete set of
newer file, input, panel, positioning, and keyboard features is demonstrated in
[`examples/new-features.lumi`](examples/new-features.lumi).

## License

Lumi is available under the MIT License.

This project was developed with AI assistance.
