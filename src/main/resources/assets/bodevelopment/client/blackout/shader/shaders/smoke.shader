frag {
    $alpha
    $res
    out vec4 fragColor;

    uniform float time;
    uniform vec4 clr1;
    uniform vec4 clr2;
    uniform float speed;

    fun mat3 rotX(float a) {
        float c = cos(a);
        float s = sin(a);
        return mat3(
            1, 0, 0,
            0, c, -s,
            0, s, c
        );
    }

    fun mat3 rotY(float a) {
        float c = cos(a);
        float s = sin(a);
        return mat3(
            c, 0, -s,
            0, 1, 0,
            s, 0, c
        );
    }

    fun float random(vec2 pos) {
        return fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453123);
    }

    fun float noise(vec2 pos) {
        vec2 i = floor(pos);
        vec2 f = fract(pos);
        float a = random(i);
        float b = random(i + vec2(1.0, 0.0));
        float c = random(i + vec2(0.0, 1.0));
        float d = random(i + vec2(1.0, 1.0));
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    fun float fbm(vec2 pos) {
        float v = 0.0;
        float a = 0.5;
        vec2 shift = vec2(100.0);
        mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
        for (int i = 0; i < 6; i++) {
            float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;
            v += a * noise(pos - 0.05 * dir * time * speed);

            pos = rot * pos * 2.0 + shift;
            a *= 0.5;
        }
        return v;
    }

    fun void main() {
        vec2 p = (gl_FragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
        p -= vec2(12.0, 0.0);

        float t = 0.0, d;

        float time2 = 1.0;

        vec2 q = vec2(0.0);
        q.x = fbm(p);
        q.y = fbm(p + vec2(1.0));
        vec2 r = vec2(fbm(p + q + vec2(1.7, 9.2) + 0.15 * time2), fbm(p + q + vec2(8.3, 2.8) + 0.126 * time2));
        float f = fbm(p + r);

        vec3 color = mix(
            vec3(0.3, 0.3, 0.6),
            vec3(0.7, 0.7, 0.7),
            clamp((f * f) * 4.0, 0.0, 1.0)
        );

        color = mix(
            color,
            vec3(0.7, 0.7, 0.7),
            clamp(length(q), 0.0, 1.0)
        );

        color = mix(
            color,
            vec3(0.4, 0.4, 0.4),
            clamp(length(r.x), 0.0, 1.0)
        );

        color = (f * f * f + 0.9 * f * f + 0.8 * f) * color;

        fragColor = clr1 + (clr2 - clr1) * color.r;
    }
}
