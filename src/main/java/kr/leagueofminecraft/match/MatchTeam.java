package kr.leagueofminecraft.match;

import java.util.Locale;

/** The two playable Rift teams plus the unassigned lobby state. */
public enum MatchTeam {
    BLUE("블루", "§9"),
    RED("레드", "§c"),
    UNASSIGNED("미배정", "§7");

    private final String displayName;
    private final String color;

    MatchTeam(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() { return displayName; }
    public String coloredName() { return color + displayName; }
    public boolean isPlayable() { return this != UNASSIGNED; }

    public static MatchTeam parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "blue", "b", "블루", "파랑" -> BLUE;
            case "red", "r", "레드", "빨강" -> RED;
            case "none", "leave", "unassigned", "미배정" -> UNASSIGNED;
            default -> throw new IllegalArgumentException("Unknown team: " + value);
        };
    }
}
