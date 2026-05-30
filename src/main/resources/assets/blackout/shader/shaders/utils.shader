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

rainbow {
    // HSL→RGB rainbow wave.  x ∈ [0, 2π] maps through the hue spectrum.
    // Returns (r, g, b) ready for mixing with a base colour.
    @fun vec3 hslWave(float x, float saturation) {
        float r = -(clamp(x - 3.0, 0.0, 1.0) + clamp(-x + 1.0, 0.0, 1.0)) + 1.0 - (clamp(x - 9.0, 0.0, 1.0) + clamp(-x + 7.0, 0.0, 1.0)) + 1.0;
        float g = -(clamp(x - 5.0, 0.0, 1.0) + clamp(-x + 3.0, 0.0, 1.1)) + 1.0;
        float b = -(clamp(x - 7.0, 0.0, 1.0) + clamp(-x + 5.0, 0.0, 1.0)) + 1.0;
        return vec3(1.0 - r * saturation, 1.0 - g * saturation, 1.0 - b * saturation);
    }

}
