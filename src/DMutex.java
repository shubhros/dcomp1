import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class DMutex {
    private DProcess entity;
    private boolean token;
    private int rn[];
    private int ln[];
    private Semaphore sem;
    private boolean locked;
    private List<Integer[]> tokenQueue;

    public DMutex(DProcess entity, int maxpid) {
        this.entity = entity;
        this.token = false;
        this.rn = new int[maxpid];
        this.ln = new int[maxpid];
        this.sem = new Semaphore(0, true);
        tokenQueue = new LinkedList<Integer[]>();
    }

    public void SetTokenOwner() {
        this.token = true;
    }

    public void RequestCs() {
        System.out.println("Requesting critical section" + entity.GetPid());
        if (!token) {
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
            System.out.println("Entering critical section " + entity.GetPid());
            locked = true;
        } else {
            Integer [] req = new Integer[2];
            req[0] = entity.GetPid();
            req[1] = this.rn[entity.GetPid()];
            tokenQueue.add(req);
            this.sem.acquireUninterruptibly();
        }

    }

    public void ReleaseCs() {
        locked = false;
        ln[entity.GetPid()] = rn[entity.GetPid()];
        for (int i = 0; i < rn.length; i++) {
            if (rn[i] == ln[i] + 1) {
                // if this pid is not in the token queue
                boolean found = false;
                for (int j = 0; j < tokenQueue.size(); j++) {
                    if (tokenQueue.get(j)[0] == i) {
                        found = true;
                    }
                }
                if (!found) {
                    // site i has requested a token
                    Integer[] req = new Integer[2];
                    req[0] = i;
                    req[1] = rn[i];
                    tokenQueue.add(req);
                }
            }
        }
        if (!tokenQueue.isEmpty()) {
            // delete the top queue and send token to that entity
            Integer[] top = tokenQueue.remove(0);

            // if my request is on the top
            if (top[0] == this.entity.GetPid()) {
                try {
                    HandleToken(this.entity.GetPid());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // respond with token
                TOKEN tkn = new TOKEN();
                tkn.frompid = entity.GetPid();

                MsgWrapper msg = new MsgWrapper();
                msg.msg = tkn;
                msg.msgid = MessageID.TOKEN;

                try {
                    entity.SendMsg(msg, top[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Exiting critical section" + entity.GetPid());
    }



    public void HandleToken(int frompid) throws InterruptedException{
        token = true;
        // wake up request cs from here
        //System.out.println("token received from " + frompid);
        this.sem.release();
    }

    public void HandleRequestCs(int frompid, int seqno) {
        if (seqno > rn[frompid]) {
                rn[frompid] = seqno;
        }
        //if (token && locked) {
        //  Integer [] req = new Integer[2];
        //    req[0] = frompid;
        //req[1] = seqno;
        //    tokenQueue.add(req);
        if (token && !locked && (rn[frompid] == ln[frompid] + 1)) {
                token = false;
                locked = false;

                // respond with token
                TOKEN tkn = new TOKEN();
                tkn.frompid = entity.GetPid();

                MsgWrapper msg = new MsgWrapper();
                msg.msg = tkn;
                msg.msgid = MessageID.TOKEN;

                try {
                    entity.SendMsg(msg, frompid);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }


