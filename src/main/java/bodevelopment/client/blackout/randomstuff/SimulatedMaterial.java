package bodevelopment.client.blackout.randomstuff;

import net.minecraft.world.item.ToolMaterial;

public enum SimulatedMaterial {
    WOOD(ToolMaterial.WOOD),
    STONE(ToolMaterial.STONE),
    IRON(ToolMaterial.IRON),
    GOLD(ToolMaterial.GOLD),
    DIAMOND(ToolMaterial.DIAMOND),
    NETHERITE(ToolMaterial.NETHERITE);

    private final ToolMaterial material;

    SimulatedMaterial(ToolMaterial material) {
        this.material = material;
    }

    public ToolMaterial get() {
        return this.material;
    }
}