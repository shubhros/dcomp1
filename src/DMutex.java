import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class DMutex {
    private DProcess entity;
    private boolean tokenAvailable;
    private boolean locked;
    private Token token; // available only when token is true
    private Semaphore nlock;
    private Semaphore tokenLock;
    private int rn[];
    private Semaphore sem;


    private List<Integer[]> tokenQueue;

    public DMutex(DProcess entity, int maxpid) {
        this.entity = entity;
        this.tokenAvailable = false;
        this.rn = new int[maxpid];
        this.sem = new Semaphore(0, true);
        this.tokenLock = new Semaphore(1, true);
        this.nlock = new Semaphore(1, true);
    }

    public void SetTokenOwner() {
        tokenLock.acquireUninterruptibly();
        this.token = new Token();
        this.token.ln = new int[this.rn.length];
        this.token.tokenQueue = new ArrayList<Integer[]>();
        this.tokenAvailable = true;
        tokenLock.release();
    }

    public void RequestCs() {
        System.out.println("Requesting critical section" + entity.GetPid());
        this.tokenLock.acquireUninterruptibly();
        if (!tokenAvailable) {
            this.tokenLock.release();

            // update rn
            this.nlock.acquireUninterruptibly();
            this.rn[entity.GetPid()]++;
            this.nlock.release();

            // broadcast message for requesting
            REQUEST req = new REQUEST();
            req.pid = entity.GetPid();

            this.nlock.acquireUninterruptibly();
            req.seqno = this.rn[entity.GetPid()];
            this.nlock.release();

            MsgWrapper msg = new MsgWrapper();
            msg.msg = req;
            msg.msgid = MessageID.REQUEST;
            entity.Broadcast(msg);

            // wait for the token
            this.sem.acquireUninterruptibly();
            System.out.println("Entering critical section " + entity.GetPid());

            this.tokenLock.acquireUninterruptibly();
            locked = true;
            this.tokenLock.release();
        } else {
            // just release the lock
            this.tokenLock.release();
        }
    }

    public void ReleaseCs() {
        this.tokenLock.acquireUninterruptibly();
        token.ln[entity.GetPid()] = rn[entity.GetPid()];
        this.nlock.acquireUninterruptibly();
        for (int i = 0; i < rn.length; i++) {
            if (rn[i] == token.ln[i] + 1) {
                // if this pid is not in the token queue
                boolean found = false;
                for (int j = 0; j < token.tokenQueue.size(); j++) {
                    if (token.tokenQueue.get(j)[0] == i) {
                        found = true;
                    }
                }
                if (!found) {
                    // site i has requested a token
                    Integer[] req = new Integer[2];
                    req[0] = i;
                    req[1] = rn[i];
                    System.out.println("release cs have a pending request from"+ req[0]);

                    token.tokenQueue.add(req);
                }
            }
        }
        this.nlock.release();
        if (!token.tokenQueue.isEmpty()) {
            // delete the top queue and send token to that entity
            Integer[] top = token.tokenQueue.remove(0);

            // if my request is on the top
            /*if (top[0] == this.entity.GetPid()) {
                try {
                    HandleToken(this.entity.GetPid());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {*/
                // respond with token
            tokenAvailable = false;
            locked = false;
            TOKENMSG tkn = new TOKENMSG();
            tkn.frompid = entity.GetPid();
            tkn.token = token;

            System.out.println("Sending2 token from" + this.entity.GetPid() +" to " + top[0]);
            MsgWrapper msg = new MsgWrapper();
            msg.msg = tkn;
            msg.msgid = MessageID.TOKEN;

            try {
                entity.SendMsg(msg, top[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            locked = false;
        }
        this.tokenLock.release();
        System.out.println("Exiting critical section" + entity.GetPid());
    }


    public void HandleToken(int frompid, Token token) throws InterruptedException {
        this.tokenLock.acquireUninterruptibly();
        this.token = token;
        this.tokenAvailable = true;

        this.tokenLock.release();
        // wake up request cs from here
        //System.out.println("token received from " + frompid + "in process"+ this.entity.GetPid());
        this.sem.release();
    }

    public void HandleRequestCs(int frompid, int seqno) {
        //System.out.println("handle request cs called for pid "+ frompid+" from pid "+ entity.GetPid());
        this.nlock.acquireUninterruptibly();

        if (seqno > rn[frompid]) {
            rn[frompid] = seqno;
        }

        //if (token && locked) {
        //  Integer [] req = new Integer[2];
        //    req[0] = frompid;
        //req[1] = seqno;
        //    tokenQueue.add(req);
        this.tokenLock.acquireUninterruptibly();
        if (tokenAvailable && !locked && (rn[frompid] == token.ln[frompid] + 1)) {
            //System.out.println(token+ " " +locked+ " "+ rn[frompid]+ " "+ ln[frompid] + "from pid"+ this.entity.GetPid());
            this.tokenAvailable = false;
            this.tokenLock.release();

            // respond with token
            TOKENMSG tkn = new TOKENMSG();
            tkn.frompid = entity.GetPid();
            tkn.token = token;

            MsgWrapper msg = new MsgWrapper();
            msg.msg = tkn;
            msg.msgid = MessageID.TOKEN;

            //System.out.println("responding with token for pid"+ frompid+ "from pid"+ this.entity.GetPid()+ " "+ token);

            try {
                entity.SendMsg(msg, frompid);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            this.tokenLock.release();
        }
        this.nlock.release();

    }
}


