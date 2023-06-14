import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub clientStub;

    public CMClientEventHandler(CMClientStub clientStub) {
        this.clientStub = clientStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            case CMInfo.CM_DATA_EVENT:
                processDataEvent(cme);
                break;
            case CMInfo.CM_FILE_EVENT:
                processFileEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processDataEvent(CMEvent cme) {
        CMDataEvent de = (CMDataEvent) cme;
        switch(de.getID()) {
            case CMDataEvent.NEW_USER:
                String newUserMsg = "--> [" + de.getUserName() + "] enters group(" +
                        de.getHandlerGroup() + ") in session(" + de.getHandlerSession() + ").";
                JOptionPane.showMessageDialog(null, newUserMsg, "New User", JOptionPane.INFORMATION_MESSAGE);
                // System.out.println(newUserMsg);
                break;
            case CMDataEvent.REMOVE_USER:
                String removeUserMsg = "--> [" + de.getUserName() + "] leaves group(" +
                        de.getHandlerGroup() + ") in session(" + de.getHandlerSession() + ").";
                JOptionPane.showMessageDialog(null, removeUserMsg, "Remove User", JOptionPane.INFORMATION_MESSAGE);
                // System.out.println(removeUserMsg);
                break;
            case CMDataEvent.INHABITANT:
                String inhabitantMsg = "[" + de.getUserName() + "] is added to group: " +
                        de.getHandlerSession() + "(" + de.getHandlerGroup() + ")";
                JOptionPane.showMessageDialog(null, inhabitantMsg, "Inhabitant", JOptionPane.INFORMATION_MESSAGE);
                // System.out.println(inhabitantMsg);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch(se.getID()) {
            case CMSessionEvent.LOGIN_ACK:
                if(se.isValidUser() == 0) {
                    JOptionPane.showMessageDialog(null, "This client fails authentication by the default server!", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    // System.err.println("--> This client fails authentication by the default server!");
                } else if(se.isValidUser() == -1) {
                    JOptionPane.showMessageDialog(null, "This client is already in the login-user list!", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    // System.err.println("--> This client is already in the login-user list!");
                } else {
                    JOptionPane.showMessageDialog(null, "This client successfully logs in to the default server.", "Login Success", JOptionPane.INFORMATION_MESSAGE);
                    // System.out.println("--> This client successfully logs in to the default server.");
                }
                break;
            case CMSessionEvent.SESSION_REMOVE_USER:
                String removeUserMsg = "[" + se.getUserName() + "] left session";
                JOptionPane.showMessageDialog(null, removeUserMsg, "User Left Session", JOptionPane.INFORMATION_MESSAGE);
                // System.out.println("["+se.getUserName()+"] left session");
            default:
                return;
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        int nOption = -1;
        switch(fe.getID()) {
            case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
                String strReq = "[" + fe.getFileReceiver() + "] requests file(" + fe.getFileName() + ").\n";
                System.out.print(strReq);
                nOption = JOptionPane.showConfirmDialog(null, strReq, "Request a file", JOptionPane.YES_NO_OPTION);
                if(nOption == JOptionPane.YES_OPTION) {
                    clientStub.replyEvent(fe, 1);
                } else {
                    clientStub.replyEvent(fe, 0);
                }
                break;
            case CMFileEvent.REPLY_PERMIT_PULL_FILE:
                if(fe.getReturnCode() == -1) {
                    JOptionPane.showMessageDialog(null, "[" + fe.getFileName() + "] does not exist in the owner!", "File Not Found", JOptionPane.ERROR_MESSAGE);
                    // System.err.print("["+fe.getFileName()+"] does not exist in the owner!\n");
                } else if(fe.getReturnCode() == 0) {
                    JOptionPane.showMessageDialog(null, "[" + fe.getFileSender() + "] rejects to send file(" +
                            fe.getFileName() + ").", "File Transfer Rejected", JOptionPane.ERROR_MESSAGE);
                    // System.err.print("["+fe.getFileSender()+"] rejects to send file(" + fe.getFileName() + ").\n");
                }
                break;
            case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
                StringBuffer strReqBuf = new StringBuffer();
                strReqBuf.append("["+fe.getFileSender()+"] wants to send a file.\n");
                strReqBuf.append("file path: "+fe.getFilePath()+"\n");
                strReqBuf.append("file size: "+fe.getFileSize()+"\n");
                System.out.print(strReqBuf.toString());
                nOption = JOptionPane.showConfirmDialog(null, strReqBuf.toString(),
                        "Push File", JOptionPane.YES_NO_OPTION);
                if(nOption == JOptionPane.YES_OPTION) {
                    clientStub.replyEvent(fe, 1);
                } else {
                    clientStub.replyEvent(fe, 1);
                }
                break;
            case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
                if(fe.getReturnCode() == 0) {
                    JOptionPane.showMessageDialog(null, "[" + fe.getFileReceiver() + "] rejected the push-file request!\n" +
                            "file path("+fe.getFilePath()+"), size("+fe.getFileSize()+").", "File Transfer Rejected", JOptionPane.ERROR_MESSAGE);
                    // System.err.print("["+fe.getFileReceiver()+"] rejected the push-file request!\n");
                    // System.err.print("file path("+fe.getFilePath()+"), size("+fe.getFileSize()+").\n");
                }
                break;
        }
    }
}
