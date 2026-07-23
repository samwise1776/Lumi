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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Lumi {
    private static final Map<String, Object> variables = new HashMap<>();
    private static final Map<String, LumiClass> classes = new HashMap<>();
    private static final Map<String, LumiButton> buttons = new HashMap<>();
    private static final Map<String, JLabel> labels = new HashMap<>();
    private static final Map<String, JTextField> textBoxes = new HashMap<>();
    private static JFrame frame;
    private static String frameTitle = "Lumi";
    private static int frameWidth = 700;
    private static int frameHeight = 900;
    private static Path scriptDirectory = Path.of("").toAbsolutePath();
    private static Path frameIconPath;

    private record LumiMethod(String parameter, List<String> body) {}
    private record LumiButton(String text, JButton component) {}

    private static final class LumiClass {
        private final Map<String, LumiMethod> methods = new LinkedHashMap<>();
        private List<String> constructorBody = List.of();
    }

    public static void main(String[] args) throws Exception {
        Path file;

        if (args.length == 0 && System.getenv("FLATPAK_ID") != null) {
            file = chooseLumiFile();
            if (file == null) return;
        } else if (args.length == 1) {
            file = Path.of(args[0]);
        } else if (args.length == 2 && args[0].equals("run")) {
            file = Path.of(args[1]);
        } else {
            System.out.println("Run a Lumi program:");
            System.out.println("  lumi <file.lumi>");
            System.out.println("  lumi run <file.lumi>");
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
        executeLines(Files.readAllLines(file), new HashMap<>());
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

            Matcher condition = Pattern.compile("if\\s+(.+?)\\s+then").matcher(line);
            if (condition.matches()) {
                ConditionalBlock block = readConditional(lines, index);
                List<String> selected = evaluateCondition(condition.group(1), locals)
                        ? block.whenTrue()
                        : block.whenFalse();
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

        Matcher frameCreate = Pattern.compile("frame\\.create\\(\"(.*)\"\\)\\s*;?")
                .matcher(line);
        if (frameCreate.matches()) {
            createFrame(frameCreate.group(1));
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
                .compile("Label\\.create\\(\"(.*)\"\\)\\.varname\\(([A-Za-z_]\\w*)\\)\\s*;?")
                .matcher(line);
        if (labelCreate.matches()) {
            createLabel(labelCreate.group(2), labelCreate.group(1));
            return;
        }

        Matcher textBoxCreate = Pattern
                .compile("textB\\s+([A-Za-z_]\\w*)\\s*=\\s*\"(.*)\"\\s*;?")
                .matcher(line);
        if (textBoxCreate.matches()) {
            createTextBox(textBoxCreate.group(1), textBoxCreate.group(2));
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

        Matcher sleep = Pattern.compile("System\\.sleep\\((\\d+)\\)\\s*;?").matcher(line);
        if (sleep.matches()) {
            Thread.sleep(Long.parseLong(sleep.group(1)));
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

        System.out.println("Unknown instruction: " + line);
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
        return "<undefined:" + name + ">";
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

    private record ConditionalBlock(
            List<String> whenTrue, List<String> whenFalse, int endIndex) {}

    private static ConditionalBlock readConditional(List<String> lines, int startIndex) {
        List<String> whenTrue = new ArrayList<>();
        List<String> whenFalse = new ArrayList<>();
        List<String> current = whenTrue;
        int depth = 1;

        for (int index = startIndex + 1; index < lines.size(); index++) {
            String line = clean(lines.get(index));
            if (line.matches("if\\s+.+\\s+then")) {
                depth++;
            } else if (line.equals("end")) {
                depth--;
                if (depth == 0) {
                    return new ConditionalBlock(whenTrue, whenFalse, index);
                }
            } else if (line.equals("else") && depth == 1) {
                current = whenFalse;
                continue;
            }
            current.add(lines.get(index));
        }

        throw new IllegalArgumentException(
                "Missing end for if statement on line " + (startIndex + 1));
    }

    private static void createFrame(String title) throws Exception {
        frameTitle = title;
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) frame.dispose();
            frame = new JFrame(frameTitle);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(frameWidth, frameHeight);
            frame.setLayout(new java.awt.FlowLayout());
            if (frameIconPath != null) {
                frame.setIconImage(new ImageIcon(frameIconPath.toString()).getImage());
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

    private static void createButton(String name, String text) {
        JButton component = GraphicsEnvironment.isHeadless() ? null : new JButton(text);
        buttons.put(name, new LumiButton(text, component));
        if (frame != null && component != null) {
            frame.add(component);
            frame.revalidate();
        }
    }

    private static void addButtonAction(String name, List<String> body) {
        LumiButton button = buttons.get(name);
        if (button == null) {
            System.out.println("Unknown button: " + name);
            return;
        }
        if (button.component() != null) {
            button.component().addActionListener(event -> {
                try {
                    executeLines(body, new HashMap<>());
                } catch (Exception error) {
                    error.printStackTrace();
                }
            });
        }
    }

    private static void createLabel(String name, String text) {
        JLabel label = new JLabel(text);
        labels.put(name, label);
        if (frame != null) {
            frame.add(label);
            frame.revalidate();
            frame.repaint();
        }
    }

    private static void createTextBox(String name, String initialText) {
        JTextField box = new JTextField(initialText, 20);
        textBoxes.put(name, box);
        if (frame != null) {
            frame.add(box);
            frame.revalidate();
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
