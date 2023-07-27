package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        table = new Table(env);
        player = new Player(env, dealer, table, 0, false);
        Player[] players = { player };
        dealer = new Dealer(env, table, players);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void terminate() {
        assertEquals(false, dealer.terminate);
        assertNotEquals(true, dealer.terminate);
        assertEquals(false, player.terminate);
        assertNotEquals(true, player.terminate);
        dealer.terminate();
        assertEquals(true, dealer.terminate);
        assertNotEquals(false, dealer.terminate);
        assertEquals(true, player.terminate);
        assertNotEquals(false, player.terminate);
    }

    @Test
    void ShuffleandFill() {
        assertEquals(dealer.env.config.tableSize, dealer.eSlots.size());
        assertNotEquals(0, dealer.eSlots.size());

        for (int i = 0; i <dealer.env.config.tableSize; i++)
            dealer.ShuffleandFill();

        assertEquals(0, dealer.eSlots.size());
        assertNotEquals(dealer.env.config.tableSize, dealer.eSlots.size());

    }
}