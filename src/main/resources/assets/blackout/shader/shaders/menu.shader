frag {
    $alpha
    $res

    uniform float time;

    out vec4 fragColor;

    fun mat2 rotate2D(float r) {
        return mat2(cos(r), sin(r), -sin(r), cos(r));
    }

    fun void main() {
        vec2 uv = (gl_FragCoord.xy - 0.5 * uResolution.xy) / uResolution.y;
        vec3 col = vec3(0);
        float t = time*0.2;

        float dd = length(uv) + 0.5;

        vec2 n = vec2(0);
        vec2 q = vec2(0);
        vec2 p = uv;
        float d = dot(p,p);
        float S = (dd - 0.3) * 5.0;
        float a = 0.0;
        mat2 m = rotate2D(dd * 1.5);

        for (float j = 0.0; j < 8.0; j++) {
            p *= m;
            n *= m;
            q = p * S + t * 4.0 + sin(t * 1.0 - d * 8.0) * 0.0018 + 3.0 * j - 0.95 * n;
            a += dot(cos(q) / S, vec2(0.2));
            n -= sin(q + (dd * 20.0));
            S *= 1.4;
        }

        float rr = 1.0 - abs(-0.2 + length(uv));
        rr = rr * rr * rr / 2.0;
        col = vec3(4.0, 0.0, 0.0) * (a + 0.5) + a + a - d;
        col = col.rgb * vec3(rr, rr, rr);


        // Output to screen
        fragColor = vec4(col * dd, uAlpha);
    }
}
