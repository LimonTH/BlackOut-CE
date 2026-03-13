frag {
    $res

    uniform float alpha;
    uniform sampler2D uTexture;

    out vec4 fragColor;

    fun void main() {
        vec4 clr = texture(uTexture, gl_FragCoord.xy / uResolution.xy);
        clr.a *= alpha;
        fragColor = clr;
    }
}


color {
    $res

    uniform sampler2D uTexture;
    uniform vec4 clr;

    out vec4 fragColor;

    fun void main() {
        fragColor = texture(uTexture, gl_FragCoord.xy / uResolution.xy) * clr;
    }
}

overlay {
    $alpha
    $res

    uniform sampler2D uTexture0;
    uniform sampler2D uTexture1;
    uniform vec4 clr;

    out vec4 fragColor;

    fun void main() {
        vec2 texPos = gl_FragCoord.xy / uResolution.xy;
        vec4 c = texture(uTexture1, texPos);
        c.a *= texture(uTexture0, texPos).r;
        c.a *= uAlpha;
        fragColor = c;
    }
}
