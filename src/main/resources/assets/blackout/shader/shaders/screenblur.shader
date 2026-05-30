frag {
    $alpha
    $res

    uniform sampler2D uTexture;
    uniform vec4 clr;
    uniform float dist;

    out vec4 fragColor;

    const float W_CENTER   = 0.2380952381; // 1.0 / 4.2
    const float W_CARDINAL = 0.1190476190; // 0.5 / 4.2
    const float W_DIAGONAL = 0.0714285714; // 0.3 / 4.2

    fun vec4 getColor(float x, float y) {
        vec2 v = (gl_FragCoord.xy + vec2(x, y) * dist) / uResolution.xy;
        return texture(uTexture, v);
    }

    fun void main() {
        vec4 total = vec4(0);

        total += getColor( 0,  0) * W_CENTER;
        total += getColor( 1,  0) * W_CARDINAL;
        total += getColor(-1,  0) * W_CARDINAL;
        total += getColor( 0,  1) * W_CARDINAL;
        total += getColor( 0, -1) * W_CARDINAL;

        total += getColor( 1,  1) * W_DIAGONAL;
        total += getColor(-1, -1) * W_DIAGONAL;
        total += getColor(-1,  1) * W_DIAGONAL;
        total += getColor( 1, -1) * W_DIAGONAL;

        total.a = uAlpha;
        fragColor = total;
    }
}
