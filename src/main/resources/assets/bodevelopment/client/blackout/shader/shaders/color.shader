frag {
    $alpha

    in vec4 vertexColor;
    uniform float time;
    uniform float alp;

    out vec4 fragColor;

    fun void main() {
        vec4 color = vertexColor;
        if (color.a == 0.0) {
            discard;
        }
        color.a *= uAlpha;
        fragColor = color;
    }
}

thingy {
    $alpha
    $res

    uniform float time;
    uniform vec2 mouse;
    uniform vec2 resolution;
    uniform vec3 clr1;
    uniform vec3 clr2;

    out fragColor;

    float getSus(vec2 timeV) {
        vec2 position = gl_FragCoord.xy / uResolution.xy ;
        vec2 timeVec = vec2(time) * 0.4 * timeV;

        vec2 mogus = (sin(position * 20.0 + timeVec) + 1.0) / 2.0;
        return length(mogus);
    }

    void main( void ) {
        vec2 position = (sin(gl_FragCoord.xy / resolution.xy + vec2(time / 10.0)) + 1.0) / 2.0;

        float sus = getSus(vec2(1, 0.3));
        float sus2 = getSus(vec2(1.5, 2));
        float sus3 = getSus(vec2(-1.5, 1));

        float m = (sin(sus + sus2 + sus3 + sin(position.x * 8.0) - sin(position.y * 8.0)) + 1.0) / 2.0;

        fragColor = vec4(clr1 + (clr2 - clr1) * m, uAlpha);
    }
}

vert {
    $posclr
    $matrices

    out vec4 vertexColor;

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

        vertexColor = Color;
    }
}
