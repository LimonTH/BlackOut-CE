package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

public class ShaderReader {
    private static final String GLSL_VERSION = "150";
    private static final Map<UnBuilt, String> shaders = new HashMap<>();
    private static final Map<String, String> replacements = new HashMap<>();

    static {
        put(
                "ver",
                "#version " + GLSL_VERSION,
                "matrices",
                "uniform mat4 ModelViewMat;\nuniform mat4 ProjMat;",
                "posclr",
                "in vec3 Position;\nin vec4 Color;",
                "posuv",
                "in vec3 Position;\nin vec2 UV0;",
                "pi",
                "3.14159265358979323846",
                "e",
                "2.7182818284590452354",
                "alpha",
                "uniform float uAlpha;",
                "res",
                "uniform vec2 uResolution;"
        );
    }

    private static void put(String... strings) {
        for (int i = 0; i < strings.length; i += 2) {
            replacements.put(strings[i], strings[i + 1]);
        }
    }

    public static int create(String name) {
        String[] s = getShaders(name);

        int fragId = GL20C.glCreateShader(35632);
        int vertId = GL20C.glCreateShader(35633);
        GL20C.glShaderSource(fragId, s[0]);
        GL20C.glShaderSource(vertId, s[1]);
        GL20C.glCompileShader(vertId);
        checkShaderCompilation(vertId, "vertex", name);
        GL20C.glCompileShader(fragId);
        checkShaderCompilation(fragId, "fragment", name);
        int id = GL20C.glCreateProgram();
        GL20C.glAttachShader(id, fragId);
        GL20C.glAttachShader(id, vertId);
        GL20C.glLinkProgram(id);
        GL20C.glDetachShader(id, vertId);
        GL20C.glDetachShader(id, fragId);
        GL20C.glDeleteShader(vertId);
        GL20C.glDeleteShader(fragId);
        GL20C.glValidateProgram(id);
        return id;
    }

    private static void checkShaderCompilation(int shaderId, String type, String name) {
        if (GL20C.glGetShaderi(shaderId, GL20C.GL_COMPILE_STATUS) == 0) {
            String log = GL20C.glGetShaderInfoLog(shaderId);
            System.err.println("Shader compilation error in " + type + " shader '" + name + "': " + log);
        }
    }

    private static String[] getShaders(String shaderName) {
        String frag = "";
        String vert = "";
        String string = readStreamToString(FileUtils.getResourceStream("shader", "shaders.blackout"));

        for (String line : string.lines().toList()) {
            if (line.startsWith(shaderName + ":")) {
                String[] paths = line.substring(shaderName.length() + 2).split(", ");
                frag = paths[0];
                vert = paths[1];
                break;
            }
        }

        return getShaders(frag, vert);
    }

    private static String[] getShaders(String frag, String vert) {
        AtomicReference<String> fs = new AtomicReference<>();
        AtomicReference<String> vs = new AtomicReference<>();
        shaders.forEach((unBuilt, string) -> {
            String str = unBuilt.file + "." + unBuilt.name;
            if (str.equals(frag)) {
                fs.set(string);
            }

            if (str.equals(vert)) {
                vs.set(string);
            }
        });
        return new String[]{fs.get(), vs.get()};
    }

    private static String modify(String original, int i) {
        for (Entry<String, String> entry : replacements.entrySet()) {
            original = original.replace("$" + entry.getKey(), entry.getValue());
        }

        return i <= 0 ? original : modify(original, --i);
    }

