frag {
    import font.utils.getAlpha;

    $alpha

    uniform sampler2D uTexture;
    uniform vec2 texRes;

    uniform vec4 clr;

    in vec2 texCoord0;

    out vec4 fragColor;

    fun void main() {
        fragColor = vec4(1.0, 1.0, 1.0, getAlpha() * uAlpha) * clr;
    }
}

shadow {
    import font.utils.getAlpha;

    $alpha

    uniform sampler2D uTexture;
    uniform vec2 texRes;

    in vec2 texCoord0;

    uniform float alphaMulti;

    out vec4 fragColor;

    fun void main() {
        fragColor = vec4(0.0, 0.0, 0.0, getAlpha() * 0.4 * uAlpha * alphaMulti);
    }
}

grad {
    import font.utils.getAlpha;

    $alpha
    $res

    uniform sampler2D uTexture;
    uniform vec2 texRes;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform float saturation;

    in vec2 texCoord0;

    out vec4 fragColor;

    fun void main() {
        float a = getAlpha();
        if (a == 0.0) {
            discard;
        }

        float pos = (gl_FragCoord.x / uResolution.x - gl_FragCoord.y / uResolution.y) * frequency;

        float x = 2.0 + fract(pos - time * speed) * 6.2831;

        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 - (clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, .0, 1.0)) + 1.0;
        fragColor = vec4(1 - r * saturation, 1 - g * saturation, 1 - b * saturation, a * uAlpha);
    }
}

wave {
    import font.utils.getAlpha;

    $alpha
    $res

    uniform sampler2D uTexture;
    uniform vec2 texRes;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform vec4 clr1;
    uniform vec4 clr2;

    in vec2 texCoord0;
    out vec4 fragColor;

    fun void main() {
        float a = getAlpha();
        if (a == 0.0) {
            discard;
        }

        vec2 position = gl_FragCoord.xy / uResolution.xy * frequency;
        float d = (sin(position.y * 2.0 - position.x + time * speed) + 1.0) / 2.0;

        vec4 c = mix(clr1, clr2, d);
        c.a *= a * uAlpha;
        fragColor = c;
    }
}


utils {
    fun float getAlpha() {
        return texture(uTexture, texCoord0).r;
    }
}

shadowvert {
    $posuv
    $matrices

    out vec2 texCoord0;

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0) + vec4(0.0015, -0.0015, 0.0, 0.0);

        texCoord0 = UV0;
    }
}
