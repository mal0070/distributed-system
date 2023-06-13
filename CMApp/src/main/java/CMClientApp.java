//import kr.ac.konkuk.ccslab.cm.*;
// kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Scanner;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CMClientApp {
    private List<String> sharedClients;

    // 추가: 파일과 해당 파일의 논리 시계 값을 저장하는 맵
    private Map<String, Integer> fileVersions;

    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    private Scanner m_scan = null;

    private int logicalClock;

    private JFrame frame;
    private JButton menuButton;
    private JButton startButton;
    private JButton terminateButton;
    private JButton loginButton;
    private JButton filePushButton;
    private JButton detectUpdateButton;
    private JButton compareClocksButton;
    private JButton deleteFileButton;
    private JButton fileShareButton;

    public void compareLogicalClocks() {
        CMServerApp serverApp = new CMServerApp();
        int serverLogicalClock = serverApp.getLogicalClock();
        int clientLogicalClock = logicalClock;

        if (serverLogicalClock > clientLogicalClock) {
            System.out.println("Server's logical clock is ahead of the client's clock.");
        } else if (serverLogicalClock < clientLogicalClock) {
            System.out.println("Client's logical clock is ahead of the server's clock.");
        } else {
            System.out.println("The logical clocks of the server and client are synchronized.");
        }
    } // 얘를 file push 안에 넣자


    public CMClientApp() {
        // 생성자에서 초기화
        fileVersions = new HashMap<>();

        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
        m_bRun = true;

        sharedClients = new ArrayList<>();

        frame = new JFrame("CM Client Application");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menuButton = new JButton("Menu");
        startButton = new JButton("Start CM");
        terminateButton = new JButton("Terminate CM");
        loginButton = new JButton("Login");
        filePushButton = new JButton("File Push");
        detectUpdateButton = new JButton("Detect Update");
        compareClocksButton = new JButton("Compare Clocks");
        deleteFileButton = new JButton("Delete File");
        fileShareButton = new JButton("File Share");

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(menuButton);
        panel.add(startButton);
        panel.add(terminateButton);
        panel.add(loginButton);
        panel.add(filePushButton);
        panel.add(detectUpdateButton);
        panel.add(compareClocksButton);
        panel.add(deleteFileButton);
        panel.add(fileShareButton);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

    }

    public CMClientStub getClientStub() {
        return m_clientStub;
    }

    public CMClientEventHandler getClientEventHandler() {
        return m_eventHandler;
    }


    public void startTest() {
        System.out.println("client application starts.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        m_scan = new Scanner(System.in);
        String strInput = null;
        int nCommand = -1;

        // Create ActionListeners for buttons
        ActionListener menuButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle menu button click
                System.out.println("Menu button clicked!");
            }
        };

        ActionListener startButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle start button click
                System.out.println("Start button clicked!");
                testStartCM();
            }
        };

        ActionListener terminateButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle terminate button click
                System.out.println("Terminate button clicked!");
                testTerminateCM();
            }
        };

        ActionListener loginButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle login button click
                System.out.println("Login button clicked!");
                testSyncLoginDS();
            }
        };

        ActionListener filePushButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle file push button click
                System.out.println("File Push button clicked!");
                clientFilePush();
            }
        };

        ActionListener detectUpdateButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Handle detect update button click
                System.out.println("Detect Update button clicked!");
                startWatchService();
            }
        };

        // Set ActionListeners to corresponding buttons

        menuButton.addActionListener(menuButtonListener);
        startButton.addActionListener(startButtonListener);
        terminateButton.addActionListener(terminateButtonListener);
        loginButton.addActionListener(loginButtonListener);
        filePushButton.addActionListener(filePushButtonListener);
        detectUpdateButton.addActionListener(detectUpdateButtonListener);

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
                case 12:
                    clientFilePush();
                    break;
                case 10: // logout from default server
                    testLogoutDS();
                    break;
                case 1:
                    startWatchService();
                    break;
                case 2:
                    compareLogicalClocks();
                    break;
                case 4:
                    fileShare();
                    break;
                case 3:
                    deleteFile();;
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
            if (console == null) {
                System.out.print("password: ");
                strPassword = br.readLine();
            } else
                strPassword = new String(console.readPassword("password: "));
        } catch (IOException e) {
            e.printStackTrace();
        }

        loginAckEvent = m_clientStub.syncLoginCM(strUserName, strPassword);
        if (loginAckEvent != null) {
            // print login result
            if (loginAckEvent.isValidUser() == 0) {
                System.err.println("This client fails authentication by the default server!");
            } else if (loginAckEvent.isValidUser() == -1) {
                System.err.println("This client is already in the login-user list!");
            } else {
                System.out.println("This client successfully logs in to the default server.");
            }
        } else {
            System.err.println("failed the login request!");
        }

        System.out.println("======");
    }

    public void testStartCM() {
        boolean bRet = m_clientStub.startCM();
        if (!bRet) {
            System.err.println("CM initialization error!");
            return;
        }
        logicalClock = 0;

        startTest();
    }

    public void testTerminateCM() {
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
        if (fcRet != JFileChooser.APPROVE_OPTION) return;
        File[] files = fc.getSelectedFiles();

        for (File file : files)
            System.out.println("selected file = " + file);
        if (files.length < 1) {
            System.err.println("No file selected!");
            return;
        }

        // input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        System.out.println("For \"mlim\", you must run CMClientFile before the file transfer.");
        String receiver = scanner.nextLine().trim();

        // send files
        for (File file : files) {

            CMServerApp serverApp = new CMServerApp();
            int serverLogicalClock = serverApp.getLogicalClock();
            int clientLogicalClock = logicalClock;

            if (serverLogicalClock <= clientLogicalClock) {
                System.out.println("Client's logical clock is ahead of the server's clock.");
                m_clientStub.pushFile(file.getPath(), receiver, CMInfo.FILE_OVERWRITE);
                serverLogicalClock = clientLogicalClock;

            }
        }

    }

    public void testLogoutDS() {
        boolean bRequestResult = false;
        System.out.println("====== logout from default server");
        bRequestResult = m_clientStub.logoutCM();
        if (bRequestResult)
            System.out.println("successfully sent the logout request.");
        else
            System.err.println("failed the logout request!");
        System.out.println("======");
    }

    public void startWatchService() {
        Path watchDir = Paths.get("/Users/imina/Desktop/학교/분산시스템/distributed-system/CMApp/client-file-path"); // 감시할 디렉토리 경로를 지정합니다.
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            System.out.println("Watch service started for directory: " + watchDir);

            // 사용자 입력을 받기 위한 Scanner 객체 생성
            Scanner scanner = new Scanner(System.in);
            // WatchService를 계속해서 실행합니다.
            while (true) {
                WatchKey key;
                try {
                    key = watchService.take(); // 변경된 이벤트가 발생할 때까지 대기합니다.
                } catch (InterruptedException e) {
                    System.err.println("Error while waiting for watch events: " + e.getMessage());
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // 변경된 이벤트의 종류를 확인합니다.
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue; // 오버플로우 이벤트는 무시합니다.
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        // 생성된 파일의 경로를 추출합니다.
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchDir.resolve(filePath);
                        File createdFile = absolutePath.toFile();
                        logicalClock++;
                        // 파일을 서버로 전송합니다.
                        m_clientStub.pushFile(createdFile.getPath(), "mlim");

                        // 생성된 파일 정보를 출력합니다.
                        System.out.println("Created file: " + createdFile + logicalClock++);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // 수정된 파일의 경로를 추출합니다.
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchDir.resolve(filePath);
                        File modifiedFile = absolutePath.toFile();
                        logicalClock++;
                        // 수정된 파일 정보를 출력합니다.
                        System.out.println("Modified file: " + modifiedFile + logicalClock);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        // 삭제된 파일의 경로를 추출합니다.
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchDir.resolve(filePath);
                        File deletedFile = absolutePath.toFile();
                        logicalClock++;
                        // 삭제된 파일 정보를 출력합니다.
                        System.out.println("Deleted file: " + deletedFile + logicalClock);
                    }
                }

                // WatchKey를 초기화하고 계속해서 감시를 수행합니다.
                key.reset();
                // 사용자 입력을 체크합니다.
                if (scanner.hasNextLine()) {
                    String userInput = scanner.nextLine();

                    // 종료 조건을 확인합니다.
                    if (userInput.equals("stop")) {
                        break;  // 루프를 종료합니다.
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting watch service: " + e.getMessage());
        }
    }

    public void fileShare() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Select file to share: ");
        Path transferHome = m_clientStub.getTransferedFileHome();

        // Open file chooser to choose a file
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(transferHome.toFile());
        int fcRet = fc.showOpenDialog(null);
        if (fcRet != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToShare = fc.getSelectedFile();
        if (fileToShare == null || !fileToShare.exists()) {
            System.err.println("Selected file does not exist!");
            return;
        }

        // Input clients to share the file with
        System.out.println("Enter the client IDs to share the file with (separated by commas):");
        String clientIDs = scanner.nextLine().trim();
        String[] clientIDArray = clientIDs.split(",");
        for (String clientID : clientIDArray) {
            sharedClients.add(clientID.trim());
        }

        // Send file sharing request to selected clients
        for (String clientID : sharedClients) {
            m_clientStub.pushFile(fileToShare.getPath(), clientID, CMInfo.FILE_OVERWRITE);
        }

        System.out.println("File shared successfully!");
    }

    public void deleteFile() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Select a file to delete: ");
        Path transferHome = m_clientStub.getTransferedFileHome();

        // Open file chooser to choose a file
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(transferHome.toFile());
        int fcRet = fc.showOpenDialog(null);
        if (fcRet != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToDelete = fc.getSelectedFile();
        if (fileToDelete == null || !fileToDelete.exists()) {
            System.err.println("Selected file does not exist!");
            return;
        }

//        // Confirm file deletion
//        System.out.println("Are you sure you want to delete the file? (Y/N)");
//        String confirm = scanner.nextLine().trim();
//        if (!confirm.equalsIgnoreCase("Y")) {
//            System.out.println("File deletion canceled.");
//            return;
//        }

        // Delete the file
        boolean deleted = fileToDelete.delete();
        if (deleted) {
            System.out.println("File deleted successfully: " + fileToDelete.getAbsolutePath());
        } else {
            System.err.println("Failed to delete the file: " + fileToDelete.getAbsolutePath());
        }
    }

/*
    public void fileShare() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== select files to send: ");
        Path transferHome = m_clientStub.getTransferedFileHome();
        // open file chooser to choose files
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(transferHome.toFile());
        int fcRet = fc.showOpenDialog(null);
        if (fcRet != JFileChooser.APPROVE_OPTION) return;
        File[] files = fc.getSelectedFiles();

        for (File file : files)
            System.out.println("selected file = " + file);
        if (files.length < 1) {
            System.err.println("No file selected!");
            return;
        }

        // input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        String receiver = scanner.nextLine().trim();

        // send files
        for (File file : files) {
            // 클라이언트가 자신의 서버로 파일을 전송
            m_clientStub.pushFile(file.getPath(), receiver, CMInfo.FILE_OVERWRITE);

            // 서버로부터 파일을 전송받을 클라이언트 목록을 얻어옴
            List<String> serverClients = m_clientStub.getServerInfo().getGroup("CLIENT").getMemberList();
            serverClients.remove(receiver); // 자신을 제외한 다른 클라이언트들만 남김

            // 다른 클라이언트의 서버로 파일을 전송
            for (String client : serverClients) {
                m_clientStub.pushFile(file.getPath(), client, CMInfo.FILE_OVERWRITE);
            }
        }

    }*/







    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.testStartCM();

        System.out.println("Client application is terminated.");
    }
}



