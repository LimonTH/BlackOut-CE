frag {
    import utils.rainbow.hslWave;

    $alpha
    $res

    in vec2 realPos;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform float saturation;

    out vec4 fragColor;

    fun void main() {
        float position = gl_FragCoord.x / uResolution.x * frequency * 4.0;
        float x = 2.0 + fract(position + time * speed) * 6.2831;
        vec3 rgb = hslWave(x, saturation);

        fragColor = vec4(rgb, 1.0);
    }
}

vert {
    in vec3 Position;
    uniform mat4 uMatrices;
    $matrices

    out vec2 realPos;

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * uMatrices * vec4(Position, 1.0);
        realPos = vec2(Position.x, Position.y);
    }
}
