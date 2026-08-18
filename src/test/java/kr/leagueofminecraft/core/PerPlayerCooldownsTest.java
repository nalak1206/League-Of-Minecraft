package kr.leagueofminecraft.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PerPlayerCooldownsTest {
    private enum Action { ZHONYA, YOUMUU }

    @Test void keepsItemsAndPlayersIndependent() {
        PerPlayerCooldowns<Action> cooldowns = new PerPlayerCooldowns<>();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        cooldowns.start(first, Action.ZHONYA, 1_000L, 5_000L);

        assertEquals(5_000L, cooldowns.remainingMillis(first, Action.ZHONYA, 1_000L));
        assertEquals(0L, cooldowns.remainingMillis(first, Action.YOUMUU, 1_000L));
        assertEquals(0L, cooldowns.remainingMillis(second, Action.ZHONYA, 1_000L));
    }

    @Test void expiresAndPrunesCooldowns() {
        PerPlayerCooldowns<Action> cooldowns = new PerPlayerCooldowns<>();
        UUID player = UUID.randomUUID();
        cooldowns.start(player, Action.ZHONYA, 1_000L, 2_000L);
        assertEquals(1L, cooldowns.remainingMillis(player, Action.ZHONYA, 2_999L));
        cooldowns.prune(3_000L);
        assertEquals(0L, cooldowns.remainingMillis(player, Action.ZHONYA, 3_000L));
    }
}
