package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.rendering.shader.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShaderSetup {
    private final List<Consumer<Shader>> list = new ArrayList<>();

    public ShaderSetup(Consumer<Shader> consumer) {
        this.list.add(consumer);
    }

    public ShaderSetup() {
    }

    public ShaderSetup append(Consumer<Shader> consumer) {
        this.list.add(consumer);
        return this;
    }

    public void setup(Shader shader) {
        this.list.forEach(consumer -> consumer.accept(shader));
    }
}
