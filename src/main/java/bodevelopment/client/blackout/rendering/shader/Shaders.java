package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.util.Capes;

import java.util.Arrays;

public class Shaders {
    public static Shader rainbow;
    public static Shader menu;
    public static Shader picker;
    public static Shader font;
    public static Shader fontshadow;
    public static Shader texture;
    public static Shader blur;
    public static Shader color;
    public static Shader screenblur;
    public static Shader glowesp;
    public static Shader outlineesp;
    public static Shader screentex;
    public static Shader rounded;
    public static Shader roundedshadow;
    public static Shader roundedfade;
    public static Shader shadowfade;
    public static Shader tenacity;
    public static Shader tenacityshadow;
    public static Shader roundedrainbow;
    public static Shader shadowrainbow;
    public static Shader gradientfont;
    public static Shader bloom;
    public static Shader textureUV;
    public static Shader blurUV;
    public static Shader smoke;
    public static Shader skeet;
    public static Shader shaderbloom;
    public static Shader bloomblur;
    public static Shader screentexcolor;
    public static Shader screentexoverlay;
    public static Shader fontwave;
    public static Shader convert;
    public static Shader subtract;

    public static void loadPrograms() {
        ShaderReader.loadShaders();
        Arrays.stream(Shaders.class.getDeclaredFields()).forEach(field -> {
            try {
                field.set(field, newShader(field.getName()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        Capes.requestCapes();
    }

    private static Shader newShader(String name) {
        return new Shader(name);
    }
}
