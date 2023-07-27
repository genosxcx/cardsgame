package bguspl.set.ex;

import bguspl.set.Env;

public class timeThread extends Thread {

  // The number of seconds to count down from
  private long countdown;
  public Env env;
  public Thread timerThread;
  public boolean resetTimer;
  public boolean terminate;
  public Dealer dealer;

  public timeThread(Env env, Dealer dealer) {
    this.env = env;
    countdown = env.config.turnTimeoutMillis;
    resetTimer = true;
    terminate = false;
    this.dealer = dealer;
  }

  @Override
  public void run() {
    timerThread = Thread.currentThread();

    while (!terminate) {
      if (countdown < 0) {
        dealer.pauseGame = true;

        resetTimer = true;
      }
      if (resetTimer) {
        resetTimer = false;
        countdown = env.config.turnTimeoutMillis;
      }
      if (countdown > env.config.turnTimeoutWarningMillis) {
        env.ui.setCountdown(countdown, false);
        countdown -= 1000;
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

      }else if(countdown > 0 && countdown < 1000){
        countdown = 0;
        try {
          Thread.sleep(countdown);
        } catch (InterruptedException ignored) {
        }
      }
      
      else  {
        env.ui.setCountdown(countdown, true);
        countdown -= 10;
        try {
          Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
      }
    }

  }
}