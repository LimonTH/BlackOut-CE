package bodevelopment.client.blackout.util;

public class SelectedComponent {
    private static int id = -1;
    private static int prevId = 0;

    public static boolean isSelected() {
        return id != -1;
    }

    public static void reset() {
        id = -1;
    }

    public static boolean is(int newId) {
        return id == newId;
    }

    public static int getId() {
        return id;
    }

    public static void setId(int newId) {
        id = newId;
    }

    public static int nextId() {
        return prevId++;
    }
}
