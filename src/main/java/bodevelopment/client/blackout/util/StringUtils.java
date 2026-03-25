package bodevelopment.client.blackout.util;

public class StringUtils {
    public static double similarity(String string1, String string2) {
        String shorter;
        String longer;
        if (string1.length() > string2.length()) {
            shorter = string2.toLowerCase();
            longer = string1.toLowerCase();
        } else {
            shorter = string1.toLowerCase();
            longer = string2.toLowerCase();
        }

        int i = 0;
        int matched = 0;

        for (int c = 0; c < longer.length(); c++) {
            char charLonger = longer.charAt(c);

            for (int ci = i; ci < shorter.length(); ci++) {
                if (charLonger == shorter.charAt(ci)) {
                    matched++;
                    i = ci;
                    break;
                }
            }
        }

        return (float) matched / longer.length();
    }
}
