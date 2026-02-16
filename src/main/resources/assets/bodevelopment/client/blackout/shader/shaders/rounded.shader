frag {
    $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec4 clr;
    uniform vec2 rad;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun void main() {
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            fragColor = vec4(clr.rgb, clr.a * (1 - a1) * uAlpha);
        } else if (a1 == 0.0) {
            fragColor = vec4(clr.rgb, clr.a * uAlpha);
        }  else if (a1 == 1.0) {
            discard;
        }
    }
}

shadow {
    $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec4 shadowClr;
    uniform vec2 rad;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun float getShadowAlpha(float d) {
        return pow(clamp(1 - d / (rad.x + rad.y + 1), 0.0, 1.0), 1.2);
    }

    fun void main() {
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            vec4 color = vec4(shadowClr.rgb, shadowClr.a * getShadowAlpha(d) * a1);
            fragColor = vec4(color.rgb, color.a * uAlpha);
        } else if (a1 == 0.0) {
            discard;
        }  else if (a1 == 1.0) {
            float a = getShadowAlpha(d);
            fragColor = (a > 0.0) ? vec4(shadowClr.rgb, shadowClr.a * a * uAlpha) : vec4(0.0);
        }
    }
}

fade {
    $alpha
    $res

    in vec2 realPos;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform vec4 pos;
    uniform vec2 rad;
    uniform vec4 clr;
    uniform vec4 clr2;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun void main() {
        float position = gl_FragCoord.x / uResolution.x * frequency * 4.0;
        vec4 grad = mix(clr, clr2, (sin(position + time * speed) + 1.0) / 2.0);
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 != 1.0) {
            fragColor = vec4(grad.rgb, grad.a * (1.0 - a1) * uAlpha);
        } else {
            discard;
        }
    }
}

shadowfade {
    $alpha
    $res

    in vec2 realPos;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform vec4 pos;
    uniform vec2 rad;
    uniform vec4 clr;
    uniform vec4 clr2;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun float getShadowAlpha(float d) {
        return pow(clamp(1 - d / (rad.x + rad.y + 1), 0.0, 1.0), 1.2);
    }

    fun void main() {
        float position = gl_FragCoord.x / uResolution.x * frequency * 4.0;
        vec4 grad = mix(clr, clr2, (sin(position + time * speed) + 1.0) / 2.0);
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            vec4 color = vec4(grad.rgb, grad.a * getShadowAlpha(d) * a1);
            fragColor = vec4(color.rgb, color.a * uAlpha);
        } else if (a1 == 0.0) {
            discard;
        }  else if (a1 == 1.0) {
            float a = getShadowAlpha(d);
            fragColor = (a > 0.0) ? vec4(grad.rgb, grad.a * a * uAlpha) : vec4(0.0);
        }
    }
}

rainbow {
    $alpha
    $res

    in vec2 realPos;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform float saturation;
    uniform vec4 pos;
    uniform vec2 rad;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun void main() {
        float position = gl_FragCoord.x / uResolution.x * frequency * 4.0;
        float x = 2.0 + fract(position + time * speed) * 6.2831;

        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 - (clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, .0, 1.0)) + 1.0;

        vec4 bow = vec4(1 - r * saturation, 1 - g * saturation, 1 - b * saturation, 1.0);
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 != 1.0) {
            fragColor = vec4(bow.rgb, bow.a * (1.0 - a1) * uAlpha);
        } else {
            discard;
        }
    }
}

shadowrainbow {
    $alpha
    $res

    in vec2 realPos;

    uniform float time;
    uniform float frequency;
    uniform float speed;
    uniform float saturation;
    uniform vec4 pos;
    uniform vec2 rad;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun float getShadowAlpha(float d) {
        return pow(clamp(1 - d / (rad.x + rad.y + 1), 0.0, 1.0), 1.2);
    }

    fun void main() {
        float position = gl_FragCoord.x / uResolution.x * frequency * 4.0;
        float x = 2.0 + fract(position + time * speed) * 6.2831;

        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 - (clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, .0, 1.0)) + 1.0;

        vec4 bow = vec4(1 - r * saturation, 1 - g * saturation, 1 - b * saturation, 1.0);
        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            vec4 color = vec4(bow.rgb, bow.a * getShadowAlpha(d) * a1);
            fragColor = vec4(color.rgb, color.a * uAlpha);
        } else if (a1 == 0.0) {
            discard;
        }  else if (a1 == 1.0) {
            float a = getShadowAlpha(d);
            fragColor = (a > 0.0) ? vec4(bow.rgb, bow.a * a * uAlpha) : vec4(0.0);
        }
    }
}

tenacity {
    $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec2 rad;
    uniform float time;
    uniform float speed;
    uniform vec4 color1;
    uniform vec4 color2;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun void main() {
        vec2 st = realPos / pos.zw;

        vec2 center = (pos.xy + pos.zw / 2.0) / pos.zw;

        vec2 o = vec2(st.x * 2.0, st.y * 2.0) - center * 2.0;

        float angle = time * 0.5 * speed;
        float cosAngle = cos(angle);
        float sinAngle = sin(angle);
        float x = o.x * cosAngle - o.y * sinAngle;
        float y = o.x * sinAngle + o.y * cosAngle;

        x = (x + 1.0) / 2.0;
        y = (y + 1.0) / 2.0;

        vec4 color = mix(color1, color2, (x + y) / 2.0);

        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            fragColor = vec4(color.rgb, color.a * (1 - a1) * uAlpha);
        } else if (a1 == 0.0) {
            fragColor = vec4(color.rgb, color.a * uAlpha);
        }  else if (a1 == 1.0) {
            discard;
        }
    }
}

tenacityshadow {
     $alpha

    in vec2 realPos;

    uniform vec4 pos;
    uniform vec2 rad;
    uniform float time;
    uniform float speed;
    uniform vec4 color1;
    uniform vec4 color2;

    out vec4 fragColor;

    fun float roundedDist(vec2 vec, vec4 v) {
        float x = clamp(vec.x, v.x, v.x + v.z) - vec.x;
        float y = clamp(vec.y, v.y, v.y + v.w) - vec.y;

        return x * x + y * y;
    }

    fun float getShadowAlpha(float d) {
        return pow(clamp(1 - d / (rad.x + rad.y + 1), 0.0, 1.0), 1.2);
    }

    fun void main() {
        vec2 st = realPos / pos.zw;

        vec2 center = (pos.xy + pos.zw / 2.0) / pos.zw;

        vec2 o = vec2(st.x * 2.0, st.y * 2.0) - center * 2.0;

        float angle = time * 0.5 * speed;
        float cosAngle = cos(angle);
        float sinAngle = sin(angle);
        float x = o.x * cosAngle - o.y * sinAngle;
        float y = o.x * sinAngle + o.y * cosAngle;

        x = (x + 1.0) / 2.0;
        y = (y + 1.0) / 2.0;

        vec4 clr = mix(color1, color2, (x + y) / 2.0);

        float d = sqrt(roundedDist(realPos, pos));

        float offset = d - rad.x;
        float a1 = clamp(offset, 0.0, 1.0);

        if (a1 > 0.0 && a1 < 1.0) {
            vec4 color = vec4(clr.rgb, clr.a * getShadowAlpha(d) * a1);
            fragColor = vec4(color.rgb, color.a * uAlpha);
        } else if (a1 == 0.0) {
            discard;
        }  else if (a1 == 1.0) {
            float a = getShadowAlpha(d);
            fragColor = (a > 0.0) ? vec4(clr.rgb, clr.a * a * uAlpha) : vec4(0.0);
        }
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
