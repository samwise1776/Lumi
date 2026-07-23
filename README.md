# Lumi

Lumi is a small programming language written in Java. It supports variables,
arithmetic, formatted printing, conditionals, constructors, methods, sleeping,
file creation, user input, and Swing interfaces with frames, labels, buttons,
text boxes, panels, positioning, component fonts, and keyboard actions.

## Example

```lumi
name = "Ray"
answer = 10 + 5 * 2

if name == "Ray" then
    print "Hello *name*"
    print answer
else if name == "Lumi" then
    print "Hello Lumi"
else
    print "Unknown user"
end
```

Loops use `-t-` between the starting and ending numbers. Increment the loop
variable with `++`:

```lumi
for i = 1-t-60
    print i
    i++
end
```

The ending number is included, so this prints 1 through 60. `variable++` also
works outside a loop when the variable contains a number.

Convert a numeric string into an integer with `(int)`. Lumi reports an error
when the string is not a whole number:

```lumi
h = "123"
inth = (int) h
print inth
```

Text boxes can reject non-integer input:

```lumi
textB ageBox = ""
ageBox.setIntsOnly(true)
```

Create a multiline text area with either `textA` or `textarea`. The `scan`
method copies the current text from a text box or text area into a variable:

```lumi
textA notes = "Write here"
notes.scan(savedNotes)
print savedNotes

textB nameBox = "Ray"
nameBox.scan(name)
print name
```

Lumi also reports readable errors for missing quotation marks, missing closing
parentheses/brackets/braces, undefined variables, and using `++` on a value
that is not a number.

File content can be text, an integer, a numeric expression, or a variable:

```lumi
score = 123
file.create("score.txt", score)
file.create("answer.txt", 10 + 5)
file.create("message.txt", "Hello")
```

## Lumi IDE and command help

Open the built-in graphical editor:

```bash
lumi ide
```

The aliases `lumi -ide` and `lumi --ide` work too. The IDE provides new,
open, save, save-as, run, captured program output, keyboard shortcuts, and
unsaved-change protection.

Explore the command line with:

```bash
lumi --help
lumi --keywords
lumi --features
lumi --version
```

Help also accepts `lumi help`, `lumi h`, `lumi -h`, and `lumi -help`.

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

Labels remain Swing `JLabel` components and can show variables or multiple
lines. Use `\n` inside text for a line break:

```lumi
message = "Hello Ray\nWelcome to Lumi"
Label.create(message).varname(greetingLabel)
greetingLabel.setFont("Arial", BOLD, 24)
```

The supported font styles are `BOLD`, `PLAIN`, `ITALIC`, `UNDERLINED`,
`INDENTED`, and `LARGE`. `INDENTED` adds left padding equal to the font size,
while `LARGE` uses a bold font at one-and-a-half times the requested size.

See [`examples/features.lumi`](examples/features.lumi) and
[`examples/gui-and-if.lumi`](examples/gui-and-if.lumi). The complete set of
newer file, input, panel, positioning, and keyboard features is demonstrated in
[`examples/new-features.lumi`](examples/new-features.lumi).

## License

Lumi is available under the MIT License.

This project was developed with AI assistance.
