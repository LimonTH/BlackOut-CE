glow {
    import esp.utils.getDist;

    $alpha
    $res

    uniform sampler2D uTexture;
    uniform int rad;
    uniform vec4 clr;
    uniform vec4 clr2;

    out vec4 fragColor;

    fun void main() {
        float dist = getDist();
        vec4 c = texture(uTexture, gl_FragCoord.xy / uResolution.xy);

        if (c.a == 0.0) {
            float dist = getDist();
            if (dist > 0.0 && dist <= 1.0)
                c = vec4(clr.rgb, clr.a * (1.0 - dist));
        } else c = clr2;

        c.a *= uAlpha;
        fragColor = c;
    }
}

outline {
    import esp.utils.getDist;

    $alpha
    $res

    uniform sampler2D uTexture;
    uniform int rad;
    uniform vec4 clr;
    uniform vec4 clr2;

    out vec4 fragColor;

    fun void main() {
        vec4 c = texture(uTexture, gl_FragCoord.xy / uResolution.xy);

        if (c.a == 0.0) {
            float dist = getDist();
            if (dist > 0.0) {
                float sus = 1.0 - 1.0 / (rad / 2.0);
                if (dist <= sus)
                    c = clr;
                else
                    c = vec4(clr.rgb, clr.a * (1.0 - (dist - sus) / (1.0 - sus)));
            }
        } else c = clr2;

        c.a *= uAlpha;
        fragColor = c;
    }
}

utils {
    fun float getDist() {
        if (texture(uTexture, gl_FragCoord.xy / uResolution.xy).a != 0.0) {
            return -1.0;
        } else {
            float closestDist = 69.0;
            for (int x = -rad; x <= rad; x++) {
                for (int y = -rad; y <= rad; y++) {
                    float d = sqrt(x * x + y * y);
                    if (d < closestDist && texture(uTexture, (gl_FragCoord.xy + vec2(x, y)) / uResolution.xy).a > 0.03) closestDist = d;
                }
            }
            return closestDist / rad;
        }
    }
}

vert {
    $matrices

    in vec3 Position;

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    }
}
