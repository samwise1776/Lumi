# Lumi

Lumi is a small programming language written in Java. It supports variables,
arithmetic, formatted printing, conditionals, constructors, methods, sleeping,
and simple Swing interfaces with frames, labels, buttons, and text boxes.

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
[`examples/gui-and-if.lumi`](examples/gui-and-if.lumi).

## License

Lumi is available under the MIT License.

This project was developed with AI assistance.
