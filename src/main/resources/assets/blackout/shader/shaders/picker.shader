frag {
    import utils.math.lerpProgress;

    $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec3 clr;

    out vec4 fragColor;

    fun void main() {
        float saturation = lerpProgress(realPos.x, pos.x, pos.x + pos.z);
        float brightness = 1 - lerpProgress(realPos.y, pos.y, pos.y + pos.w);

        vec3 white = vec3(1.0, 1.0, 1.0);
        vec3 c = white + (clr - white) * saturation;

        fragColor = vec4(c * brightness, uAlpha);
    }
}