    private static String readStreamToString(InputStream inputStream) {
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadShaders() {
        List<UnBuilt> unBuilts = new ArrayList<>();

        for (String file : readStreamToString(FileUtils.getResourceStream("shader", "load.blackout")).lines().toList()) {
            try (InputStream stream = FileUtils.getResourceStream("shader", "shaders", file + ".shader")) {
                unBuilts.addAll(readShaders(file, readStreamToString(stream)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        shaders.clear();
        unBuilts.forEach(s -> {
            processImports(s, unBuilts);
            shaders.put(s, s.build());
        });
    }

    private static void processImports(UnBuilt unBuilt, List<UnBuilt> list) {
        List<UnBuilt.ShaderMethod> imported = new ArrayList<>();
        unBuilt.imports.forEach(i -> {
            UnBuilt.ShaderMethod method = findMethod(i, list);
            if (method != null) {
                imported.addFirst(method);
            }
        });
        imported.forEach(unBuilt.methods::addFirst);
    }

    private static UnBuilt.ShaderMethod findMethod(String name, List<UnBuilt> list) {
        String[] parts = name.split("\\.");

        for (UnBuilt unBuilt : list) {
            if (unBuilt.file.equals(parts[0]) && unBuilt.name.equals(parts[1])) {
                for (UnBuilt.ShaderMethod method : unBuilt.methods) {
                    if (method.name.equals(parts[2])) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    private static List<UnBuilt> readShaders(String file, String content) {
        content = modify(content, 5);
        List<UnBuilt> shaders = new ArrayList<>();
        int inline = 0;
        BetterBuilder builder = new BetterBuilder();

        for (String c : content.split("")) {
            if (c.equals("{")) {
                inline++;
            }

            if (c.equals("}")) {
                inline--;
            }

            switch (builder.stage) {
                case 0:
                    if (c.equals("{") && inline == 1) {
                        builder.save("name");
                        builder.nextStage();
                        builder.reset();
                    } else if (c.equals("\n")) {
                        builder.reset();
                    } else if (!c.equals(" ") && inline == 0) {
                        builder.append(c);
                    }
                    break;
                case 1:
                    if (inline == 0) {
                        shaders.add(readShader(builder.get("name"), file, builder.get()));
                        builder.fullReset();
                    } else {
                        builder.append(c);
                    }
            }
        }

        return shaders;
    }

    private static UnBuilt readShader(String name, String file, String string) {
        List<UnBuilt.ShaderMethod> methods = readMethods(string);
        List<UnBuilt.ShaderField> fields = readFields(string);
        List<String> imports = readImports(string);
        return new UnBuilt(name, file, methods, fields, imports);
    }

    private static List<String> readImports(String string) {
        List<String> imports = new ArrayList<>();
        BetterBuilder builder = new BetterBuilder();

        for (String c : string.split("")) {
            switch (builder.stage) {
                case 0:
                    if (builder.is(c, "import")) {
                        builder.reset();
                        builder.nextStage();
                    } else if (c.equals("\n") || c.equals(" ")) {
                        builder.reset();
                    }
                    break;
                case 1:
                    if (c.equals(" ") || c.equals("\n")) {
                        builder.reset();
                    } else if (c.equals(";")) {
                        imports.add(builder.get());
                        builder.fullReset();
                    } else {
                        builder.append(c);
                    }
            }
        }

        return imports;
    }

    private static List<UnBuilt.ShaderMethod> readMethods(String string) {
        List<String> dividers = new ArrayList<>();
        dividers.add(" ");
        dividers.add("(");
        dividers.add(")");
        List<UnBuilt.ShaderMethod> methods = new ArrayList<>();
        int inline = 0;
        int methodInline = 0;
        boolean insideMethod = false;
        boolean isPre = false;
        BetterBuilder builder = new BetterBuilder();

        for (String c : string.split("")) {
            switch (builder.stage) {
                case 0:
                    String sus = builder.getA(c);
                    if (sus.equals("fun ") || sus.equals("@fun ")) {
                        isPre = sus.equals("@fun ");
                        builder.reset();
                        builder.nextStage();
                    } else if (c.equals("\n") || c.equals(" ")) {
                        builder.reset();
                    }
                    break;
                case 1:
                    if (dividers.contains(c)) {
                        builder.save("type");
                        builder.nextStage();
                        builder.reset();
                    } else {
                        builder.append(c);
                    }
                    break;
                case 2:
                    if (dividers.contains(c)) {
                        builder.save("name");
                        builder.nextStage();
                        builder.reset();
                    } else {
                        builder.append(c);
                    }
                    break;
                case 3:
                    if (c.equals(")")) {
                        builder.save("args");
                        builder.nextStage();
                        builder.reset();
                    } else {
                        builder.append(c);
                    }
                    break;
                case 4:
                    if (c.equals("{")) {
                        inline++;
                        if (!insideMethod) {
                            methodInline = inline;
                            builder.reset();
                            insideMethod = true;
                        } else {
                            builder.append(c);
                        }
                    } else if (c.equals("}")) {
                        if (--inline < methodInline && insideMethod) {
                            methods.add(new UnBuilt.ShaderMethod(builder.get("name"), builder.get("type"), builder.get("args"), builder.get(), isPre));
                            builder.fullReset();
                            insideMethod = false;
                            isPre = false;
                        } else {
                            builder.append(c);
                        }
                    } else {
                        builder.append(c);
                    }
            }
        }

        return methods;
    }

    private static List<UnBuilt.ShaderField> readFields(String string) {
        List<UnBuilt.ShaderField> fields = new ArrayList<>();
        List<String> prefixes = new ArrayList<>();
        prefixes.add("uniform");
        prefixes.add("out");
        prefixes.add("in");
        prefixes.add("const");
        boolean hasValue = false;
        BetterBuilder builder = new BetterBuilder();

        for (String c : string.split("")) {
            int i = builder.stage;
            switch (i) {
                case 0:
                    String str = builder.getA(c);
                    String prefix = str.substring(0, str.length() - 1);
                    if (prefixes.contains(prefix)) {
                        builder.save("prefix", prefix);
                        builder.reset();
                        builder.nextStage();
                    } else if (c.equals("\n") || c.equals(" ")) {
                        builder.reset();
                    }
                    break;
                case 1:
                    if (c.equals(" ")) {
                        builder.save("type");
                        builder.reset();
                        builder.nextStage();
                    } else {
                        builder.append(c);
                    }
                    break;
                case 2:
                    if (c.equals("=")) {
                        builder.save("name");
                        builder.reset();
                        hasValue = true;
                    } else if (c.equals(";")) {
                        String value;
                        String name;
                        if (hasValue) {
                            value = builder.get();
                            name = builder.get("name");
                        } else {
                            value = "";
                            name = builder.get();
                        }

                        fields.add(new UnBuilt.ShaderField(name, builder.get("type"), builder.get("prefix"), value));
                        hasValue = false;
                        builder.fullReset();
                    } else if (!c.equals(" ")) {
                        builder.append(c);
                    }
            }
        }

        return fields;
    }

    private static class BetterBuilder {
        private final StringBuilder stringBuilder = new StringBuilder();
        private final Map<String, String> saved = new HashMap<>();
        private int stage = 0;

        private void nextStage() {
            this.stage++;
        }

        private void resetStage() {
            this.stage = 0;
        }

        private BetterBuilder append(String string) {
            this.stringBuilder.append(string);
            return this;
        }

        private boolean is(String string) {
            return this.stringBuilder.toString().equals(string);
        }

        private boolean is(String string, String string2) {
            return this.append(string).get().equals(string2);
        }

        private void reset() {
            this.stringBuilder.delete(0, this.stringBuilder.length() + 1);
        }

        private void fullReset() {
            this.reset();
            this.saved.clear();
            this.resetStage();
        }

        private void save(String key, String value) {
            this.saved.put(key, value);
        }

        private void save(String key) {
            this.saved.put(key, this.get());
        }

        private String get(String key) {
            return this.saved.get(key);
        }

        private String get() {
            return this.stringBuilder.toString();
        }

        private String getA(String string) {
            return this.append(string).get();
        }
    }

    private record UnBuilt(
            String name, String file, List<ShaderMethod> methods, List<ShaderField> fields, List<String> imports
    ) {
        private String build() {
            StringBuilder builder = new StringBuilder();
            builder.append("#version").append(" ").append("150").append("\n");

            for (ShaderField uniform : this.fields) {
                builder.append(uniform.build()).append("\n");
            }

            for (ShaderMethod method : this.methods) {
                if (method.pre) {
                    builder.append(method.build()).append("\n");
                }
            }

            for (ShaderMethod methodx : this.methods) {
                if (!methodx.pre) {
                    builder.append(methodx.build()).append("\n");
                }
            }

            return builder.toString();
        }

        private record ShaderField(String name, String type, String string, String value) {
            private String build() {
                StringBuilder builder = new StringBuilder();
                if (this.string != null) {
                    builder.append(this.string).append(" ");
                }

                builder.append(this.type).append(" ").append(this.name);
                if (!this.value.isEmpty()) {
                    builder.append(" = ").append(this.value);
                }

                return builder.append(";").toString();
            }
        }

        private record ShaderMethod(String name, String type, String args, String content, boolean pre) {
            private String build() {
                return this.type + " " +
                        this.name + "(" + this.args + ") {" +
                        this.content + "}";
            }
        }
    }
}
