//import kr.ac.konkuk.ccslab.cm.*;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.nio.file.Files;

public class CMServerApp extends JFrame {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;
    private boolean m_bRun;
    private Scanner m_scan = null;

    private int logicalClock;

    public int getLogicalClock() {
        return logicalClock;
    }

    public CMServerApp()
    {
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMServerEventHandler(m_serverStub);
        m_bRun = true;

    }
    public CMServerStub getServerStub()
    {
        return m_serverStub;
    }
    public CMServerEventHandler getServerEventHandler()
    {
        return m_eventHandler;
    }


    public void startTest() {
        System.out.println("Server application starts.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        m_scan = new Scanner(System.in);
        String strInput = null;
        int nCommand = -1;
        while (m_bRun) {
            System.out.println("Type \"0\" for menu.");
            System.out.print("> ");
            try {
                strInput = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            try {
                nCommand = Integer.parseInt(strInput);
            } catch (NumberFormatException e) {
                System.out.println("Incorrect command number!");
                continue;
            }

            switch (nCommand) {
                case 100:
                    startCM();
                    break;
                case 999:
                    terminateCM();
                    return;
                case 6: // print current login users
                    printLoginUsers();
                    break;
            }
        }
    }

    public void startCM()
    {
        boolean bRet = m_serverStub.startCM();
        if(!bRet)
        {
            System.err.println("CM initialization error!");
            return;
        }
        logicalClock = 0;
        startTest();
    }

    public void terminateCM()
    {
        m_serverStub.terminateCM();
        m_bRun = false;
    }
    public void printLoginUsers()
    {
        System.out.println("========== print login users");
        CMMember loginUsers = m_serverStub.getLoginUsers();
        if(loginUsers == null)
        {
            System.err.println("The login users list is null!");
            return;
        }

        System.out.println("Currently ["+loginUsers.getMemberNum()+"] users are online.");
        Vector<CMUser> loginUserVector = loginUsers.getAllMembers();
        Iterator<CMUser> iter = loginUserVector.iterator();
        int nPrintCount = 0;
        while(iter.hasNext())
        {
            CMUser user = iter.next();
            System.out.print("User: "+user.getName()+ ", Logical Clock: "+logicalClock);
            nPrintCount++;
            if((nPrintCount % 10) == 0)
            {
                System.out.println();
                nPrintCount = 0;
            }
        }
    }





    public static void main(String[] args) {
        CMServerApp server = new CMServerApp();
        CMServerStub cmStub = server.getServerStub();
        cmStub.setAppEventHandler(server.getServerEventHandler());
        server.startCM();

        System.out.println("Server application is terminated.");
    }
    //전송된걸 받으면 서버의 logical clock을 올리기(전송메세지에 담긴 클라이언트의 logical clock 값으로 맞추기)
}
