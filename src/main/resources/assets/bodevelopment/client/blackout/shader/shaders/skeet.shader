frag {
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

        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 - (clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, .0, 1.0)) + 1.0;

        vec4 bow = vec4(1 - r * saturation, 1 - g * saturation, 1 - b * saturation, 1.0);

        fragColor = vec4(bow.rgb, 1.0);
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
