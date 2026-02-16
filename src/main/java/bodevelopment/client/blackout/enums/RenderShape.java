package bodevelopment.client.blackout.enums;

public enum RenderShape {
    Outlines(true, false),
    Sides(false, true),
    Full(true, true);

    public final boolean outlines;
    public final boolean sides;

    RenderShape(boolean outlines, boolean sides) {
        this.outlines = outlines;
        this.sides = sides;
    }
}
