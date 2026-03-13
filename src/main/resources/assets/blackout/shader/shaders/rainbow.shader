frag {
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

        float x = 2.0 + fract(pos - time / 10) * 6.0;

        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 -(clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, 0.0, 1.0)) + 1.0;

        fragColor = vec4(r, g, b, alp * uAlpha);
    }
}
