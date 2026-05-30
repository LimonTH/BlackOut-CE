frag {
    import utils.rainbow.hslWave;

    $alpha

    in vec4 vertexColor;
    uniform float time;
    uniform float alp;

    out vec4 fragColor;

    fun void main() {
        vec4 color = vertexColor;
        if (color.a == 0.0) {
            discard;
        }

        float pos = gl_FragCoord.x / 1000.0 - gl_FragCoord.y / 1000.0;
        float x = 2.0 + fract(pos - time / 10.0) * 6.0;
        vec3 rgb = hslWave(x, 1.0);

        fragColor = vec4(rgb, alp * uAlpha);
    }
}
