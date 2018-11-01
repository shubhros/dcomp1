public class Main {
    public static void main(String[] args) throws InterruptedException {
        int maxpid = 10;
        DProcess p[] = new DProcess[maxpid];
        DMutex d[] = new DMutex[maxpid];
        for (int i = 0; i < maxpid; i++) {
            p[i] = new DProcess(i, maxpid);
            p[i].StartServer();
        }

        Thread.sleep(1000);
        for (int i = 0; i < maxpid; i++) {
            for (int j = 0; j < maxpid; j++) {
                if (i != j) {
                    p[i].connectToProcess(j);
                }

            }

        }
        Thread.sleep(1000);

        p[6].SetTokenOwner();

        p[0].RequestCs();


    }
}