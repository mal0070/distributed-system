//import kr.ac.konkuk.ccslab.cm.*;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;
    private int m_nCheckCount;	// for internal forwarding simulation
    private boolean m_bDistFileProc;	// for distributed file processing
    public CMServerEventHandler(CMServerStub serverStub)
    {
        m_serverStub = serverStub;
        m_nCheckCount = 0;
        m_bDistFileProc = false;
    }
    @Override
    public void processEvent(CMEvent cme) {

        switch(cme.getType())
        {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_FILE_EVENT:
                processFileEvent(cme);
                break;
            default:
                return;
        }
    }
    private void processSessionEvent(CMEvent cme)
    {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID())
        {
            case CMSessionEvent.LOGIN:
                System.out.println("["+se.getUserName()+"] requests login.");
                break;
            case CMSessionEvent.CHANGE_SESSION:
                System.out.println("["+se.getUserName()+"] changes to session("+se.getSessionName()+").");
                break;
            default:
                return;
        }
    }
    private void processFileEvent(CMEvent cme)
    {
        CMFileEvent fe = (CMFileEvent) cme;
        switch(fe.getID())
        {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                System.out.println("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+").");
                System.err.print("["+fe.getFileReceiver()+"] requests file("+fe.getFileName()+").\n");
                System.err.print("The pull-file request is not automatically permitted!\n");
                System.err.print("To change to automatically permit the pull-file request, \n");
                System.err.print("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
                break;
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if(fe.getReturnCode() == -1)
                {
                    System.err.print("["+fe.getFileName()+"] does not exist in the owner!\n");
                }
                else if(fe.getReturnCode() == 0)
                {
                    System.err.print("["+fe.getFileSender()+"] rejects to send file("
                            +fe.getFileName()+").\n");
                }
                break;
            case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
                System.out.println("["+fe.getFileSender()+"] wants to send a file("+fe.getFilePath()+
                        ").");
                System.err.print("The push-file request is not automatically permitted!\n");
                System.err.print("To change to automatically permit the push-file request, \n");
                System.err.print("set the PERMIT_FILE_TRANSFER field to 1 in the cm-server.conf file\n");
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                if(fe.getReturnCode() == 0)
                {
                    System.err.print("["+fe.getFileReceiver()+"] rejected the push-file request!\n");
                    System.err.print("file path("+fe.getFilePath()+"), size("+fe.getFileSize()+").\n");
                }
                break;
            case CMFileEvent.CANCEL_FILE_SEND:
            case CMFileEvent.CANCEL_FILE_SEND_CHAN:
                System.out.println("["+fe.getFileSender()+"] cancelled the file transfer.");
                break;
            case CMFileEvent.CANCEL_FILE_RECV_CHAN:
                System.out.println("["+fe.getFileReceiver()+"] cancelled the file request.");
                break;
        }
        return;
    }
}
