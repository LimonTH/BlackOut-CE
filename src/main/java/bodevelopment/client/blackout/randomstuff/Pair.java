package bodevelopment.client.blackout.randomstuff;


import java.util.Objects;

public class Pair<A, B> extends net.minecraft.util.Pair<A, B> {
    public Pair(A left, B right) {
        super(left, right);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else {
            return obj instanceof Pair<?, ?> pair && Objects.equals(this.getLeft(), pair.getLeft()) && Objects.equals(this.getRight(), pair.getRight());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getLeft(), this.getRight());
    }
}
