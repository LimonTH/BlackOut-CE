frag {
    $alpha
    $res
    uniform sampler2D uTexture;

    uniform float blur;
    uniform vec4 clr;

    in vec2 texCoord0;

    out vec4 fragColor;

    fun vec4 getColor(float x, float y) {
        return texture(uTexture, texCoord0 + vec2(x, y) * blur / uResolution);
    }

    fun vec4 getBlurColor() {
        vec4 total = vec4(0);

        total += getColor(0, 0) * 0.2380952381;
        total += getColor(1, 0) * 0.119047619;
        total += getColor(-1, 0) * 0.119047619;
        total += getColor(0, 1) * 0.119047619;
        total += getColor(0, -1) * 0.119047619;

        total += getColor(1, 1) * 0.07142857143;
        total += getColor(-1, -1) * 0.07142857143;
        total += getColor(-1, 1) * 0.07142857143;
        total += getColor(1, -1) * 0.07142857143;

        vec4 c = total * clr;
        c.a *= uAlpha;
        return c;
    }

    fun void main() {
        fragColor = getBlurColor();
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

    fun vec4 getColor(float x, float y) {
        return texture(uTexture, vec2(lerp(lerpProgress(realPos.x, pos.x, pos.z), uv.x, uv.z), lerp(lerpProgress(realPos.y, pos.y, pos.w), uv.y, uv.w)) + vec2(x, y) * blur / uResolution);
    }

    fun vec4 getBlurColor() {
        vec4 total = vec4(0);

        total += getColor(0, 0) * 0.2380952381;
        total += getColor(1, 0) * 0.119047619;
        total += getColor(-1, 0) * 0.119047619;
        total += getColor(0, 1) * 0.119047619;
        total += getColor(0, -1) * 0.119047619;

        total += getColor(1, 1) * 0.07142857143;
        total += getColor(-1, -1) * 0.07142857143;
        total += getColor(-1, 1) * 0.07142857143;
        total += getColor(1, -1) * 0.07142857143;

        vec4 c = total * clr;
        c.a *= uAlpha;
        return c;
    }

    fun void main() {
        fragColor = getBlurColor();
    }
}
