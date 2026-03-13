vert {
    in vec3 Position;
    $matrices

    fun void main() {
        gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    }
}
