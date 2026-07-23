import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

public class Lumi {
    private static final String VERSION = "0.5.2";
    private static final Map<String, Object> variables = new HashMap<>();
    private static final Map<String, LumiClass> classes = new HashMap<>();
    private static final Map<String, LumiButton> buttons = new HashMap<>();
    private static final Map<String, JLabel> labels = new HashMap<>();
    private static final Map<String, JTextField> textBoxes = new HashMap<>();
    private static final Map<String, JTextArea> textAreas = new HashMap<>();
    private static final Map<String, JPanel> panels = new HashMap<>();
    private static final Map<String, JComponent> components = new HashMap<>();
    private static final Map<String, List<String>> keyActions = new HashMap<>();
    private static JFrame frame;
    private static String frameTitle = "Lumi";
    private static int frameWidth = 700;
    private static int frameHeight = 900;
    private static Path scriptDirectory = Path.of("").toAbsolutePath();
    private static Path frameIconPath;
    private static int nextComponentY = 10;

    private record LumiMethod(String parameter, List<String> body) {}
    private record LumiButton(String text, JButton component) {}

    private static final class LumiClass {
        private final Map<String, LumiMethod> methods = new LinkedHashMap<>();
        private List<String> constructorBody = List.of();
    }

    public static void main(String[] args) throws Exception {
        Path file;

        if (args.length == 1 && isIdeCommand(args[0])) {
            launchIde();
            return;
        } else if (args.length == 1 && isHelpCommand(args[0])) {
            printHelp();
            return;
        } else if (args.length == 1 && args[0].equals("--keywords")) {
            printKeywords();
            return;
        } else if (args.length == 1 && args[0].equals("--version")) {
            System.out.println("Lumi " + VERSION);
            return;
        } else if (args.length == 1 && args[0].equals("--features")) {
            printFeatures();
            return;
        } else if (args.length == 0 && System.getenv("FLATPAK_ID") != null) {
            file = chooseLumiFile();
            if (file == null) return;
        } else if (args.length == 1) {
            file = Path.of(args[0]);
        } else if (args.length == 2 && args[0].equals("run")) {
            file = Path.of(args[1]);
        } else {
            printHelp();
            return;
        }

        if (!file.toString().endsWith(".lumi")) {
            System.out.println("Lumi files must use the .lumi extension");
            return;
        }

        if (!Files.isRegularFile(file)) {
            System.out.println("Lumi could not find this file:");
            System.out.println("  " + file.toAbsolutePath());
            System.out.println("Check the file name and your terminal's current folder.");
            return;
        }

        Path absoluteFile = file.toAbsolutePath().normalize();
        if (absoluteFile.getParent() != null) {
            scriptDirectory = absoluteFile.getParent();
        }
        List<String> source = Files.readAllLines(file);
        try {
            validateSyntax(source);
            executeLines(source, new HashMap<>());
        } catch (IllegalArgumentException error) {
            System.err.println("Lumi error: " + error.getMessage());
            System.exit(1);
        }
    }

    private static boolean isIdeCommand(String argument) {
        return argument.equals("ide") || argument.equals("-ide") || argument.equals("--ide");
    }

    private static boolean isHelpCommand(String argument) {
        return argument.equals("help")
                || argument.equals("h")
                || argument.equals("-h")
                || argument.equals("-help")
                || argument.equals("--help");
    }

