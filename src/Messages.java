import java.io.Serializable;

enum MessageID
{
    HELLO, REQUEST, TOKEN, RELEASE;
}


class MsgWrapper implements Serializable {
    public MessageID msgid;
    public Object msg;
}

class HelloPacket implements Serializable {
    int pid;
}

class HiPacket implements Serializable {
    int pid;
    String message;
}

// Request and reply messages

class REQUEST implements Serializable
{
    int pid;
    int seqno;
}

class TOKEN implements Serializable
{
    int frompid;
}