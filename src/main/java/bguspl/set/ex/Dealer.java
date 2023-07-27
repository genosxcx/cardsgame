package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    public final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    public volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Offer offers = new Offer();
    public List<Integer> eSlots = new LinkedList<>();// list for empty slots
    private volatile boolean plFreeze = true;// freeze all players until table is filled
    private volatile boolean initPlayers = true;// freeze all players until table is full in the beggining
    private timeThread timer;
    private Thread timerThread;
    public volatile boolean pauseGame = false;// pause game until table is full
    public volatile boolean condition = false;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        timer = new timeThread(env, this);
        for (int i = 0; i < env.config.tableSize; i++) {// all slots are empty in the beggining
            eSlots.add(i);
        }
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        timerThread = new Thread(timer);
        pauseGame = true;// pause to fill the table
        for (Player pl : players) {// initializing threads
            Thread pThread = new Thread(pl, "" + pl.id);
            pThread.start();
        }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");

        // timerLoop();
        timerThread.start();
        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            pauseGame = false;// start game
            timerLoop();
            boolean shouldTermiante = env.util.findSets(deck, 1).size() == 0;
            if (shouldTermiante)
                terminate();
            removeAllCardsFromTable();
            updateTimerDisplay(true);
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        while (!shouldFinish() && !pauseGame) {
            updateTimerDisplay(false);
            helpFunction();
        }
    }

    private void helpFunction() {
        if (offers.size() > 0) {// check if we have submitted offers - (sets to check)
            List<Integer> ofr = offers.removeOffer();// get set
            Player pl = offers.removePlayer();// player who submitted the set
            if (pl != null && ofr != null) {
                int[] slotsSet = ConvertToArray(ofr);// convert from List set of slots to array set of slots
                int[] cardsSet = new int[env.config.featureSize];// set of cards
                for (int i = 0; i < slotsSet.length; i++) {
                    cardsSet[i] = table.getslotcard(slotsSet[i]);// convert from slots to cards on slots
                }
                boolean isLegal = env.util.testSet(cardsSet);// check if set is legal

                if (isLegal) {// legal
                    removeCardsFromTable(slotsSet);// remove cards from table
                    pl.condition = 1;
                    timer.resetTimer = true;
                } else {// not legal
                    pl.onlyDelete = true;
                    pl.condition = -1;
                    pl.returnOffer(ofr);// return to player his submitted set
                }
                synchronized (pl) {
                    pl.playerThread.interrupt();
                }
            }
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        timer.terminate = true;
        for (int i=players.length-1;i>=0;i--) {
            players[i].terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable(int[] slotsSet) {
        // TODO implement
        for (int i = 0; i < slotsSet.length; i++) {// check which cards to remove
            deck.remove((Object) table.getslotcard(slotsSet[i]));// remove card from deck
            table.removeCard(slotsSet[i]);// remove card from table
            table.removeTokens(slotsSet[i]);// remove tokens from table
            Fill(slotsSet[i]);// fill the emptied slot
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        pauseGame = true;
        while (eSlots.size() > 0) {// if there is an empty slot on the table shuffle and fill
            ShuffleandFill();
        }

        if (initPlayers) {
            for (Player p : players) {// freeze players until
                p.isFreeze = false;
            }
        } else {
            for (Player p : players) {// freeze players until
                if (p.getisFreeze())
                    p.isFreeze = false;
            }
        }
        initPlayers = false;
       // timer.resetTimer = true;
       pauseGame = false;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        plFreeze = true;
        for (int i = 0; i < table.cardToSlot.length; i++) {// remove all cards from table
            if (table.cardToSlot[i] != null) {// if slot is empty
                int slot = table.cardToSlot[i];
                table.removeTokens(slot);
                int card = table.removeCardd(slot, players);
                deck.add(card);
                // eSlots.add(i);// add empty slot to eSlot list

            }
        }
        // remove the tokens from players
        for (int i = 0; i < players.length; i++) {
            players[i].removeAllTokens();
        }
        for (int i = 0; i < env.config.columns * env.config.rows; i++) {// all slots are empty
            eSlots.add(i);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        int maxScore = 0;
        int num = 0;
        for (Player p : players) {// find max score
            if (p.score() > maxScore) {
                maxScore = p.score();
            }
        }

        for (Player p : players) {// find how much winners
            if (p.score() == maxScore) {
                num++;
            }
        }

        int[] winners = new int[num];// array for winners

        int i = 0;
        for (Player p : players) {// fill array with winners
            if (p.score() == maxScore) {
                winners[i] = p.id;
            }
        }
        env.ui.announceWinner(winners);// announce all winners
    }

    private void Fill(int slot) {
        Random rand = new Random();
        int randCard = deck.remove(rand.nextInt(deck.size()));// get random card from deck
        table.placeCard(randCard, slot);// put chosen random card in chosen random empty slot
    }

    public void ShuffleandFill() {
        Random rand = new Random();
        int randSlot = eSlots.remove(rand.nextInt(eSlots.size()));// get random empty slot
        int randCard = deck.remove(rand.nextInt(deck.size()));// get random card from deck

        table.placeCard(randCard, randSlot);// put chosen random card in chosen random empty slot
    }

    private int[] ConvertToArray(List<Integer> toConvert) {// convert List of int to array of int
        int[] output = new int[env.config.featureSize];
        int i = 0;
        while (i < toConvert.size()) {
            output[i] = toConvert.get(i);
            i++;
        }
        return output;
    }

    public boolean getGameCondition() {
        return pauseGame;
    }
}
