package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads, preprocesses, and compiles GLSL shader programs defined in
 * the custom {@code .shader} DSL.
 *
 * <h3>DSL Reference</h3>
 * <pre>
 * name {
 *     $macro           // built-in macro expansion
 *     import a.b.c;    // import method c from shader a::b
 *     fun Type name(args) { body }   // regular function
 *    {@code @}fun Type name(args) { body }  // pre-declared function (placed before others)
 *     uniform / in / out / const ... // passed through as-is
 * }
 * </pre>
 */
public final class ShaderReader {

    private static final int GLSL_VERSION = 150;
    private static final String VERSION_DIRECTIVE = "#version " + GLSL_VERSION + "\n";

    /** Macro name → GLSL replacement text. */
    private static final Map<String, String> MACROS = new LinkedHashMap<>();

    static {
        // @formatter:off
        MACROS.put("ver",       VERSION_DIRECTIVE.trim());
        MACROS.put("matrices",  "uniform mat4 ModelViewMat;\nuniform mat4 ProjMat;");
        MACROS.put("posclr",    "in vec3 Position;\nin vec4 Color;");
        MACROS.put("posuv",     "in vec3 Position;\nin vec2 UV0;");
        MACROS.put("pi",        "3.14159265358979323846");
        MACROS.put("e",         "2.7182818284590452354");
        MACROS.put("alpha",     "uniform float uAlpha;");
        MACROS.put("res",       "uniform vec2 uResolution;");
        // @formatter:on
    }

    /**
     * Parses all {@code .shader} files, expands macros/imports,
     * and stores the resulting GLSL programs in the internal cache.
     */
    public static void loadAll() {
        List<String> fileNames = readLines("shader/load.blackout");
        List<ShaderUnit> units = new ArrayList<>();

        for (String fileName : fileNames) {
            String raw = readResource("shader/shaders/" + fileName + ".shader");
            units.addAll(parseFile(fileName, raw));
        }

        for (ShaderUnit unit : units) {
            resolveImports(unit, units);
        }

        ShaderRegistry.clear();
        for (ShaderUnit unit : units) {
            ShaderRegistry.put(fileKey(unit.file, unit.name), unit.build());
        }
    }

    public static int create(String programName) {
        String spec = readResource("shader/shaders.blackout");
        String fragKey = null;
        String vertKey = null;

        for (String line : spec.lines().toList()) {
            if (line.startsWith(programName + ":")) {
                String[] parts = line.substring(programName.length() + 2).split(", ");
                fragKey = parts[0];
                vertKey = parts[1];
                break;
            }
        }

        if (fragKey == null || vertKey == null) {
            throw new IllegalArgumentException("Unknown shader program: " + programName);
        }

        String fragSource = ShaderRegistry.get(fragKey);
        String vertSource = ShaderRegistry.get(vertKey);
        if (fragSource == null) throw new IllegalArgumentException("Fragment shader not found: " + fragKey);
        if (vertSource == null) throw new IllegalArgumentException("Vertex shader not found: " + vertKey);

        return compileAndLink(fragSource, vertSource, programName);
    }

