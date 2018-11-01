import java.io.IOException;
import java.util.concurrent.Semaphore;

public class DMutex {
    private DProcess entity;
    private boolean token;
    private int rn[];
    private int ln[];
    private Semaphore sem;

    public DMutex(DProcess entity, int maxpid) {
        this.entity = entity;
        this.token = false;
        this.rn = new int[maxpid];
        this.ln = new int[maxpid];
        this.sem = new Semaphore(0, true);
    }

    public void SetTokenOwner() {
        this.token = true;
    }

    public void RequestCs() {
        this.rn[entity.GetPid()]++;
        REQUEST req = new REQUEST();
        req.pid = entity.GetPid();
        req.seqno = this.rn[entity.GetPid()];

        MsgWrapper msg = new MsgWrapper();
        msg.msg = req;
        msg.msgid = MessageID.REQUEST;
        entity.Broadcast(msg);
        try {
            this.sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("got access to critical section "+ this.entity.GetPid());
    }

    public void ReleaseCs() {

    }

    public void HandleToken(int frompid) {
        token = true;
        // wake up request cs from here
        System.out.println("token received from "+frompid);
        this.sem.release();
    }

    public void HandleRequestCs(int frompid, int seqno) {
        if (seqno > rn[frompid]) {
            rn[frompid] = seqno;
        }
        if ((seqno == ln[frompid] + 1) && token) {
            System.out.println("allowing pid " + frompid + " to enter critical section");
        }
        if (token) {

            token = false;

            // respond with token
            TOKEN tkn = new TOKEN();
            tkn.frompid = entity.GetPid();

            MsgWrapper msg = new MsgWrapper();
            msg.msg = tkn;
            msg.msgid = MessageID.TOKEN;

            try {
                entity.SendMsg(msg, frompid);
            } catch (IOException e) {
                System.out.println("could not send message" + e);
            }

        }
    }
}

