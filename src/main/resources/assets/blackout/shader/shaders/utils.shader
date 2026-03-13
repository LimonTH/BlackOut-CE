math {
    fun float lerp(float delta, float start, float end) {
        return start + (end - start) * delta;
    }

    fun float clampLerp(float delta, float start, float end) {
        return start + (end - start) * clamp(delta, 0, 1);
    }

    fun float lerpProgress(float value, float start, float end) {
        return (value - start) / (end - start);
    }

    fun vec2 clampVec2(vec2 val, vec4 c) {
        return vec2(clamp(val.x, c.x, c.z), clamp(val.y, c.y, c.w));
    }

    fun float sqdist(vec2 v1, vec2 v2) {
        vec2 v3 = v1 - v2;
        v3 *= v3;
        return v3.x + v3.y;
    }
}