    private static List<ShaderUnit> parseFile(String fileName, String content) {
        content = expandMacros(content);
        List<ShaderUnit> units = new ArrayList<>();
        List<String> lines = content.lines().toList();

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).strip();
            if (isBlockStart(line)) {
                String name = line.substring(0, line.length() - 2).strip(); // remove " {"
                StringBuilder body = new StringBuilder();
                int depth = 1;
                i++;
                while (i < lines.size() && depth > 0) {
                    String inner = lines.get(i);
                    for (int ci = 0; ci < inner.length(); ci++) {
                        if (inner.charAt(ci) == '{') depth++;
                        else if (inner.charAt(ci) == '}') depth--;
                    }
                    if (depth > 0 || !inner.strip().equals("}")) {
                        if (body.length() > 0) body.append('\n');
                        body.append(inner);
                    }
                    i++;
                }
                units.add(parseUnit(name, fileName, body.toString()));
            }
            i++;
        }
        return units;
    }

    private static boolean isBlockStart(String line) {
        if (line.isEmpty() || !line.endsWith(" {")) return false;
        String name = line.substring(0, line.length() - 2).strip();
        return !name.startsWith("fun ") && !name.startsWith("@fun ") && !name.startsWith("import ");
    }

    private static ShaderUnit parseUnit(String name, String file, String body) {
        List<Function> functions = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        List<String> imports = new ArrayList<>();

        List<String> lines = body.lines().toList();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) { i++; continue; }

            if (line.startsWith("import ")) {
                imports.add(extractImport(line));
                i++;
            } else if (line.startsWith("fun ") || line.startsWith("@fun ")) {
                boolean pre = line.startsWith("@fun ");
                String decl = line;
                StringBuilder funcBody = new StringBuilder();
                int depth = 0;

                for (int ci = 0; ci < lines.get(i).length(); ci++) {
                    if (lines.get(i).charAt(ci) == '{') depth++;
                    else if (lines.get(i).charAt(ci) == '}') depth--;
                }
                if (depth == 0) {
                    i++;
                    if (i < lines.size()) {
                        for (int ci = 0; ci < lines.get(i).length(); ci++) {
                            if (lines.get(i).charAt(ci) == '{') depth++;
                            else if (lines.get(i).charAt(ci) == '}') depth--;
                        }
                    }
                }
                while (i < lines.size() && depth > 0) {
                    i++;
                    if (i >= lines.size()) break;
                    String inner = lines.get(i);
                    for (int ci = 0; ci < inner.length(); ci++) {
                        if (inner.charAt(ci) == '{') depth++;
                        else if (inner.charAt(ci) == '}') depth--;
                    }
                    if (funcBody.length() > 0) funcBody.append('\n');
                    funcBody.append(inner);
                }

                String fullSig = decl + "\n" + funcBody;
                functions.add(parseFunction(fullSig, pre));
                i++;
            } else if (line.startsWith("uniform ") || line.startsWith("in ") ||
                       line.startsWith("out ") || line.startsWith("const ")) {
                fields.add(parseField(line));
                i++;
            } else {
                fields.add(new Field("", "", "", line));
                i++;
            }
        }

        return new ShaderUnit(name, file, functions, fields, imports);
    }

    private static final Pattern IMPORT_PAT = Pattern.compile("import\\s+([\\w.]+)\\s*;");

    private static String extractImport(String line) {
        Matcher m = IMPORT_PAT.matcher(line);
        return m.find() ? m.group(1) : "";
    }

    private static Function parseFunction(String content, boolean pre) {
        String stripped = content.replaceFirst("@?fun\\s+", "");
        int parenIdx = stripped.indexOf('(');
        if (parenIdx < 0) return new Function("?", "void", "", "", pre);

        String beforeParen = stripped.substring(0, parenIdx).strip();
        int lastSpace = beforeParen.lastIndexOf(' ');
        String returnType = lastSpace >= 0 ? beforeParen.substring(0, lastSpace).strip() : "void";
        String funcName = lastSpace >= 0 ? beforeParen.substring(lastSpace + 1).strip() : beforeParen;

        int closeParen = stripped.indexOf(')');
        String args = (closeParen > parenIdx) ? stripped.substring(parenIdx + 1, closeParen).strip() : "";

        int openBrace = stripped.indexOf('{');
        int closeBrace = findMatchingBrace(stripped, openBrace);
        String funcBody = (openBrace >= 0 && closeBrace > openBrace)
                ? stripped.substring(openBrace + 1, closeBrace).strip()
                : stripped.substring(closeParen + 1).strip();

        return new Function(funcName, returnType, args, funcBody, pre);
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static Field parseField(String line) {
        line = line.strip();
        // Remove trailing semicolon for easier parsing
        String value = "";
        int semiIdx = line.indexOf(';');
        String body = semiIdx >= 0 ? line.substring(0, semiIdx).strip() : line;

        String[] parts = body.split("\\s+", 3);
        String prefix = parts[0];                              // uniform / in / out / const
        String type = parts.length > 1 ? parts[1] : "";
        String rest = parts.length > 2 ? parts[2] : "";

        String name;
        int eqIdx = rest.indexOf('=');
        if (eqIdx >= 0) {
            name = rest.substring(0, eqIdx).strip();
            value = rest.substring(eqIdx + 1).strip();
        } else {
            name = rest;
        }
        return new Field(name, type, prefix, value);
    }

    private static String expandMacros(String source) {
        for (Map.Entry<String, String> entry : MACROS.entrySet()) {
            source = source.replace("$" + entry.getKey(), entry.getValue());
        }
        return source;
    }

    private static void resolveImports(ShaderUnit unit, List<ShaderUnit> allUnits) {
        List<Function> imported = new ArrayList<>();
        for (String importRef : unit.imports) {
            String[] parts = importRef.split("\\.");
            if (parts.length >= 3) {
                String targetKey = fileKey(parts[0], parts[1]);
                for (ShaderUnit other : allUnits) {
                    if (fileKey(other.file, other.name).equals(targetKey)) {
                        for (Function f : other.functions) {
                            if (f.name.equals(parts[2])) {
                                imported.add(f);
                            }
                        }
                    }
                }
            }
        }
        unit.functions.addAll(0, imported);
    }

    private static String fileKey(String file, String name) {
        return file + "." + name;
    }

    private static int compileAndLink(String fragSource, String vertSource, String name) {
        int fragId = compileShader(GL20C.GL_FRAGMENT_SHADER, fragSource, "fragment", name);
        int vertId = compileShader(GL20C.GL_VERTEX_SHADER, vertSource, "vertex", name);

        int programId = GL20C.glCreateProgram();
        GL20C.glAttachShader(programId, fragId);
        GL20C.glAttachShader(programId, vertId);
        GL20C.glLinkProgram(programId);
        GL20C.glDetachShader(programId, vertId);
        GL20C.glDetachShader(programId, fragId);
        GL20C.glDeleteShader(vertId);
        GL20C.glDeleteShader(fragId);
        GL20C.glValidateProgram(programId);
        return programId;
    }

    private static int compileShader(int type, String source, String typeName, String programName) {
        int id = GL20C.glCreateShader(type);
        GL20C.glShaderSource(id, source);
        GL20C.glCompileShader(id);
        if (GL20C.glGetShaderi(id, GL20C.GL_COMPILE_STATUS) == 0) {
            String log = GL20C.glGetShaderInfoLog(id);
            System.err.println("Shader compilation error in " + typeName + " shader '" + programName + "': " + log);
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream is = FileUtils.getResourceStream(path.split("/"))) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }

    private static List<String> readLines(String path) {
        return readResource(path).lines()
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .toList();
    }

    private static class ShaderUnit {
        final String name;
        final String file;
        final List<Function> functions;
        final List<Field> fields;
        final List<String> imports;

        ShaderUnit(String name, String file, List<Function> functions,
                   List<Field> fields, List<String> imports) {
            this.name = name;
            this.file = file;
            this.functions = functions;
            this.fields = fields;
            this.imports = imports;
        }

        String build() {
            StringBuilder sb = new StringBuilder(VERSION_DIRECTIVE);

            for (Field f : fields) {
                sb.append(f.build()).append('\n');
            }
            for (Function f : functions) {
                if (f.pre) sb.append(f.build()).append('\n');
            }
            for (Function f : functions) {
                if (!f.pre) sb.append(f.build()).append('\n');
            }
            return sb.toString();
        }
    }

    private record Function(String name, String returnType, String args, String body, boolean pre) {
        String build() {
            return returnType + " " + name + "(" + args + ") {\n" + body + "\n}";
        }
    }

    private record Field(String name, String type, String prefix, String raw) {
        String build() {
            // 'raw' is either a full pass-through line (for unrecognized GLSL)
            // or an initializer value for parsed fields.
            boolean isPassThrough = name.isEmpty() && type.isEmpty() && prefix.isEmpty();
            if (isPassThrough) return raw;

            StringBuilder sb = new StringBuilder();
            if (!prefix.isEmpty()) sb.append(prefix).append(' ');
            if (!type.isEmpty()) sb.append(type).append(' ');
            sb.append(name);
            if (raw != null && !raw.isEmpty()) sb.append(" = ").append(raw);
            return sb.append(';').toString();
        }
    }

    /** Simple registry holding compiled GLSL source strings, keyed by "file.name". */
    private static final class ShaderRegistry {
        private static final Map<String, String> store = new LinkedHashMap<>();

        static void clear() { store.clear(); }
        static void put(String key, String source) { store.put(key, source); }
        static String get(String key) { return store.get(key); }
    }
}