    private static void launchIde() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("The Lumi IDE needs a graphical desktop.");
            return;
        }
        SwingUtilities.invokeLater(() -> new LumiIde().show());
    }

    private static void printHelp() {
        System.out.println("=====LUMI HELP=====");
        System.out.println("Lumi " + VERSION + " - programming language");
        System.out.println();
        System.out.println("Keywords:");
        System.out.println("  print - use a space then quotation marks to print your text");
        System.out.println("          or use a variable, such as: print name");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  lumi <file.lumi>       Run a Lumi program");
        System.out.println("  lumi run <file.lumi>   Run a Lumi program");
        System.out.println("  lumi ide               Open the Lumi IDE");
        System.out.println("  lumi --ide             Open the Lumi IDE");
        System.out.println("  lumi --keywords        List Lumi keywords and built-ins");
        System.out.println("  lumi --features        List language features");
        System.out.println("  lumi --version         Print the Lumi version");
        System.out.println("  lumi help              Show this help");
        System.out.println("  lumi h                 Short help alias");
        System.out.println("  lumi -h                Short help alias");
        System.out.println("  lumi -help             Show this help");
        System.out.println("  lumi --help            Show this help");
    }

    private static void printKeywords() {
        System.out.println("Language: print let var if then else end import true false null");
        System.out.println("System:   System.sleep System.findval System.ctor");
        System.out.println("Desktop:  frame Label LButton textB textA textarea Panel key notify input");
        System.out.println("Files:    file.create");
        System.out.println("Members:  create close delete varname visible icon action listen");
        System.out.println("          add setX setY setFont setIntsOnly read readFor scan");
    }

    private static void printFeatures() {
        System.out.println("Lumi features:");
        System.out.println("  Variables, strings, interpolation, arithmetic, and conditions");
        System.out.println("  Constructors, imports, methods, parameters, and sleeping");
        System.out.println("  Files, user input, and desktop notifications");
        System.out.println("  Frames, labels, buttons, text boxes, panels, and keyboard actions");
        System.out.println("  Built-in graphical IDE with open, save, run, and output");
    }

    private static Path chooseLumiFile() throws Exception {
        final Path[] selected = new Path[1];
        SwingUtilities.invokeAndWait(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open a Lumi program");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Lumi programs (*.lumi)", "lumi"));
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                selected[0] = chooser.getSelectedFile().toPath();
            }
        });
        return selected[0];
    }

    private static void executeLines(List<String> lines, Map<String, Object> locals)
            throws Exception {
        for (int index = 0; index < lines.size(); index++) {
            String line = clean(lines.get(index));

            if (line.isBlank() || line.equals("}")) {
                continue;
            }

            Matcher multilineAssignment = Pattern
                    .compile("(?:(?:let|var)\\s+)?([A-Za-z_]\\w*)\\s*=\\s*\\{")
                    .matcher(line);
            if (multilineAssignment.matches()) {
                Block block = readBlock(lines, index);
                variables.put(
                        multilineAssignment.group(1),
                        concatenateBlock(block.lines(), locals));
                index = block.endIndex();
                continue;
            }

            Matcher constructor = Pattern
                    .compile("System\\.ctor\\(([A-Za-z_]\\w*)\\)\\s*\\{")
                    .matcher(line);
            if (constructor.matches()) {
                Block block = readBlock(lines, index);
                defineClass(constructor.group(1), block.lines(), locals);
                index = block.endIndex();
                continue;
            }

            Matcher action = Pattern
                    .compile("([A-Za-z_]\\w*)\\.action\\(\\)\\s*\\{")
                    .matcher(line);
            if (action.matches()) {
                Block block = readBlock(lines, index);
                addButtonAction(action.group(1), block.lines());
                index = block.endIndex();
                continue;
            }

            Matcher keyListener = Pattern
                    .compile("key\\.listen\\(([^)]+)\\)\\s*\\{")
                    .matcher(line);
            if (keyListener.matches()) {
                Block block = readBlock(lines, index);
                registerKeyAction(keyListener.group(1).trim(), block.lines());
                index = block.endIndex();
                continue;
            }

            Matcher forLoop = Pattern
                    .compile("for\\s+([A-Za-z_]\\w*)\\s*=\\s*(-?\\d+)\\s*-t-\\s*(-?\\d+)")
                    .matcher(line);
            if (forLoop.matches()) {
                Block block = readEndBlock(lines, index);
                runForLoop(
                        forLoop.group(1),
                        Long.parseLong(forLoop.group(2)),
                        Long.parseLong(forLoop.group(3)),
                        block.lines(),
                        locals);
                index = block.endIndex();
                continue;
            }

            Matcher condition = Pattern.compile("if\\s+(.+?)\\s+then").matcher(line);
            if (condition.matches()) {
                ConditionalBlock block = readConditional(lines, index);
                List<String> selected = block.whenFalse();
                if (evaluateCondition(condition.group(1), locals)) {
                    selected = block.whenTrue();
                } else {
                    for (ElseIfBranch branch : block.elseIfBranches()) {
                        if (evaluateCondition(branch.condition(), locals)) {
                            selected = branch.lines();
                            break;
                        }
                    }
                }
                executeLines(selected, new HashMap<>(locals));
                index = block.endIndex();
                continue;
            }

            executeStatement(line, locals);
        }
    }

    private static void executeStatement(String line, Map<String, Object> locals)
            throws Exception {
        Matcher importConstructor = Pattern
                .compile("import\\s+([A-Za-z_]\\w*)\\s*;?")
                .matcher(line);
        if (importConstructor.matches()) {
            importConstructor(importConstructor.group(1), locals);
            return;
        }

        Matcher fileCreate = Pattern
                .compile("file\\.create\\(\"([^\"]+)\",\\s*(.+)\\)\\s*;?")
                .matcher(line);
        if (fileCreate.matches()) {
            createFile(
                    fileCreate.group(1),
                    display(evaluate(fileCreate.group(2), locals)));
            return;
        }

        Matcher input = Pattern
                .compile("input\\(\"(.*)\"\\s*\\(([A-Za-z_]\\w*)\\)\\s*\\)\\s*;?")
                .matcher(line);
        if (input.matches()) {
            variables.put(input.group(2), readInput(interpolate(input.group(1), locals)));
            return;
        }

        Matcher notification = Pattern
                .compile("notify\\(\"(.*)\",\\s*\"(.*)\"\\)\\s*;?")
                .matcher(line);
        if (notification.matches()) {
            sendNotification(
                    interpolate(notification.group(1), locals),
                    interpolate(notification.group(2), locals));
            return;
        }

        Matcher frameCreate = Pattern.compile("frame\\.create\\(\"(.*)\"\\)\\s*;?")
                .matcher(line);
        if (frameCreate.matches()) {
            createFrame(frameCreate.group(1));
            return;
        }

        if (line.matches("frame\\.close\\(\\)\\s*;?")) {
            closeFrame();
            return;
        }

        Matcher frameSize = Pattern
                .compile("frame\\.size\\s*=\\s*(\\d+)\\s*,\\s*(\\d+)\\s*;?")
                .matcher(line);
        if (frameSize.matches()) {
            frameWidth = Integer.parseInt(frameSize.group(1));
            frameHeight = Integer.parseInt(frameSize.group(2));
            if (frame != null) {
                frame.setSize(frameWidth, frameHeight);
            }
            return;
        }

        Matcher frameIcon = Pattern.compile("frame\\.icon\\(\"(.*)\"\\)\\s*;?")
                .matcher(line);
        if (frameIcon.matches()) {
            setFrameIcon(frameIcon.group(1));
            return;
        }

        Matcher frameVisible = Pattern
                .compile("frame\\.visible\\((true|false)\\)\\s*;?")
                .matcher(line);
        if (frameVisible.matches()) {
            if (frame != null) {
                frame.setVisible(Boolean.parseBoolean(frameVisible.group(1)));
            }
            return;
        }

        Matcher buttonCreate = Pattern
                .compile("LButton\\.new\\.create\\(\"(.*)\"\\)\\.varname\\.([A-Za-z_]\\w*)\\s*;?")
                .matcher(line);
        if (buttonCreate.matches()) {
            createButton(buttonCreate.group(2), buttonCreate.group(1));
            return;
        }

        Matcher labelCreate = Pattern
                .compile("Label\\.create\\((.+)\\)\\.varname\\(([A-Za-z_]\\w*)\\)\\s*;?")
                .matcher(line);
        if (labelCreate.matches()) {
            createLabel(
                    labelCreate.group(2),
                    display(evaluate(labelCreate.group(1), locals)));
            return;
        }

        Matcher panelCreate = Pattern
                .compile("Panel\\s+([A-Za-z_]\\w*)\\s*=\\s*\\((.*?)\\)\\s*;?")
                .matcher(line);
        if (panelCreate.matches()) {
            createPanel(panelCreate.group(1), panelCreate.group(2));
            return;
        }

        Matcher panelAdd = Pattern
                .compile("([A-Za-z_]\\w*)\\.add\\(([A-Za-z_]\\w*)\\)\\s*;?")
                .matcher(line);
        if (panelAdd.matches() && panels.containsKey(panelAdd.group(1))) {
            addToPanel(panelAdd.group(1), panelAdd.group(2));
            return;
        }

        Matcher setPosition = Pattern
                .compile("([A-Za-z_]\\w*)\\.set([XY])\\(([+\\-*]?\\d+)\\)\\s*;?")
                .matcher(line);
        if (setPosition.matches()) {
            setComponentPosition(
                    setPosition.group(1),
                    setPosition.group(2),
                    setPosition.group(3));
            return;
        }

        Matcher setFont = Pattern
                .compile("([A-Za-z_]\\w*)\\.setFont\\(\"([^\"]+)\"\\s*,\\s*"
                        + "(BOLD|PLAIN|ITALIC|UNDERLINED|INDENTED|LARGE)\\s*,\\s*"
                        + "(\\d+)\\)\\s*;?", Pattern.CASE_INSENSITIVE)
                .matcher(line);
        if (setFont.matches()) {
            setComponentFont(
                    setFont.group(1),
                    setFont.group(2),
                    setFont.group(3),
                    Integer.parseInt(setFont.group(4)));
            return;
        }

        Matcher deleteComponent = Pattern
                .compile("([A-Za-z_]\\w*)\\.delete(?:\\(\\))?\\s*;?")
                .matcher(line);
        if (deleteComponent.matches()) {
            deleteComponent(deleteComponent.group(1));
            return;
        }

        Matcher textBoxCreate = Pattern
                .compile("textB\\s+([A-Za-z_]\\w*)\\s*=\\s*\"(.*)\"\\s*;?")
                .matcher(line);
        if (textBoxCreate.matches()) {
            createTextBox(textBoxCreate.group(1), textBoxCreate.group(2));
            return;
        }

        Matcher textAreaCreate = Pattern
                .compile("(?:textA|textarea)\\s+([A-Za-z_]\\w*)\\s*=\\s*\"(.*)\"\\s*;?")
                .matcher(line);
        if (textAreaCreate.matches()) {
            createTextArea(
                    textAreaCreate.group(1),
                    interpolate(textAreaCreate.group(2), locals)
                            .replace("\\n", "\n")
                            .replace("\\t", "\t"));
            return;
        }

        Matcher intsOnly = Pattern
                .compile("([A-Za-z_]\\w*)\\.setIntsOnly\\((true|false)\\)\\s*;?",
                        Pattern.CASE_INSENSITIVE)
                .matcher(line);
        if (intsOnly.matches()) {
            setTextBoxIntsOnly(
                    intsOnly.group(1),
                    Boolean.parseBoolean(intsOnly.group(2)));
            return;
        }

        Matcher readFor = Pattern
                .compile("([A-Za-z_]\\w*)\\.read\\.readFor\\(\"(.*)\"\\)\\s*;?")
                .matcher(line);
        if (readFor.matches()) {
            JTextField box = textBoxes.get(readFor.group(1));
            if (box != null) {
                box.setText(readFor.group(2));
            }
            return;
        }

        Matcher readText = Pattern
                .compile("([A-Za-z_]\\w*)\\.read\\(([A-Za-z_]\\w*)\\)\\s*;?")
                .matcher(line);
        if (readText.matches()) {
            JTextField box = textBoxes.get(readText.group(1));
            if (box == null) {
                System.out.println("Unknown text box: " + readText.group(1));
            } else {
                variables.put(readText.group(2), box.getText());
            }
            return;
        }

        Matcher scanText = Pattern
                .compile("([A-Za-z_]\\w*)\\.scan\\(([A-Za-z_]\\w*)\\)\\s*;?")
                .matcher(line);
        if (scanText.matches()) {
            scanTextComponent(scanText.group(1), scanText.group(2));
            return;
        }

        Matcher sleep = Pattern.compile("System\\.sleep\\((\\d+)\\)\\s*;?").matcher(line);
        if (sleep.matches()) {
            Thread.sleep(Long.parseLong(sleep.group(1)));
            return;
        }

        Matcher increment = Pattern
                .compile("([A-Za-z_]\\w*)\\+\\+\\s*;?")
                .matcher(line);
        if (increment.matches()) {
            incrementVariable(increment.group(1), locals);
            return;
        }

        Matcher assignment = Pattern
                .compile("(?:(?:let|var)\\s+)?([A-Za-z_]\\w*)\\s*=\\s*(.+?)\\s*;?")
                .matcher(line);
        if (assignment.matches() && !line.contains("==")) {
            variables.put(assignment.group(1), evaluate(assignment.group(2), locals));
            return;
        }

        if (line.startsWith("print ")) {
            System.out.println(evaluatePrint(line.substring(6).trim(), locals));
            return;
        }

        Matcher methodCall = Pattern
                .compile("([A-Za-z_]\\w*)\\.([A-Za-z_]\\w*)\\((.*?)\\)\\s*;?")
                .matcher(line);
        if (methodCall.matches()) {
            callMethod(
                    methodCall.group(1),
                    methodCall.group(2),
                    methodCall.group(3).trim(),
                    locals);
            return;
        }

        throw new IllegalArgumentException("Unknown instruction: " + line);
    }

    private static String concatenateBlock(List<String> lines, Map<String, Object> locals) {
        StringBuilder result = new StringBuilder();
        for (String rawLine : lines) {
            String part = clean(rawLine);
            if (part.isBlank()) continue;
            if (part.startsWith("+")) part = part.substring(1).trim();
            result.append(display(evaluate(part, locals)));
        }
        return result.toString();
    }

    private static Object evaluatePrint(String expression, Map<String, Object> locals) {
        Matcher percentFormat = Pattern
                .compile("f\"(.*?%s.*?)\"\\s*\\+\\s*([A-Za-z_]\\w*)\\s*;?")
                .matcher(expression);
        if (percentFormat.matches()) {
            Object value = findVariable(percentFormat.group(2), locals);
            return percentFormat.group(1).replace("%s", display(value));
        }

        Matcher interpolation = Pattern.compile("f\"(.*)\"\\s*;?").matcher(expression);
        if (interpolation.matches()) {
            Matcher marker = Pattern.compile("\\*([A-Za-z_]\\w*)\\*")
                    .matcher(interpolation.group(1));
            StringBuffer result = new StringBuffer();
            while (marker.find()) {
                marker.appendReplacement(
                        result,
                        Matcher.quoteReplacement(display(findVariable(marker.group(1), locals))));
            }
            marker.appendTail(result);
            return result.toString();
        }

        return evaluate(expression, locals);
    }

    private static Object evaluate(String expression, Map<String, Object> locals) {
        String value = expression.trim().replaceFirst(";\\s*$", "");

        Matcher intConversion = Pattern.compile("\\(int\\)\\s*(.+)").matcher(value);
        if (intConversion.matches()) {
            Object converted = evaluate(intConversion.group(1), locals);
            if (converted instanceof Number number) return number.longValue();
            String text = display(converted).trim();
            if (!text.matches("[+-]?\\d+")) {
                throw new IllegalArgumentException(
                        "Cannot convert \"" + text
                                + "\" to int because it is not a whole number.");
            }
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                        "Cannot convert \"" + text + "\" to int because it is too large.");
            }
        }

        Matcher find = Pattern
                .compile("System\\.findval\\(([A-Za-z_]\\w*)\\)")
                .matcher(value);
        if (find.matches()) {
            return findVariable(find.group(1), locals);
        }

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            String text = value.substring(1, value.length() - 1)
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"");
            return interpolate(text, locals);
        }

        if (value.matches("-?\\d+")) {
            return Long.parseLong(value);
        }

        if (value.matches("[\\w\\s()+\\-*/.]+") && value.matches(".*[+\\-*/()].*")) {
            return new ArithmeticParser(value, locals).parse();
        }

        return findVariable(value, locals);
    }

    private static Object findVariable(String name, Map<String, Object> locals) {
        if (locals.containsKey(name)) {
            return locals.get(name);
        }
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        throw new IllegalArgumentException(
                "Undefined variable \"" + name
                        + "\". If you meant to print text, put quotation marks around it.");
    }

    private record OpenDelimiter(char character, int line) {}

    private static void validateSyntax(List<String> lines) {
        List<OpenDelimiter> open = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            boolean inString = false;
            for (int index = 0; index < line.length(); index++) {
                char current = line.charAt(index);
                if (current == '"' && (index == 0 || line.charAt(index - 1) != '\\')) {
                    inString = !inString;
                    continue;
                }
                if (!inString && current == '#') break;
                if (inString) continue;
                if (current == '(' || current == '[' || current == '{') {
                    open.add(new OpenDelimiter(current, lineIndex + 1));
                } else if (current == ')' || current == ']' || current == '}') {
                    if (open.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Line " + (lineIndex + 1)
                                        + ": unexpected closing " + current);
                    }
                    OpenDelimiter last = open.remove(open.size() - 1);
                    if (!delimitersMatch(last.character(), current)) {
                        throw new IllegalArgumentException(
                                "Line " + (lineIndex + 1) + ": expected "
                                        + closingDelimiter(last.character())
                                        + " before " + current);
                    }
                }
            }
            if (inString) {
                throw new IllegalArgumentException(
                        "Line " + (lineIndex + 1)
                                + ": missing closing quotation mark (\").");
            }
        }
        if (!open.isEmpty()) {
            OpenDelimiter last = open.get(open.size() - 1);
            throw new IllegalArgumentException(
                    "Line " + last.line() + ": missing closing "
                            + closingDelimiter(last.character()));
        }
    }

    private static boolean delimitersMatch(char opening, char closing) {
        return (opening == '(' && closing == ')')
                || (opening == '[' && closing == ']')
                || (opening == '{' && closing == '}');
    }

    private static char closingDelimiter(char opening) {
        return switch (opening) {
            case '(' -> ')';
            case '[' -> ']';
            default -> '}';
        };
    }

    private static void defineClass(
            String name, List<String> body, Map<String, Object> locals) throws Exception {
        LumiClass lumiClass = new LumiClass();
        List<String> constructorBody = new ArrayList<>();

        for (int index = 0; index < body.size(); index++) {
            String line = clean(body.get(index));
            Matcher method = Pattern
                    .compile("\\.([A-Za-z_]\\w*)\\s*(?:\\(([A-Za-z_]\\w*)?\\))?\\s*\\{")
                    .matcher(line);
            if (method.matches()) {
                Block methodBlock = readBlock(body, index);
                lumiClass.methods.put(
                        method.group(1),
                        new LumiMethod(method.group(2), methodBlock.lines()));
                index = methodBlock.endIndex();
            } else {
                constructorBody.add(body.get(index));
            }
        }

        lumiClass.constructorBody = List.copyOf(constructorBody);
        classes.put(name, lumiClass);
    }

    private static void importConstructor(
            String name, Map<String, Object> callerLocals) throws Exception {
        LumiClass lumiClass = classes.get(name);
        if (lumiClass == null) {
            System.out.println("Cannot import unknown constructor: " + name);
            return;
        }
        executeLines(lumiClass.constructorBody, new HashMap<>(callerLocals));
    }

    private static void callMethod(
            String className,
            String methodName,
            String argument,
            Map<String, Object> callerLocals) throws Exception {
        LumiClass lumiClass = classes.get(className);
        if (lumiClass == null) {
            System.out.println("Unknown constructor: " + className);
            return;
        }

        LumiMethod method = lumiClass.methods.get(methodName);
        if (method == null) {
            System.out.println("Unknown method: " + className + "." + methodName);
            return;
        }

        Map<String, Object> methodLocals = new HashMap<>();
        if (method.parameter() != null) {
            methodLocals.put(method.parameter(), evaluate(argument, callerLocals));
        }
        executeLines(method.body(), methodLocals);
    }

    private record Block(List<String> lines, int endIndex) {}

    private static Block readEndBlock(List<String> lines, int startIndex) {
        List<String> body = new ArrayList<>();
        int depth = 1;
        for (int index = startIndex + 1; index < lines.size(); index++) {
            String line = clean(lines.get(index));
            if (line.matches("(?:if\\s+.+\\s+then|for\\s+.+)")) {
                depth++;
            } else if (line.equals("end")) {
                depth--;
                if (depth == 0) return new Block(body, index);
            }
            body.add(lines.get(index));
        }
        throw new IllegalArgumentException(
                "Missing end for for loop on line " + (startIndex + 1));
    }

    private static void runForLoop(
            String variable,
            long start,
            long end,
            List<String> body,
            Map<String, Object> locals) throws Exception {
        variables.put(variable, start);
        while (numberVariable(variable, locals) <= end) {
            long before = numberVariable(variable, locals);
            executeLines(body, locals);
            long after = numberVariable(variable, locals);
            if (after <= before) {
                throw new IllegalArgumentException(
                        "The for loop variable " + variable
                                + " must increase. Add " + variable + "++ inside the loop.");
            }
        }
    }

    private static void incrementVariable(String name, Map<String, Object> locals) {
        long incremented = numberVariable(name, locals) + 1;
        if (locals.containsKey(name)) locals.put(name, incremented);
        else variables.put(name, incremented);
    }

    private static long numberVariable(String name, Map<String, Object> locals) {
        Object value = findVariable(name, locals);
        if (value instanceof Number number) return number.longValue();
        throw new IllegalArgumentException(name + " must contain a number");
    }

    private static Block readBlock(List<String> lines, int startIndex) {
        List<String> body = new ArrayList<>();
        int depth = braceChange(lines.get(startIndex));

        for (int index = startIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index);
            int change = braceChange(line);
            depth += change;
            if (depth == 0) {
                return new Block(body, index);
            }
            body.add(line);
        }

        throw new IllegalArgumentException(
                "Missing closing } for block on line " + (startIndex + 1));
    }

    private static int braceChange(String line) {
        boolean inString = false;
        int change = 0;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"' && (index == 0 || line.charAt(index - 1) != '\\')) {
                inString = !inString;
            } else if (!inString && current == '{') {
                change++;
            } else if (!inString && current == '}') {
                change--;
            }
        }
        return change;
    }

    private static String clean(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            return "";
        }
        return trimmed;
    }

    private static String display(Object value) {
        return value == null ? "null" : value.toString();
    }

    private static String interpolate(String text, Map<String, Object> locals) {
        Matcher marker = Pattern.compile("\\*([A-Za-z_]\\w*)\\*").matcher(text);
        StringBuffer result = new StringBuffer();
        while (marker.find()) {
            marker.appendReplacement(
                    result,
                    Matcher.quoteReplacement(display(findVariable(marker.group(1), locals))));
        }
        marker.appendTail(result);
        return result.toString();
    }

    private static boolean evaluateCondition(String condition, Map<String, Object> locals) {
        Matcher comparison = Pattern
                .compile("(.+?)\\s*(==|!=|<=|>=|<|>)\\s*(.+)")
                .matcher(condition.trim());
        if (!comparison.matches()) {
            Object value = evaluate(condition, locals);
            return !(value == null || value.equals(false) || value.equals(0L)
                    || value.toString().isBlank());
        }

        Object left = evaluate(comparison.group(1), locals);
        Object right = evaluate(comparison.group(3), locals);
        String operator = comparison.group(2);

        if (operator.equals("==")) return left.equals(right);
        if (operator.equals("!=")) return !left.equals(right);

        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            double first = leftNumber.doubleValue();
            double second = rightNumber.doubleValue();
            return switch (operator) {
                case "<" -> first < second;
                case ">" -> first > second;
                case "<=" -> first <= second;
                case ">=" -> first >= second;
                default -> false;
            };
        }

        int order = left.toString().compareTo(right.toString());
        return switch (operator) {
            case "<" -> order < 0;
            case ">" -> order > 0;
            case "<=" -> order <= 0;
            case ">=" -> order >= 0;
            default -> false;
        };
    }

    private record ElseIfBranch(String condition, List<String> lines) {}

    private record ConditionalBlock(
            List<String> whenTrue,
            List<ElseIfBranch> elseIfBranches,
            List<String> whenFalse,
            int endIndex) {}

    private static ConditionalBlock readConditional(List<String> lines, int startIndex) {
        List<String> whenTrue = new ArrayList<>();
        List<ElseIfBranch> elseIfBranches = new ArrayList<>();
        List<String> whenFalse = new ArrayList<>();
        List<String> current = whenTrue;
        int depth = 1;

        for (int index = startIndex + 1; index < lines.size(); index++) {
            String line = clean(lines.get(index));
            Matcher elseIf = Pattern.compile("else\\s+if\\s+(.+?)\\s+then").matcher(line);
            if (depth == 1 && elseIf.matches()) {
                List<String> branchLines = new ArrayList<>();
                elseIfBranches.add(new ElseIfBranch(elseIf.group(1), branchLines));
                current = branchLines;
                continue;
            }
            if (line.equals("else") && depth == 1) {
                current = whenFalse;
                continue;
            }
            if (line.matches("if\\s+.+\\s+then")) {
                depth++;
            } else if (line.equals("end")) {
                depth--;
                if (depth == 0) {
                    return new ConditionalBlock(
                            whenTrue, List.copyOf(elseIfBranches), whenFalse, index);
                }
            }
            current.add(lines.get(index));
        }

        throw new IllegalArgumentException(
                "Missing end for if statement on line " + (startIndex + 1));
    }

    private static void createFrame(String title) throws Exception {
        frameTitle = title;
        nextComponentY = 10;
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) frame.dispose();
            frame = new JFrame(frameTitle);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(frameWidth, frameHeight);
            frame.setLayout(null);
            if (frameIconPath != null) {
                frame.setIconImage(new ImageIcon(frameIconPath.toString()).getImage());
            }
            for (Map.Entry<String, List<String>> action : keyActions.entrySet()) {
                attachKeyAction(action.getKey(), action.getValue());
            }
        });
    }

    private static void setFrameIcon(String fileName) {
        Path requested = Path.of(fileName);
        frameIconPath = (requested.isAbsolute() ? requested : scriptDirectory.resolve(requested))
                .normalize();
        if (!Files.isRegularFile(frameIconPath)) {
            System.out.println("Frame icon not found: " + frameIconPath);
            frameIconPath = null;
            return;
        }
        if (frame != null) {
            frame.setIconImage(new ImageIcon(frameIconPath.toString()).getImage());
        }
    }

    private static void closeFrame() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private static void createButton(String name, String text) {
        JButton component = new JButton(text);
        buttons.put(name, new LumiButton(text, component));
        components.put(name, component);
        addToFrame(component);
    }

    private static void addButtonAction(String name, List<String> body) {
        LumiButton button = buttons.get(name);
        if (button == null) {
            System.out.println("Unknown button: " + name);
            return;
        }
        button.component().addActionListener(event -> runEventBody(body));
    }

    private static void createLabel(String name, String text) {
        JLabel label = new JLabel(formatLabelText(text));
        labels.put(name, label);
        components.put(name, label);
        addToFrame(label);
    }

    private static String formatLabelText(String text) {
        if (!text.contains("\n") && !text.contains("\r")) return text;
        return "<html>" + escapeHtml(text)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", "<br>") + "</html>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void createTextBox(String name, String initialText) {
        JTextField box = new JTextField(initialText, 20);
        textBoxes.put(name, box);
        components.put(name, box);
        addToFrame(box);
    }

    private static void createTextArea(String name, String initialText) {
        JTextArea area = new JTextArea(initialText, 5, 24);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        textAreas.put(name, area);
        components.put(name, area);
        addToFrame(area);
    }

    private static void scanTextComponent(String componentName, String variableName) {
        JTextComponent textComponent = findTextComponent(componentName);
        variables.put(variableName, textComponent.getText());
    }

    private static void setTextBoxIntsOnly(String name, boolean enabled) {
        JTextComponent box = findTextComponent(name);
        AbstractDocument document = (AbstractDocument) box.getDocument();
        if (!enabled) {
            document.setDocumentFilter(null);
            return;
        }
        if (!box.getText().matches("-?\\d*")) box.setText("");
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(
                    FilterBypass bypass,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attributes) throws BadLocationException {
                String current = bypass.getDocument().getText(
                        0, bypass.getDocument().getLength());
                String replacement = text == null ? "" : text;
                String updated = current.substring(0, offset)
                        + replacement
                        + current.substring(offset + length);
                if (updated.matches("-?\\d*")) {
                    super.replace(bypass, offset, length, replacement, attributes);
                }
            }

            @Override
            public void insertString(
                    FilterBypass bypass,
                    int offset,
                    String text,
                    AttributeSet attributes) throws BadLocationException {
                replace(bypass, offset, 0, text, attributes);
            }
        });
    }

    private static JTextComponent findTextComponent(String name) {
        JTextField box = textBoxes.get(name);
        if (box != null) return box;
        JTextArea area = textAreas.get(name);
        if (area != null) return area;
        throw new IllegalArgumentException("Unknown text box or text area: " + name);
    }

    private static void createPanel(String name, String componentNames) {
        JPanel panel = new JPanel(new java.awt.FlowLayout());
        panels.put(name, panel);
        components.put(name, panel);
        addToFrame(panel);
        if (!componentNames.isBlank()) {
            for (String componentName : componentNames.split(",")) {
                addToPanel(name, componentName.trim());
            }
        }
    }

    private static void addToPanel(String panelName, String componentName) {
        JPanel panel = panels.get(panelName);
        JComponent component = components.get(componentName);
        if (component == null) {
            System.out.println("Unknown component: " + componentName);
            return;
        }
        panel.add(component);
        panel.setSize(panel.getPreferredSize());
        panel.revalidate();
        panel.repaint();
    }

    private static void addToFrame(JComponent component) {
        if (frame != null) {
            java.awt.Dimension size = component.getPreferredSize();
            component.setBounds(10, nextComponentY, Math.max(size.width, 80), Math.max(size.height, 28));
            nextComponentY += Math.max(size.height, 28) + 10;
            frame.add(component);
            frame.revalidate();
            frame.repaint();
        }
    }

    private static void setComponentPosition(String name, String axis, String operation) {
        JComponent component = components.get(name);
        if (component == null) {
            System.out.println("Unknown component: " + name);
            return;
        }
        int current = axis.equals("X") ? component.getX() : component.getY();
        int amount = Integer.parseInt(
                operation.startsWith("+") || operation.startsWith("-") || operation.startsWith("*")
                        ? operation.substring(1)
                        : operation);
        int updated;
        if (operation.startsWith("+")) updated = current + amount;
        else if (operation.startsWith("-")) updated = current - amount;
        else if (operation.startsWith("*")) updated = current * amount;
        else updated = amount;
        if (axis.equals("X")) component.setLocation(updated, component.getY());
        else component.setLocation(component.getX(), updated);
    }

    private static void setComponentFont(
            String name, String family, String requestedStyle, int requestedSize) {
        JComponent component = components.get(name);
        if (component == null) {
            System.out.println("Unknown component: " + name);
            return;
        }
        if (requestedSize < 1) {
            System.out.println("Font size must be at least 1");
            return;
        }

        String styleName = requestedStyle.toUpperCase();
        int javaStyle = switch (styleName) {
            case "BOLD", "LARGE" -> Font.BOLD;
            case "ITALIC" -> Font.ITALIC;
            default -> Font.PLAIN;
        };
        float size = styleName.equals("LARGE") ? requestedSize * 1.5f : requestedSize;
        Font font = new Font(family, javaStyle, Math.round(size));
        if (styleName.equals("UNDERLINED")) {
            Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            font = font.deriveFont(attributes);
        }
        component.setFont(font);

        if (styleName.equals("INDENTED")) {
            component.setBorder(BorderFactory.createCompoundBorder(
                    component.getBorder(),
                    BorderFactory.createEmptyBorder(0, requestedSize, 0, 0)));
        }

        Dimension preferred = component.getPreferredSize();
        component.setSize(
                Math.max(component.getWidth(), preferred.width),
                Math.max(component.getHeight(), preferred.height));
        component.revalidate();
        component.repaint();
    }

    private static void deleteComponent(String name) {
        JComponent component = components.remove(name);
        if (component == null) {
            System.out.println("Unknown component: " + name);
            return;
        }

        java.awt.Container parent = component.getParent();
        if (parent != null) {
            parent.remove(component);
            parent.revalidate();
            parent.repaint();
        }

        buttons.remove(name);
        labels.remove(name);
        textBoxes.remove(name);
        textAreas.remove(name);
        panels.remove(name);
    }

    private static void registerKeyAction(String key, List<String> body) {
        String normalized = key.replace("\"", "").toUpperCase();
        keyActions.put(normalized, List.copyOf(body));
        if (frame != null) attachKeyAction(normalized, body);
    }

    private static void attachKeyAction(String key, List<String> body) {
        KeyStroke stroke = KeyStroke.getKeyStroke("pressed " + key);
        if (stroke == null || frame == null) {
            System.out.println("Unknown key: " + key);
            return;
        }
        frame.getRootPane().registerKeyboardAction(
                event -> runEventBody(body),
                stroke,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static void runEventBody(List<String> body) {
        try {
            executeLines(body, new HashMap<>());
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private static void createFile(String fileName, String content) throws Exception {
        Path requested = Path.of(fileName);
        Path target = (requested.isAbsolute() ? requested : scriptDirectory.resolve(requested))
                .normalize();
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    private static String readInput(String prompt) throws Exception {
        if (!GraphicsEnvironment.isHeadless() && frame != null && frame.isVisible()) {
            String answer = JOptionPane.showInputDialog(frame, prompt);
            return answer == null ? "" : answer;
        }
        System.out.print(prompt);
        System.out.flush();
        String answer = new BufferedReader(new InputStreamReader(System.in)).readLine();
        return answer == null ? "" : answer;
    }

    private static void sendNotification(String title, String message) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[" + title + "] " + message);
            return;
        }

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(
                    frame,
                    message,
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.Image image = frameIconPath != null
                    ? new ImageIcon(frameIconPath.toString()).getImage()
                    : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            TrayIcon icon = new TrayIcon(image, "Lumi");
            icon.setImageAutoSize(true);
            tray.add(icon);
            icon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            javax.swing.Timer cleanup = new javax.swing.Timer(5000, event -> {
                tray.remove(icon);
                ((javax.swing.Timer) event.getSource()).stop();
            });
            cleanup.setRepeats(false);
            cleanup.start();
        } catch (java.awt.AWTException error) {
            JOptionPane.showMessageDialog(
                    frame,
                    message,
                    title,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static final class LumiIde {
        private final JFrame window = new JFrame("Lumi IDE");
        private final JTextArea editor = new JTextArea();
        private final JTextArea output = new JTextArea();
        private final JLabel status = new JLabel("Ready");
        private Path currentFile;
        private boolean dirty;

        LumiIde() {
            configureWindow();
            configureEditor();
            configureOutput();
            window.setJMenuBar(createMenuBar());
            window.add(createToolbar(), BorderLayout.NORTH);

            JSplitPane split = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(editor),
                    new JScrollPane(output));
            split.setResizeWeight(0.75);
            split.setDividerLocation(520);
            window.add(split, BorderLayout.CENTER);

            status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            window.add(status, BorderLayout.SOUTH);
            updateTitle();
        }

        void show() {
            window.setVisible(true);
            editor.requestFocusInWindow();
        }

        private void configureWindow() {
            window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            window.setSize(950, 720);
            window.setMinimumSize(new Dimension(650, 450));
            window.setLocationRelativeTo(null);
            if (frameIconPath != null) {
                window.setIconImage(new ImageIcon(frameIconPath.toString()).getImage());
            }
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent event) {
                    if (confirmDiscardChanges()) window.dispose();
                }
            });
        }

        private void configureEditor() {
            editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
            editor.setTabSize(4);
            editor.setText("""
                    # Welcome to the Lumi IDE
                    name = "Ray"
                    print "Hello *name*"
                    """);
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    markDirty();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    markDirty();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    markDirty();
                }
            });
            dirty = false;
        }

        private void configureOutput() {
            output.setEditable(false);
            output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            output.setText("Lumi IDE " + VERSION + "\n");
        }

        private JMenuBar createMenuBar() {
            JMenuBar bar = new JMenuBar();
            JMenu file = new JMenu("File");
            file.add(menuItem("New", "control N", event -> newFile()));
            file.add(menuItem("Open…", "control O", event -> openFile()));
            file.add(menuItem("Save", "control S", event -> save()));
            file.add(menuItem("Save As…", "control shift S", event -> saveAs()));
            file.addSeparator();
            file.add(menuItem("Exit", "alt F4", event -> {
                if (confirmDiscardChanges()) window.dispose();
            }));

            JMenu run = new JMenu("Run");
            run.add(menuItem("Run Program", "control F5", event -> runProgram()));
            run.add(menuItem("Clear Output", null, event -> output.setText("")));

            JMenu help = new JMenu("Help");
            help.add(menuItem("Keywords", null, event -> showKeywords()));
            help.add(menuItem("About Lumi", null, event -> JOptionPane.showMessageDialog(
                    window,
                    "Lumi IDE " + VERSION + "\nA small programming language written in Java.",
                    "About Lumi",
                    JOptionPane.INFORMATION_MESSAGE)));

            bar.add(file);
            bar.add(run);
            bar.add(help);
            return bar;
        }

        private JMenuItem menuItem(
                String text,
                String accelerator,
                java.awt.event.ActionListener listener) {
            JMenuItem item = new JMenuItem(text);
            if (accelerator != null) item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
            item.addActionListener(listener);
            return item;
        }

        private JToolBar createToolbar() {
            JToolBar toolbar = new JToolBar();
            toolbar.setFloatable(false);
            toolbar.add(toolbarButton("New", event -> newFile()));
            toolbar.add(toolbarButton("Open", event -> openFile()));
            toolbar.add(toolbarButton("Save", event -> save()));
            toolbar.addSeparator();
            toolbar.add(toolbarButton("▶ Run", event -> runProgram()));
            return toolbar;
        }

        private JButton toolbarButton(
                String text, java.awt.event.ActionListener listener) {
            JButton button = new JButton(text);
            button.addActionListener(listener);
            return button;
        }

        private void markDirty() {
            dirty = true;
            updateTitle();
        }

        private void updateTitle() {
            String name = currentFile == null ? "Untitled.lumi" : currentFile.getFileName().toString();
            window.setTitle((dirty ? "* " : "") + name + " — Lumi IDE");
        }

        private boolean confirmDiscardChanges() {
            if (!dirty) return true;
            int choice = JOptionPane.showConfirmDialog(
                    window,
                    "Save changes before continuing?",
                    "Unsaved Lumi program",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return false;
            }
            return choice != JOptionPane.YES_OPTION || save();
        }

        private void newFile() {
            if (!confirmDiscardChanges()) return;
            currentFile = null;
            editor.setText("# New Lumi program\n");
            dirty = false;
            output.setText("");
            status.setText("New program");
            updateTitle();
        }

        private void openFile() {
            if (!confirmDiscardChanges()) return;
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Open Lumi program");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Lumi programs (*.lumi)", "lumi"));
            if (chooser.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) return;

            try {
                currentFile = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
                editor.setText(Files.readString(currentFile));
                editor.setCaretPosition(0);
                dirty = false;
                status.setText("Opened " + currentFile);
                updateTitle();
            } catch (java.io.IOException error) {
                showError("Could not open file", error);
            }
        }

        private boolean save() {
            if (currentFile == null) return saveAs();
            try {
                Files.writeString(currentFile, editor.getText());
                dirty = false;
                status.setText("Saved " + currentFile);
                updateTitle();
                return true;
            } catch (java.io.IOException error) {
                showError("Could not save file", error);
                return false;
            }
        }

        private boolean saveAs() {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Lumi program");
            chooser.setSelectedFile(new java.io.File(
                    currentFile == null ? "program.lumi" : currentFile.getFileName().toString()));
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Lumi programs (*.lumi)", "lumi"));
            if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return false;

            Path selected = chooser.getSelectedFile().toPath();
            if (!selected.toString().toLowerCase().endsWith(".lumi")) {
                selected = Path.of(selected + ".lumi");
            }
            currentFile = selected.toAbsolutePath().normalize();
            return save();
        }

        private void runProgram() {
            if (!save()) return;
            output.setText("");
            status.setText("Running " + currentFile.getFileName() + "…");

            new SwingWorker<Integer, String>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    String executable = System.getProperty("os.name").toLowerCase().contains("win")
                            ? "java.exe"
                            : "java";
                    Path java = Path.of(System.getProperty("java.home"), "bin", executable);
                    ProcessBuilder builder = new ProcessBuilder(
                            java.toString(),
                            "-cp",
                            System.getProperty("java.class.path"),
                            Lumi.class.getName(),
                            currentFile.toString());
                    builder.directory(currentFile.getParent().toFile());
                    builder.redirectErrorStream(true);
                    Process process = builder.start();
                    try (BufferedReader reader = process.inputReader()) {
                        String line;
                        while ((line = reader.readLine()) != null) publish(line + "\n");
                    }
                    return process.waitFor();
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String chunk : chunks) output.append(chunk);
                    output.setCaretPosition(output.getDocument().getLength());
                }

                @Override
                protected void done() {
                    try {
                        int code = get();
                        status.setText(code == 0 ? "Finished successfully" : "Exited with code " + code);
                    } catch (Exception error) {
                        status.setText("Run failed");
                        showError("Could not run program", error);
                    }
                }
            }.execute();
        }

        private void showKeywords() {
            JOptionPane.showMessageDialog(
                    window,
                    """
                    print  let  var  if  then  else  end  import
                    System.sleep  System.findval  System.ctor
                    frame  Label  LButton  textB  Panel
                    file.create  key.listen  input  notify
                    """,
                    "Lumi keywords",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        private void showError(String title, Exception error) {
            JOptionPane.showMessageDialog(
                    window,
                    error.getMessage(),
                    title,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class ArithmeticParser {
        private final String input;
        private final Map<String, Object> locals;
        private int position;

        ArithmeticParser(String input, Map<String, Object> locals) {
            this.input = input;
            this.locals = locals;
        }

        long parse() {
            long result = expression();
            skipSpaces();
            if (position != input.length()) {
                throw new IllegalArgumentException(
                        "Unexpected arithmetic near: " + input.substring(position));
            }
            return result;
        }

        private long expression() {
            long result = term();
            while (true) {
                skipSpaces();
                if (take('+')) result += term();
                else if (take('-')) result -= term();
                else return result;
            }
        }

        private long term() {
            long result = factor();
            while (true) {
                skipSpaces();
                if (take('*')) result *= factor();
                else if (take('/')) result /= factor();
                else return result;
            }
        }

        private long factor() {
            skipSpaces();
            if (take('-')) return -factor();
            if (take('(')) {
                long result = expression();
                skipSpaces();
                if (!take(')')) throw new IllegalArgumentException("Missing )");
                return result;
            }

            int start = position;
            while (position < input.length()
                    && (Character.isLetterOrDigit(input.charAt(position))
                    || input.charAt(position) == '_')) {
                position++;
            }
            if (start == position) {
                throw new IllegalArgumentException("Expected a number or variable");
            }

            String token = input.substring(start, position);
            if (token.matches("\\d+")) return Long.parseLong(token);
            Object value = findVariable(token, locals);
            if (value instanceof Number number) return number.longValue();
            throw new IllegalArgumentException(token + " is not a number");
        }

        private boolean take(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }
    }
}
