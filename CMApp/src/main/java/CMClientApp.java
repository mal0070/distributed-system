//import kr.ac.konkuk.ccslab.cm.*;
// kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class CMClientApp {
    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    private Scanner m_scan = null;
    public CMClientApp()
    {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
        m_bRun=true;
    }
    public CMClientStub getClientStub()
    {
        return m_clientStub;
    }
    public CMClientEventHandler getClientEventHandler()
    {
        return m_eventHandler;
    }

    public void startTest() {
        System.out.println("client application starts.");
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
                    testStartCM();
                    break;
                case 999:
                    testTerminateCM();
                    break;
                case 11: // synchronously login to default server
                    testSyncLoginDS();
                    break;
                case 10:
                    clientFilePush();
                    break;
                case 12: // logout from default server
                    testLogoutDS();
                    break;
                default:
                    System.err.println("Unknown command.");
                    break;
            }
        }
    }

    public void testSyncLoginDS() {
            String strUserName = null;
            String strPassword = null;
            CMSessionEvent loginAckEvent = null;
            Console console = System.console();

            System.out.println("====== login to default server");
            System.out.print("user name: ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                strUserName = br.readLine();
                if(console == null)
                {
                    System.out.print("password: ");
                    strPassword = br.readLine();
                }
                else
                    strPassword = new String(console.readPassword("password: "));
            } catch (IOException e) {
                e.printStackTrace();
            }

            loginAckEvent = m_clientStub.syncLoginCM(strUserName, strPassword);
            if(loginAckEvent != null)
            {
                // print login result
                if(loginAckEvent.isValidUser() == 0)
                {
                    System.err.println("This client fails authentication by the default server!");
                }
                else if(loginAckEvent.isValidUser() == -1)
                {
                    System.err.println("This client is already in the login-user list!");
                }
                else
                {
                    System.out.println("This client successfully logs in to the default server.");
                }
            }
            else
            {
                System.err.println("failed the login request!");
            }

            System.out.println("======");
        }
    public void testStartCM()
    {
        // get local address
        List<String> localAddressList = CMCommManager.getLocalIPList();
        if(localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }
        String strCurrentLocalAddress = localAddressList.get(0).toString();

        // set config home
        m_clientStub.setConfigurationHome(Paths.get("."));
        // set file-path home
        m_clientStub.setTransferedFileHome(m_clientStub.getConfigurationHome().resolve("client-file-path"));

        // get the saved server info from the server configuration file
        String strSavedServerAddress = null;
        int nSavedServerPort = -1;
        String strNewServerAddress = null;
        String strNewServerPort = null;

        strSavedServerAddress = m_clientStub.getServerAddress();
        nSavedServerPort = m_clientStub.getServerPort();

        // ask the user if he/she would like to change the server info
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("========== start CM");
        System.out.println("my current address: "+strCurrentLocalAddress);
        System.out.println("saved server address: "+strSavedServerAddress);
        System.out.println("saved server port: "+nSavedServerPort);

        try {
            System.out.print("new server address (enter for saved value): ");
            strNewServerAddress = br.readLine().trim();
            System.out.print("new server port (enter for saved value): ");
            strNewServerPort = br.readLine().trim();

            // update the server info if the user would like to do
            if(!strNewServerAddress.isEmpty() && !strNewServerAddress.equals(strSavedServerAddress))
                m_clientStub.setServerAddress(strNewServerAddress);
            if(!strNewServerPort.isEmpty() && Integer.parseInt(strNewServerPort) != nSavedServerPort)
                m_clientStub.setServerPort(Integer.parseInt(strNewServerPort));

        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean bRet = m_clientStub.startCM();
        if(!bRet)
        {
            System.err.println("CM initialization error!");
            return;
        }
        startTest();
    }
    public void testTerminateCM()
    {
        m_clientStub.terminateCM();
        m_bRun = false;
    }

    public void clientFilePush() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== select files to send: ");
        Path transferHome = m_clientStub.getTransferedFileHome();
        // open file chooser to choose files
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(transferHome.toFile());
        int fcRet = fc.showOpenDialog(null);
        if(fcRet != JFileChooser.APPROVE_OPTION) return;
        File[] files = fc.getSelectedFiles();

        for(File file : files)
            System.out.println("selected file = " + file);
        if(files.length < 1) {
            System.err.println("No file selected!");
            return;
        }

        // input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        System.out.println("For \"mlim\", you must run CMClientFile before the file transfer.");
        String receiver = scanner.nextLine().trim();

        // send files
        for(File file : files)
            m_clientStub.pushFile(file.getPath(), receiver);
    }
    public void testLogoutDS()
    {
        boolean bRequestResult = false;
        System.out.println("====== logout from default server");
        bRequestResult = m_clientStub.logoutCM();
        if(bRequestResult)
            System.out.println("successfully sent the logout request.");
        else
            System.err.println("failed the logout request!");
        System.out.println("======");
    }

    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.testStartCM();

        System.out.println("Client application is terminated.");
        }
}


