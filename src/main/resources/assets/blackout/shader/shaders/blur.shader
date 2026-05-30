frag {
    $alpha
    $res
    uniform sampler2D uTexture;

    uniform float blur;
    uniform vec4 clr;

    in vec2 texCoord0;

    out vec4 fragColor;

    const float W_CENTER   = 0.2380952381; // 1.0 / 4.2
    const float W_CARDINAL = 0.1190476190; // 0.5 / 4.2
    const float W_DIAGONAL = 0.0714285714; // 0.3 / 4.2

    fun vec4 getColor(float x, float y) {
        return texture(uTexture, texCoord0 + vec2(x, y) * blur / uResolution);
    }

    fun vec4 getBlur() {
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

        vec4 c = total * clr;
        c.a *= uAlpha;
        return c;
    }

    fun void main() {
        fragColor = getBlur();
    }
}

blurUV {
    import utils.math.lerp;
    import utils.math.lerpProgress;

    $alpha
    $res

    uniform sampler2D uTexture;

    uniform float blur;
    uniform vec4 clr;
    uniform vec4 pos;
    uniform vec4 uv;

    in vec2 realPos;

    out vec4 fragColor;

    const float W_CENTER   = 0.2380952381;
    const float W_CARDINAL = 0.1190476190;
    const float W_DIAGONAL = 0.0714285714;

    fun vec4 getColor(float x, float y) {
        return texture(uTexture, vec2(lerp(lerpProgress(realPos.x, pos.x, pos.z), uv.x, uv.z), lerp(lerpProgress(realPos.y, pos.y, pos.w), uv.y, uv.w)) + vec2(x, y) * blur / uResolution);
    }

    fun vec4 getBlur() {
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

        vec4 c = total * clr;
        c.a *= uAlpha;
        return c;
    }

    fun void main() {
        fragColor = getBlur();
    }
}
