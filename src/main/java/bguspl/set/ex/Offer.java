package bguspl.set.ex;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Offer {
    private volatile LinkedBlockingQueue<Player> playerSet;// queue for player's submitted set

    private volatile LinkedBlockingQueue<List<Integer>> offers;// sets to be checked queue

    public Offer() {
        playerSet = new LinkedBlockingQueue<>();
        offers = new LinkedBlockingQueue<>();
    }

    public void addOffer(List<Integer> addOffer, Player p) {
        try {
            offers.put(addOffer);
            playerSet.put(p);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int size() {
        // System.out.println("in offers size (class itself");
        return offers.size();
    }

    public List<Integer> removeOffer() {
        try {
            return offers.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    public Player removePlayer() {
        if (!playerSet.isEmpty()) {
            try {
                return playerSet.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        } else
            return null;
    }
}