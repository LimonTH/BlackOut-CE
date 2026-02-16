package bodevelopment.client.blackout.rendering.font;

public class CharInfo {
    public int x;
    public int y;
    public int width;
    public int height;
    public float tx;
    public float ty;
    public float tw;
    public float th;

    public CharInfo(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void calcTexCoords(int width, int height) {
        this.tx = (float) this.x / width;
        this.ty = (float) this.y / height;
        this.tw = (float) this.width / width;
        this.th = (float) this.height / height;
    }
}
