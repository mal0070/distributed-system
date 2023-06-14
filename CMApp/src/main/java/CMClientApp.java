//import kr.ac.konkuk.ccslab.cm.*;
// kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.*;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;
import java.nio.file.FileSystems;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;

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


    private Map<String, Integer> fileVersions;

    private CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    private Scanner m_scan = null;

    private int logicalClock;

    private JTextArea fileTextArea;

    // 변수 선언
    private boolean watchRunning = false;
    private Thread watchThread;


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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
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
                    myFileEventWatch();
                    break;
                case 14:
                    shareAndPushFiles();
                    break;
                default:
                    System.err.println("Unknown command.");
                    break;
            }
        }
    }

    public void createAndShowGUI() {
        JFrame frame = new JFrame("CM Client Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create buttons
        JButton startButton = new JButton("Start CM");
        //JButton terminateButton = new JButton("Terminate CM");
        JButton stopButton = new JButton("Stop Watch");
        JButton syncLoginButton = new JButton("Sync Login");
        JButton filePushButton = new JButton("Push File");
        JButton logoutButton = new JButton("Logout");
        JButton watchButton = new JButton("Watch Files");
        JButton shareAndPushButton = new JButton("Share & Push Files");

        // Create file text area
        fileTextArea = new JTextArea();
        fileTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(fileTextArea);

        // Set button positions
        startButton.setBounds(10, 10, 150, 30);
        //terminateButton.setBounds(170, 10, 150, 30);
        syncLoginButton.setBounds(10, 50, 150, 30);
        filePushButton.setBounds(170, 50, 150, 30);
        logoutButton.setBounds(10, 90, 150, 30);
        watchButton.setBounds(170, 90, 150, 30);
        stopButton.setBounds(170, 10, 150, 30);
        shareAndPushButton.setBounds(10, 130, 150, 30);
        scrollPane.setBounds(330, 10, 200, 150); // Set the position and size of the file text area

        // Add buttons and file text area to the frame
        frame.getContentPane().setLayout(null);
        frame.getContentPane().add(startButton);
        frame.getContentPane().add(stopButton);
        frame.getContentPane().add(syncLoginButton);
        frame.getContentPane().add(filePushButton);
        frame.getContentPane().add(logoutButton);
        frame.getContentPane().add(watchButton);
        frame.getContentPane().add(shareAndPushButton);
        frame.getContentPane().add(scrollPane);

        // Add button event listeners
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testStartCM();
            }
        });

        /*terminateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testTerminateCM();
            }
        });*/

        syncLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testSyncLoginDS();
            }
        });

        filePushButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientFilePush();
                // Update the file text area after pushing the file
                updateFileTextArea();
            }
        });

        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testLogoutDS();
            }
        });

        watchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myFileEventWatch(); //버튼 클릭하면 파일 감시 이벤트 시작
            }
        });

