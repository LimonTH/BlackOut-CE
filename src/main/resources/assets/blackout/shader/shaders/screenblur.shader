frag {
    $alpha
    $res

    uniform sampler2D uTexture;
    uniform vec4 clr;
    uniform float dist;

    out vec4 fragColor;

    fun vec4 getColor(float x, float y) {
        vec2 v = (gl_FragCoord.xy + vec2(x, y) * dist) / uResolution.xy;
        return texture(uTexture, v);
    }

    fun void main() {
        vec4 total = vec4(0);
        vec4 middle = getColor(0, 0) * 0.23809523809; // 1 / 4.2
        total += middle;

        total += getColor(1, 0) * 0.11904761904; // 0.5 / 4.2
        total += getColor(-1, 0) * 0.11904761904; // 0.5 / 4.2
        total += getColor(0, 1) * 0.11904761904; // 0.5 / 4.2
        total += getColor(0, -1) * 0.11904761904; // 0.5 / 4.2

        total += getColor(1, 1) * 0.07142857142; // 0.3 / 4.2
        total += getColor(-1, -1) * 0.07142857142; // 0.3 / 4.2
        total += getColor(-1, 1) * 0.07142857142; // 0.3 / 4.2
        total += getColor(1, -1) * 0.07142857142; // 0.3 / 4.2

        total.a = uAlpha;
        fragColor = total;
    }
}
