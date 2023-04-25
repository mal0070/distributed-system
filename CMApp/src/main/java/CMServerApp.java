//import kr.ac.konkuk.ccslab.cm.*;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class CMServerApp {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;
    private boolean m_bRun;
    private Scanner m_scan = null;

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
        // get current server info from the server configuration file
        String strSavedServerAddress = null;
        List<String> localAddressList = null;
        int nSavedServerPort = -1;
        String strNewServerAddress = null;
        String strNewServerPort = null;
        int nNewServerPort = -1;

        // set config home
        m_serverStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        m_serverStub.setTransferedFileHome(m_serverStub.getConfigurationHome().resolve("server-file-path"));

        localAddressList = CMCommManager.getLocalIPList();
        if(localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }
        strSavedServerAddress = m_serverStub.getServerAddress();
        nSavedServerPort = m_serverStub.getServerPort();

        // ask the user if he/she would like to change the server info
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("========== start CM");
        System.out.println("my current address: "+localAddressList.get(0).toString());
        System.out.println("saved server address: "+strSavedServerAddress);
        System.out.println("saved server port: "+nSavedServerPort);

        try {
            System.out.print("new server address (enter for saved value): ");
            strNewServerAddress = br.readLine().trim();
            if(strNewServerAddress.isEmpty()) strNewServerAddress = strSavedServerAddress;

            System.out.print("new server port (enter for saved value): ");
            strNewServerPort = br.readLine().trim();
            try {
                if(strNewServerPort.isEmpty())
                    nNewServerPort = nSavedServerPort;
                else
                    nNewServerPort = Integer.parseInt(strNewServerPort);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return;
            }

            // update the server info if the user would like to do
            if(!strNewServerAddress.equals(strSavedServerAddress))
                m_serverStub.setServerAddress(strNewServerAddress);
            if(nNewServerPort != nSavedServerPort)
                m_serverStub.setServerPort(Integer.parseInt(strNewServerPort));

        } catch (IOException e) {
            e.printStackTrace();
        }


        boolean bRet = m_serverStub.startCM();
        if(!bRet)
        {
            System.err.println("CM initialization error!");
            return;
        }
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
            System.out.print(user.getName()+" ");
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
        cmStub.startCM();

        System.out.println("Server application is terminated.");
    }
}
