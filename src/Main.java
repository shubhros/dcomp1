import java.util.Random;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int maxpid = 5;
        DProcess p[] = new DProcess[maxpid];
        DMutex d[] = new DMutex[maxpid];
        for (int i = 0; i < maxpid; i++) {
            p[i] = new DProcess(i, maxpid);
            p[i].StartServer();
        }

        Thread.sleep(1000);
        for (int i = 0; i < maxpid; i++) {
            for (int j = 0; j < maxpid; j++) {
                //if (i != j) {
                    p[i].connectToProcess(j);
                //}

            }

        }
        Thread.sleep(1000);

        p[0].SetTokenOwner();




        for (int i = 0; i < 1; i++) {
          //  p[i].ExecuteCriticalSection();
            new Thread() {
                Random r = new Random();
                public void run() {
                    for (;;) {
                        try {
                            p[2].ExecuteCriticalSection();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    /*for (int j = 0; j < maxpid; j++) {
                        int v = r.nextInt(maxpid - 1);
                        try {
                            p[v].ExecuteCriticalSection();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }*/
                   // System.out.println("End loop");

                }
            }.start();

        }
    }
}
