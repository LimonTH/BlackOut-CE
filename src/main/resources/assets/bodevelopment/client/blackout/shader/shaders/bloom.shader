frag {
    $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec4 clr;

    out vec4 fragColor;

    fun void main() {
        float a = 1 - length((realPos.xy - pos.xy) / pos.zw);
        fragColor = vec4(clr.rgb, clr.a * a * uAlpha);
    }
}

shader {
    $alpha
    $res

    uniform sampler2D uTexture;
    uniform vec4 clr;

    out vec4 fragColor;

    fun void main() {
        vec4 c = clr;
        c.a *= uAlpha * texture(uTexture, gl_FragCoord.xy / uResolution.xy).r;
        fragColor = c;
    }
}

bloomblur {
    import screenblur.frag.getColor;

    $res

    uniform sampler2D uTexture;
    uniform float dist;

    out vec4 fragColor;

    fun vec4 getBlurColor() {
        vec4 total = vec4(0);

        total += getColor(0, 0);
        total += getColor(1, 0) * 0.5;
        total += getColor(-1, 0) * 0.5;
        total += getColor(0, 1) * 0.5;
        total += getColor(0, -1) * 0.5;

        total += getColor(1, 1) * 0.3;
        total += getColor(-1, -1) * 0.3;
        total += getColor(-1, 1) * 0.3;
        total += getColor(1, -1) * 0.3;

        vec4 c = total / 4.2;
        c.r = -c.r * c.r + 2 * c.r;
        return c;
    }

    fun void main() {
        fragColor = getBlurColor();
    }
}

convert {
    $res

    uniform sampler2D uTexture;

    out vec4 fragColor;

    fun void main() {
        float a = texture(uTexture, gl_FragCoord.xy / uResolution.xy).a;
        if (a > 0.0) a = 1.0;
        fragColor = vec4(vec3(a), 1.0);
    }
}

convert2 {
    $res

    uniform sampler2D uTexture;

    out vec4 fragColor;

    fun void main() {
        fragColor = vec4(vec3(texture(uTexture, gl_FragCoord.xy / uResolution.xy).a), 1.0);
    }
}

subtract {
    $res

    uniform sampler2D uTexture0;
    uniform sampler2D uTexture1;

    out vec4 fragColor;

    fun void main() {
        float r1 = texture(uTexture0, gl_FragCoord.xy / uResolution.xy).r;
        float r2 = texture(uTexture1, gl_FragCoord.xy / uResolution.xy).r;
        fragColor = vec4(vec3(max(r1 - r2, 0.0)), 1.0);
    }
}
