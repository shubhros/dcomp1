public class Main
{
    public static void main(String []args) throws InterruptedException
    {
        int maxpid = 10;
        DProcess p[] = new DProcess[maxpid];
        for (int i = 0; i < maxpid; i++) {
            p[i] = new DProcess(i, maxpid);
            p[i].StartServer();
        }

        Thread.sleep(1000);
        for(int i = 0; i < maxpid; i++) {
            for(int j = 0; j < maxpid; j++) {
                if (i != j) {
                    p[i].connectToProcess(j);
                }

            }

        }

        Thread.sleep(1000);

        HiPacket hi = new HiPacket();
        hi.message = "THIS IS HI MESSAGE";
        hi.pid = 0;

        MsgWrapper msg = new MsgWrapper();
        msg.msgid = MessageID.HI;
        msg.msg = hi;

        System.out.println("Testing broadcast message");
        p[0].Broadcast(msg);
    }

}
