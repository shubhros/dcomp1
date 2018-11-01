import java.net.*;
import java.io.*;
import java.util.Objects;


public class DProcess
{
    private int pid;
    private int maxpid;
    private ObjectOutputStream[] oos = new ObjectOutputStream[10];
    private ObjectInputStream[] ois = new ObjectInputStream[10];
    private DMutex dmutex;
    public DProcess(int id, int maxpid)
    {
        this.pid = id;
        this.maxpid = maxpid;
        //System.out.printf("process with id: %d created\n", pid);
        dmutex = new DMutex(this, maxpid);
    }

    public int GetPid()
    {
        return this.pid;
    }

    public void ExecuteCriticalSection() throws InterruptedException {
        Thread.sleep(30000);
    }

    public void StartServer()
    {
        new Thread() {
            public void run() {
                int lport = 50000 + pid;
                try {
                    ServerSocket server = new ServerSocket(lport);
                    //System.out.println("Server started for process "+ pid);
                    while(true) {
                        Socket socket = server.accept();
                        new Thread() {
                            public void run() {
                                try {
                                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                                    handleConnection(in, out);
                                } catch (Exception e) {
                                    System.out.println(e);
                                }
                            }
                        }.start();

                    }
                } catch(IOException e) {
                    System.out.println(e);
                }
            }
        }.start();
    }

    private void handleConnection(ObjectInputStream in, ObjectOutputStream out) {
        for (;;){
            // read the hello packet first
            try {
                MsgWrapper msg = (MsgWrapper) in.readObject();
                //System.out.println("received packet");
                if (msg.msgid == MessageID.HELLO) {
                    HelloPacket hello = (HelloPacket) msg.msg;
                    //System.out.println("Hello packet received from " + hello.pid + "in process "+ this.pid);
                    //this.oos[hello.pid] = out;
                } else if (msg.msgid == MessageID.REQUEST) {
                    REQUEST req = (REQUEST) msg.msg;
                    System.out.println("request message received from "+req.pid+" in process "+this.pid+" seq no "+req.seqno);
                    dmutex.HandleRequestCs(req.pid, req.seqno);
                } else if (msg.msgid == MessageID.TOKEN) {
                    TOKEN tkn = (TOKEN) msg.msg;
                    System.out.println("token received from "+tkn.frompid);
                    dmutex.HandleToken(tkn.frompid);
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public void connectToProcess(int tpid)
    {
        new Thread() {
            public void run() {
                try {
                    Socket socket = new Socket("127.0.0.1", 50000+tpid);
                    //System.out.println("process "+ pid + " connected to pid "+tpid);
                    ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    HelloPacket pkt = new HelloPacket();
                    pkt.pid = pid;
                    MsgWrapper msg = new MsgWrapper();
                    msg.msg = pkt;
                    msg.msgid = MessageID.HELLO;
                    os.writeObject(msg);
                    oos[tpid] = os;
                } catch(IOException e) {
                    System.out.println(e);
                }

            }
        }.start();
    }

    public void Broadcast(MsgWrapper msg) {
        for(ObjectOutputStream os: this.oos) {
            if (os != null) {
                try {
                    os.writeObject(msg);
                    //System.out.println("wrote to "+os);
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }

    }

    public void SendMsg(MsgWrapper msg, int to) throws IOException
    {
        oos[to].writeObject(msg);
    }

    public void RequestCs()
    {
        this.dmutex.RequestCs();
    }

    public void SetTokenOwner()
    {
        this.dmutex.SetTokenOwner();
    }
}
