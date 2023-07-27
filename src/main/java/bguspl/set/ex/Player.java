package bguspl.set.ex;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    public Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    public volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    Dealer dealer;
    public volatile ArrayList<Integer> tokens = new ArrayList<>();
    public volatile LinkedBlockingQueue<Integer> PressedSlots = new LinkedBlockingQueue<>();// queue for submitted
                                                                                            // actions
    public volatile boolean isFreeze;// stop player until dealer allows him to continue
    public volatile boolean onlyDelete;// player has a illegal set and must delete tokens only
    public volatile int condition;// condition for player (0 continue playing, 1 point, -1 penalty)
    public volatile boolean con;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        condition = 0;// contniue playing
        isFreeze = true;// stop player
        onlyDelete = false;// initialize
        con = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human)
            createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            try {
                Integer pSlot = PressedSlots.take();
                if (tokens.size() < env.config.featureSize && !onlyDelete) {// check if we have 3 tokens submitted or not
                    if (tokens.contains((Object) pSlot)) {// remove the exited token in set
                        tokens.remove((Object) pSlot);// remove from player's set
                        table.removeToken(id, pSlot);// update table
                    } else {// add token to the set
                        if (!dealer.eSlots.contains((Object) pSlot)) {// check if chosen slot is not empty - (late in
                                                                      // game we might have empty slots)
                            tokens.add(pSlot);// add token to set
                            table.placeToken(id, pSlot);// update table
                        }
                    }

                    if (tokens.size() == env.config.featureSize) {// we have a set after the added token
                        isFreeze = true;// freeze player until dealer check the set
                        dealer.offers.addOffer(tokens, this);// send set to dealer
                        tokens = new ArrayList<>();// clear player's tokens
                        synchronized (this) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        if (condition == 1) {// got point
                            point();
                        } else if (condition == -1) {// penalty
                            penalty();
                        }
                        
                        if (!human) {
                            PressedSlots.clear();
                        }
                    }

                } else {// we have 3 token but not legal set
                    if (tokens.contains((Object) pSlot)) {// check if we submitted token in set
                        onlyDelete = false;
                        tokens.remove((Object) pSlot);// delete token
                        table.removeToken(id, pSlot);// update table
                    }
                }
            } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
        }// get submitted token
        }
        if (!human)
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {
            }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.`
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                int keyPressed = new Random().nextInt(12);
                keyPressed(keyPressed);
                try {
                    synchronized (aiThread) {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ignored) {
                }
            }

            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
        try {
            playerThread.interrupt();
            playerThread.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     * @throws InterruptedException
     */
    public void keyPressed(int slot) {
        // TODO implement
        if (!isFreeze && !dealer.getGameCondition() && table.slotToCard[slot] != null) {
            try {
                PressedSlots.put(slot);
            } catch (InterruptedException e) { }
        }

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        long timer = env.config.pointFreezeMillis;
        while (timer > 0) {
            env.ui.setFreeze(id, timer);
            if(timer > 0 && timer < 1000){
                try {
                    Thread.sleep(env.config.penaltyFreezeMillis);
                } catch (InterruptedException ignored) {
                }
                timer = 0;
            }
            else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            timer = timer - 1000;
        }
    }
        env.ui.setFreeze(id, 0);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        isFreeze = false;// allow player to continue play
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long timer = env.config.penaltyFreezeMillis;
        while (timer > 0) {
            env.ui.setFreeze(id, timer);
            if(timer > 0 && timer < 1000){
                try {
                    Thread.sleep(env.config.penaltyFreezeMillis);
                } catch (InterruptedException ignored) {
                }
                timer = 0;
            }
            else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            timer = timer - 1000;
        }

        }
        env.ui.setFreeze(id, 0);
        isFreeze = false;// allow player to continue play
    }

    public int score() {
        return score;
    }

    public void returnOffer(List<Integer> ofr) {// return the token to player
        while (ofr.size() > 0) {
            tokens.add(ofr.remove(0));
        }
    }

   /*public Thread getThread() {
        if (human)
            return playerThread;
      *  return aiThread;
    }*/

    public boolean getisFreeze() {
        return isFreeze;
    }

    public void removeAllTokens() {
        while (tokens.size() > 0) {
            tokens.remove(0);
        }
        onlyDelete = false;
        condition = 0;
        PressedSlots.clear();

    }

    public boolean getTerminate() {
        return terminate;
    }

    public int tokensSize() {
        return tokens.size();
    }
}
