//import kr.ac.konkuk.ccslab.cm.*;
// kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import javax.swing.*;
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
                case 13:
                    startWatchService();
                    break;
                case 14:
                    compareLogicalClocks();
                    break;
                case 15:
                    shareAndPushFiles();
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



    public void shareAndPushFiles() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Select files to share or send: ");
        Path transferHome = m_clientStub.getTransferedFileHome();

        // Open file chooser to choose files
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(transferHome.toFile());
        int fcRet = fc.showOpenDialog(null);
        if (fcRet != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = fc.getSelectedFiles();
        if (files.length < 1) {
            System.err.println("No file selected!");
            return;
        }

        // Input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        System.out.println("For \"mlim\", you must run CMClientFile before the file transfer.");
        String receiver = scanner.nextLine().trim();

        // Share files and send file transfer requests
        for (File file : files) {
            System.out.println("Selected file: " + file);

            // Share file
            if (receiver.equalsIgnoreCase("SERVER")) {
                System.out.println("Sharing file with the server...");
                m_clientStub.pushFile(file.getPath(), receiver, CMInfo.FILE_OVERWRITE);
            } else if (receiver.equalsIgnoreCase("mlim")) {
                System.out.println("Enter the client IDs to share the file with (separated by commas):");
                String clientIDs = scanner.nextLine().trim();
                String[] clientIDArray = clientIDs.split(",");
                for (String clientID : clientIDArray) {
                    System.out.println("Sharing file with client: " + clientID.trim());
                    m_clientStub.pushFile(file.getPath(), clientID.trim(), CMInfo.FILE_OVERWRITE);
                }
            } else {
                System.err.println("Invalid receiver specified!");
                return;
            }
        }

        System.out.println("File sharing and sending completed successfully!");
    }

    }


    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.testStartCM();

        System.out.println("Client application is terminated.");
    }
}