// 종료 버튼에 이벤트 리스너 등록
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 버튼이 클릭되면 파일 감시 스레드 중지
                stopFileEventWatch();
            }
        });

        shareAndPushButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shareAndPushFiles();
                // Update the file text area after sharing and pushing files
                updateFileTextArea();
            }
        });

        // Set frame properties
        frame.setSize(550, 210);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // 파일 감시 스레드 시작
    private void startFileEventWatch() {
        // 이미 실행 중인 경우 중복 실행 방지
        if (watchRunning) {
            return;
        }

        watchRunning = true;

        // 파일 감시 스레드 시작
        watchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 파일 감시 작업 수행
                myFileEventWatch();

                // 파일 감시가 종료되면 watchRunning 플래그 변경
                watchRunning = false;
            }
        });

        watchThread.start();
    }

    // 파일 감시 스레드 중지
    private void stopFileEventWatch() {
        // 실행 중인 파일 감시 스레드가 있는지 확인
        if (watchThread != null && watchThread.isAlive()) {
            // 파일 감시 스레드 중지
            watchThread.interrupt();
        }
    }

    public void testSyncLoginDS() {
        String strUserName = null;
        String strPassword = null;
        CMSessionEvent loginAckEvent = null;

        // Show input dialogs for user name and password
        strUserName = JOptionPane.showInputDialog("User name:");
        strPassword = JOptionPane.showInputDialog("Password:");

        loginAckEvent = m_clientStub.syncLoginCM(strUserName, strPassword);
        if (loginAckEvent != null) {
            // Handle login result
            if (loginAckEvent.isValidUser() == 0) {
                JOptionPane.showMessageDialog(null, "This client fails authentication by the default server!");
            } else if (loginAckEvent.isValidUser() == -1) {
                JOptionPane.showMessageDialog(null, "This client is already in the login-user list!");
            } else {
                JOptionPane.showMessageDialog(null, "This client successfully logs in to the default server.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Failed to send the login request!");
        }
        /*
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

        System.out.println("======");*/
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
            JOptionPane.showMessageDialog(null, "No file selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /*/ input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        System.out.println("For \"mlim\", you must run CMClientFile before the file transfer.");
        String receiver = scanner.nextLine().trim();*/

        String receiver = JOptionPane.showInputDialog("Receiver of files:\n\n" +
                "Type \"SERVER\" for the server or \"mlim\" for client receiver.\n" +
                "For \"mlim\", you must run CMClientFile before the file transfer.");

        CMServerApp serverApp = new CMServerApp();
        int serverLogicalClock = serverApp.getLogicalClock();
        int clientLogicalClock = logicalClock;

        // send files
        for (File file : files) {
            if (serverLogicalClock <= clientLogicalClock) {
                JOptionPane.showMessageDialog(null, "Client's logical clock is ahead of the server's clock (or same).\nYou can push to the server.",
                        "inform", JOptionPane.INFORMATION_MESSAGE);
                //System.out.println("Client's logical clock is ahead of the server's clock.");
                m_clientStub.pushFile(file.getPath(), receiver, CMInfo.FILE_OVERWRITE);
                serverLogicalClock = clientLogicalClock;

            }else{
                JOptionPane.showMessageDialog(null, "Server's logical clock is ahead of the client's clock.\nYou cannot push to the server.",
                        "inform", JOptionPane.ERROR_MESSAGE);
                break;
                //System.out.println("Server's logical clock is ahead of the client's clock. You cannot push to Server.");
            }
            return;
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

    public void myFileEventWatch() {

        //Path 객체 생성 -> 감시할 디렉토리 지정
        Path watchPath = Paths.get("/Users/imina/Desktop/학교/분산시스템/distributed-system/CMApp/client-file-path");
        try {
            //WatchService 객체 생성
            WatchService watchService = FileSystems.getDefault().newWatchService();

            //WatchService에 Path 등록하고, 감시할 이벤트 유형 지정
            watchPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            JOptionPane.showMessageDialog(null,"Watch service started for directory: " + watchPath);

           Scanner scanner = new Scanner(System.in);

            while (true) {
                WatchKey key; //변경사항이 감지되면 반환할 객체: 변경된 파일/디렉토리의 경로, 이벤트 등의 정보 포함
                try {
                    key = watchService.take(); // take(): 변경된 이벤트가 발생할 때까지 blocked
                } catch (InterruptedException e) {
                    System.err.println("Error while waiting for watch events: " + e.getMessage());
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) { //pollEvent(): WatchEvent의 리스트 가져옴
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue; // 오버플로우 이벤트는 무시
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) { //파일 생성
                        //생성된 파일의 경로 추출
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchPath.resolve(filePath);
                        File createdFile = absolutePath.toFile();
                        logicalClock++;
                        // 파일을 서버로 전송 : 파일이 생성되었는지 컴퓨터 내에서 확인하기 위해 작성하였다.
                        m_clientStub.pushFile(createdFile.getPath(), "mlim");

                        // 생성된 파일 정보를 출력
                        JOptionPane.showMessageDialog(null,"Created file: " + createdFile + logicalClock++);
                       // System.out.println("Created file: " + createdFile + logicalClock++);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // 수정된 파일의 경로를 추출
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchPath.resolve(filePath);
                        File modifiedFile = absolutePath.toFile();
                        logicalClock++;
                        // 수정된 파일 정보를 출력
                        JOptionPane.showMessageDialog(null,"Modified file: " + modifiedFile + logicalClock);
                       // System.out.println("Modified file: " + modifiedFile + logicalClock);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        // 삭제된 파일의 경로를 추출
                        Path filePath = ((WatchEvent<Path>) event).context();
                        Path absolutePath = watchPath.resolve(filePath);
                        File deletedFile = absolutePath.toFile();
                        logicalClock++;
                        // 삭제된 파일 정보를 출력
                        JOptionPane.showMessageDialog(null,"Deleted file: " + deletedFile + logicalClock);
                        //System.out.println("Deleted file: " + deletedFile + logicalClock);
                    }
                    // 변경사항 처리 후 WatchKey를 초기화하여 다음 변경사항을 감지할 수 있도록 준비
                    key.reset();

                }

                // 변경사항 처리 후 WatchKey를 초기화하여 다음 변경사항을 감지할 수 있도록 준비
               // key.reset();

               if (scanner.hasNextLine()) {
                    String userInput = scanner.nextLine();

                    // 루프 종료 조건
                    if (userInput.equals("stop watch")) {
                        break;
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
            JOptionPane.showMessageDialog(null, "No file selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        /*/ Input file receiver
        System.out.println("Receiver of files: ");
        System.out.println("Type \"SERVER\" for the server or \"mlim\" for client receiver.");
        System.out.println("For \"mlim\", you must run CMClientFile before the file transfer.");
        String receiver = scanner.nextLine().trim();*/
        String receiver = JOptionPane.showInputDialog("Receiver of files:\n\n" +
                "Type \"SERVER\" for the server or \"mlim\" for client receiver.\n" +
                "For \"mlim\", you must run CMClientFile before the file transfer.");

        // Share files and send file transfer requests
        for (File file : files) {
            System.out.println("Selected file: " + file);

            // Share file
            if (receiver.equalsIgnoreCase("SERVER")) {
                System.out.println("Sharing file with the server...");
                m_clientStub.pushFile(file.getPath(), receiver, CMInfo.FILE_OVERWRITE);
            } else if (receiver.equalsIgnoreCase("mlim")) {
                //System.out.println("Enter the client IDs to share the file with (separated by commas):");
                String clientIDs = JOptionPane.showInputDialog("Enter the client IDs to share the file with (separated by commas):");
                String[] clientIDArray = clientIDs.split(",");
                    for (String clientID : clientIDArray) {
                        System.out.println("Sharing file with client: " + clientID.trim());
                        m_clientStub.pushFile(file.getPath(), clientID.trim(), CMInfo.FILE_OVERWRITE);
                    }
                } else{
                    JOptionPane.showMessageDialog(null, "Invalid receiver specified!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
        }

        JOptionPane.showMessageDialog(null, "File sharing and sending completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        //System.out.println("File sharing and sending completed successfully!");
    }

    public void updateFileTextArea() {
        StringBuilder sb = new StringBuilder();

        for (String client : sharedClients) {
            sb.append("[").append(client).append("]\n");
            List<String> clientFiles = getClientFiles(client);
            for (String file : clientFiles) {
                sb.append("- ").append(file).append("\n");
            }
            sb.append("\n");
        }

        fileTextArea.setText(sb.toString());
    }

    public List<String> getClientFiles(String clientName) {
        List<String> clientFiles = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : fileVersions.entrySet()) {
            String file = entry.getKey();
            int version = entry.getValue();
            String[] tokens = file.split("/");
            String owner = tokens[0];

            if (owner.equals(clientName)) {
                clientFiles.add(file + " (v" + version + ")");
            }
        }
        return clientFiles;
    }


    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.testStartCM();

        System.out.println("Client application is terminated.");
    }
}



