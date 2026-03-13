frag {
    $alpha

    uniform sampler2D uTexture;

    uniform vec4 clr;

    in vec2 texCoord0;

    out vec4 fragColor;

    fun void main() {
        vec4 c = texture(uTexture, texCoord0) * clr;
        fragColor = vec4(c.r, c.g, c.b, c.a * uAlpha);
    }
}

frag2 {
    import utils.math.lerpProgress;
    $alpha

    uniform sampler2D uTexture;

    uniform vec4 clr;

    in vec2 texCoord0;

    out vec4 fragColor;

    fun void main() {
        vec4 c = texture(uTexture, texCoord0) * clr;
        fragColor = vec4(c.r, c.g, c.b, c.a * uAlpha);
    }
}

fragUV {
    import utils.math.lerp;
    import utils.math.lerpProgress;
    $alpha

    uniform sampler2D uTexture;
    uniform vec4 clr;
    uniform vec4 pos;
    uniform vec4 uv;

    in vec2 realPos;
    out vec4 fragColor;

    fun void main() {
        vec4 c = texture(uTexture, vec2(lerp(lerpProgress(realPos.x, pos.x, pos.z), uv.x, uv.z), lerp(lerpProgress(realPos.y, pos.y, pos.w), uv.y, uv.w)));
        fragColor = vec4(c.r, c.g, c.b, c.a * uAlpha) * clr;
    }
}

vert {
    $posuv
    $matrices

    out vec2 texCoord0;

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

        texCoord0 = UV0;
    }
}
