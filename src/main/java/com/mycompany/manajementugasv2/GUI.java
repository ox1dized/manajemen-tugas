package com.mycompany.manajementugasv2;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.proteanit.sql.DbUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class GUI extends javax.swing.JFrame {

    private Statement St;
    private PreparedStatement Pst;
    private Connection Con;
    private ResultSet Rs;
    private String sql = "";
    private final String BOT_TOKEN = "6834224537:AAEiequHvfNyN4YsvOO-zUEu9gGhfvXGvvA";
    private final String CHAT_ID = "5039553958";
    private TelegramBot telegramBot;
    ImageIcon logo = new ImageIcon("logo.png");
    public GUI() {
        initComponents();
        KoneksiDatabase();
        tabelData.setDefaultEditor(Object.class, null);
        tabelData2.setDefaultEditor(Object.class, null);
        tabelData3.setDefaultEditor(Object.class, null);
        FirstColumn();
        this.setLocationRelativeTo(null);
        
        ActionListener updateProgressBar = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowCount = tabelData.getRowCount();
                int completedCount = 0;

                for (int i = 0; i < rowCount; i++) {
                    String status = (String) tabelData.getValueAt(i, 4);
                    if (status.equals("Sudah Selesai")) {
                        completedCount++;
                    }
                }

                int progress = (int) (((double) completedCount / rowCount) * 100);
                progressBar.setValue(progress);
            }
        };
        Timer timer = new Timer(500, updateProgressBar);
        timer.start();
        UpdateTable();
        UpdateTable2();
        UpdateTable3();
        jLabel9.setText("");
        jLabel9.setIcon(logo);
        CustomTable();
        telegramBot = new TelegramBot(BOT_TOKEN, CHAT_ID);

        // Scheduler untuk memeriksa tenggat tugas dan mengirim notifikasi
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                checkAndSendTaskReminders();
            }
        };
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndSendTaskReminders, 0, 10, TimeUnit.MINUTES);
    }
    private void checkAndSendTaskReminders() {
        // Ambil data tenggat tugas dari tabel
        int rowCount = tabelData.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            Date deadline = (Date) tabelData.getValueAt(i, 1);
            String taskName = (String) tabelData.getValueAt(i, 2);

            // Jika tenggat tugas kurang dari 24 jam dari sekarang
            if (isDeadlineNear(deadline) && !isDeadlinePassedWithException(deadline)) {
                String reminderMessage = "⚠️ Reminder: Tugas '" + taskName + "' akan segera berakhir!";
                try {
                    // Kirim notifikasi ke bot Telegram
                    telegramBot.sendMessage(reminderMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private boolean isDeadlinePassedWithException(Date deadline) {
        long currentTime = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        long oneDayInMillis = 24 * 60 * 60 * 1000; // One day in milliseconds
        long deadlinePlusOneDay = deadlineTime + oneDayInMillis;
        return currentTime > deadlinePlusOneDay;
    }
    private boolean isDeadlineNear(Date deadline) {
        long currentTime = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000; // One week in milliseconds
        return (deadlineTime - currentTime) < oneWeekInMillis;
    }
    private class TelegramBot extends TelegramLongPollingBot {

        private final String botToken;
        private final String chatId;

        public TelegramBot(String botToken, String chatId) {
            this.botToken = botToken;
            this.chatId = chatId;
        }

        @Override
        public void onUpdateReceived(Update update) {
            
        }

        @Override
        public String getBotUsername() {
            return "ManajemenTugasBot";
        }

        @Override
        public String getBotToken() {
            return botToken;
        }

        public void sendMessage(String message) throws TelegramApiException {
            SendMessage sendMessage = new SendMessage(chatId, message);
            execute(sendMessage);
        }
        
    }
    public void KoneksiDatabase(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_tugas","root","12345");
            Pst = Con.prepareStatement("select * from tb_tugas order by Tenggat");
            Rs = Pst.executeQuery();
            ResultSetMetaData rsmd = Rs.getMetaData();
            int n = rsmd.getColumnCount();
            DefaultTableModel model = (DefaultTableModel)tabelData.getModel();
            DefaultTableModel model2 = (DefaultTableModel)tabelData2.getModel();
            DefaultTableModel model3 = (DefaultTableModel)tabelData3.getModel();
            model.setRowCount(0);
            
            while(Rs.next()){
                Vector v = new Vector();
                for(int i=1; i<=n; i++){
                    v.add(Rs.getString("No"));
                    v.add(Rs.getString("Tenggat"));
                    v.add(Rs.getString("Nama"));
                    v.add(Rs.getString("Kategori"));
                    v.add(Rs.getString("Status"));
                }
                model.addRow(v);
                model2.addRow(v);
                model3.addRow(v);
            }
//            JOptionPane.showMessageDialog(null, "Koneksi Berhasil");
        } catch (Exception e) {
            System.out.println("Koneksi Gagal" + e.getMessage());
        }
    }
    public void UpdateTable() {
        try{
            String sql = "select * from tb_tugas order by Tenggat";
            Pst = Con.prepareStatement(sql);
            Rs = Pst.executeQuery(sql);
            tabelData.setModel(DbUtils.resultSetToTableModel(Rs));
            tabelData.getColumnModel().getColumn(0).setMinWidth(0);
            tabelData.getColumnModel().getColumn(0).setMaxWidth(0);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    public void UpdateTable2() {
        try{
            String sql = "select * from tb_tugas order by Tenggat";
            Pst = Con.prepareStatement(sql);
            Rs = Pst.executeQuery(sql);
            tabelData2.setModel(DbUtils.resultSetToTableModel(Rs));
            tabelData2.getColumnModel().getColumn(0).setMinWidth(0);
            tabelData2.getColumnModel().getColumn(0).setMaxWidth(0);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    public void UpdateTable3() {
        try{
            String sql = "select * from tb_tugas order by Tenggat";
            Pst = Con.prepareStatement(sql);
            Rs = Pst.executeQuery(sql);
            tabelData3.setModel(DbUtils.resultSetToTableModel(Rs));
            tabelData3.getColumnModel().getColumn(0).setMinWidth(0);
            tabelData3.getColumnModel().getColumn(0).setMaxWidth(0);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    public void FirstColumn(){
        tabelData.getColumnModel().getColumn(0).setMinWidth(0);
        tabelData.getColumnModel().getColumn(0).setMaxWidth(0);
        tabelData2.getColumnModel().getColumn(0).setMinWidth(0);
        tabelData2.getColumnModel().getColumn(0).setMaxWidth(0);
        tabelData3.getColumnModel().getColumn(0).setMinWidth(0);
        tabelData3.getColumnModel().getColumn(0).setMaxWidth(0);
    }
    public void CustomJOption(){
        UIManager UI=new UIManager();
        UI.put("OptionPane.background", new Color(51, 51, 51));
        UI.put("Panel.background", new Color(51, 51, 51));
        UI.put("OptionPane.messageForeground", new Color(255, 204, 51));
        UI.put("Button.background", new Color(255, 204, 51));
    }
    public void CustomTable(){
        tabelData.getTableHeader().setFont(new Font("Poppins", Font.BOLD, 12));
        tabelData.getTableHeader().setOpaque(false);
        tabelData.getTableHeader().setBackground(new Color(255, 204, 51));
        tabelData.getTableHeader().setForeground(new Color(51, 51, 51));
        tabelData2.getTableHeader().setFont(new Font("Poppins", Font.BOLD, 12));
        tabelData2.getTableHeader().setOpaque(false);
        tabelData2.getTableHeader().setBackground(new Color(255, 204, 51));
        tabelData2.getTableHeader().setForeground(new Color(51, 51, 51));
        tabelData3.getTableHeader().setFont(new Font("Poppins", Font.BOLD, 12));
        tabelData3.getTableHeader().setOpaque(false);
        tabelData3.getTableHeader().setBackground(new Color(255, 204, 51));
        tabelData3.getTableHeader().setForeground(new Color(51, 51, 51));
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        homeButton = new javax.swing.JButton();
        tamditButton = new javax.swing.JButton();
        hapusButton = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabelData = new javax.swing.JTable();
        progressBar = new javax.swing.JProgressBar();
        progressLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        ingatButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabelData2 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        namaTugasField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        dateChooser = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        statusCheckBox1 = new javax.swing.JCheckBox();
        statusCheckBox2 = new javax.swing.JCheckBox();
        statusCheckBox3 = new javax.swing.JCheckBox();
        tambahButton = new javax.swing.JButton();
        pilihButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        editButton1 = new javax.swing.JButton();
        kategoriField = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabelData3 = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        hapusButtonYes = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 204, 51));

        homeButton.setBackground(new java.awt.Color(51, 51, 51));
        homeButton.setFont(new java.awt.Font("Poppins Medium", 1, 14)); // NOI18N
        homeButton.setForeground(new java.awt.Color(255, 204, 51));
        homeButton.setText("Home");
        homeButton.setBorder(null);
        homeButton.setMargin(new java.awt.Insets(20, 14, 20, 14));
        homeButton.setPreferredSize(new java.awt.Dimension(37, 50));
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeButtonActionPerformed(evt);
            }
        });

        tamditButton.setBackground(new java.awt.Color(51, 51, 51));
        tamditButton.setFont(new java.awt.Font("Poppins Medium", 1, 14)); // NOI18N
        tamditButton.setForeground(new java.awt.Color(255, 204, 51));
        tamditButton.setText("Tambah/Edit");
        tamditButton.setBorder(null);
        tamditButton.setMargin(new java.awt.Insets(20, 14, 20, 14));
        tamditButton.setPreferredSize(new java.awt.Dimension(37, 50));
        tamditButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tamditButtonActionPerformed(evt);
            }
        });

        hapusButton.setBackground(new java.awt.Color(51, 51, 51));
        hapusButton.setFont(new java.awt.Font("Poppins Medium", 1, 14)); // NOI18N
        hapusButton.setForeground(new java.awt.Color(255, 204, 51));
        hapusButton.setText("Hapus");
        hapusButton.setBorder(null);
        hapusButton.setMargin(new java.awt.Insets(20, 14, 20, 14));
        hapusButton.setPreferredSize(new java.awt.Dimension(37, 50));
        hapusButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hapusButtonActionPerformed(evt);
            }
        });

        jLabel9.setText("jLabel9");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(homeButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(tamditButton, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
            .addComponent(hapusButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(homeButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(tamditButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(hapusButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 240, Short.MAX_VALUE)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 130, 500));

        jPanel2.setBackground(new java.awt.Color(51, 51, 51));

        tabelData.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        tabelData.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No", "Tenggat", "Nama", "Kategori", "Status"
            }
        ));
        tabelData.setRowHeight(25);
        tabelData.setSelectionBackground(new java.awt.Color(255, 204, 102));
        jScrollPane1.setViewportView(tabelData);

        progressBar.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        progressBar.setForeground(new java.awt.Color(255, 204, 51));
        progressBar.setStringPainted(true);

        progressLabel.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        progressLabel.setForeground(new java.awt.Color(255, 204, 51));
        progressLabel.setText("Progress:");

        jLabel1.setFont(new java.awt.Font("Poppins Black", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 204, 51));
        jLabel1.setText("MANAJEMEN TUGAS");

        ingatButton.setBackground(new java.awt.Color(255, 204, 51));
        ingatButton.setFont(new java.awt.Font("Poppins SemiBold", 0, 12)); // NOI18N
        ingatButton.setForeground(new java.awt.Color(51, 51, 51));
        ingatButton.setText("Ingatkan");
        ingatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ingatButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(ingatButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(progressLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 530, Short.MAX_VALUE))
                .addGap(20, 20, 20))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(progressLabel)
                        .addComponent(ingatButton)))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Home", jPanel2);

        jPanel3.setBackground(new java.awt.Color(51, 51, 51));

        tabelData2.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        tabelData2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No", "Tenggat", "Nama", "Kategori", "Status"
            }
        ));
        tabelData2.setRowHeight(25);
        tabelData2.setSelectionBackground(new java.awt.Color(255, 204, 102));
        tabelData2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabelData2.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tabelData2);

        jLabel2.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Nama Tugas:");

        namaTugasField.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Kategori:");

        jLabel3.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Tenggat:");

        jLabel4.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Status:");

        statusCheckBox1.setBackground(new java.awt.Color(51, 51, 51));
        buttonGroup1.add(statusCheckBox1);
        statusCheckBox1.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        statusCheckBox1.setForeground(new java.awt.Color(255, 51, 51));
        statusCheckBox1.setText("Belum Selesai");
        statusCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusCheckBox1ActionPerformed(evt);
            }
        });

        statusCheckBox2.setBackground(new java.awt.Color(51, 51, 51));
        buttonGroup1.add(statusCheckBox2);
        statusCheckBox2.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        statusCheckBox2.setForeground(new java.awt.Color(255, 204, 51));
        statusCheckBox2.setText("Sedang Dikerjakan");
        statusCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusCheckBox2ActionPerformed(evt);
            }
        });

        statusCheckBox3.setBackground(new java.awt.Color(51, 51, 51));
        buttonGroup1.add(statusCheckBox3);
        statusCheckBox3.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        statusCheckBox3.setForeground(new java.awt.Color(102, 255, 0));
        statusCheckBox3.setText("Sudah Selesai");
        statusCheckBox3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusCheckBox3ActionPerformed(evt);
            }
        });

        tambahButton.setBackground(new java.awt.Color(255, 204, 51));
        tambahButton.setFont(new java.awt.Font("Poppins SemiBold", 0, 12)); // NOI18N
        tambahButton.setForeground(new java.awt.Color(51, 51, 51));
        tambahButton.setText("Tambah");
        tambahButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tambahButtonActionPerformed(evt);
            }
        });

        pilihButton.setBackground(new java.awt.Color(255, 204, 51));
        pilihButton.setFont(new java.awt.Font("Poppins SemiBold", 0, 12)); // NOI18N
        pilihButton.setForeground(new java.awt.Color(51, 51, 51));
        pilihButton.setText("Pilih");
        pilihButton.setPreferredSize(new java.awt.Dimension(82, 26));
        pilihButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pilihButtonActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Poppins Black", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 204, 51));
        jLabel6.setText("TAMBAH / EDIT  TUGAS");

        editButton1.setBackground(new java.awt.Color(255, 204, 51));
        editButton1.setFont(new java.awt.Font("Poppins SemiBold", 0, 12)); // NOI18N
        editButton1.setForeground(new java.awt.Color(51, 51, 51));
        editButton1.setText("Edit");
        editButton1.setPreferredSize(new java.awt.Dimension(82, 26));
        editButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButton1ActionPerformed(evt);
            }
        });

        kategoriField.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(dateChooser, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE))
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(kategoriField)))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(namaTugasField))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 530, Short.MAX_VALUE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(statusCheckBox1)
                                        .addGap(18, 18, 18)
                                        .addComponent(statusCheckBox2)
                                        .addGap(18, 18, 18)
                                        .addComponent(statusCheckBox3))
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(tambahButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(editButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(pilihButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(20, 20, 20))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel6)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(namaTugasField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(kategoriField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(dateChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusCheckBox1)
                    .addComponent(jLabel4)
                    .addComponent(statusCheckBox2)
                    .addComponent(statusCheckBox3))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tambahButton)
                    .addComponent(pilihButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );

        jTabbedPane1.addTab("Tambah/Edit", jPanel3);

        jPanel4.setBackground(new java.awt.Color(51, 51, 51));

        tabelData3.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        tabelData3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No", "Tenggat", "Nama", "Kategori", "Status"
            }
        ));
        tabelData3.setRowHeight(25);
        tabelData3.setSelectionBackground(new java.awt.Color(255, 204, 102));
        tabelData3.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabelData3.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabelData3.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tabelData3);

        jLabel7.setFont(new java.awt.Font("Poppins Medium", 0, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 204, 51));
        jLabel7.setText("(*Pilih baris data yang ingin dihapus)");

        jLabel8.setFont(new java.awt.Font("Poppins Black", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 204, 51));
        jLabel8.setText("HAPUS TUGAS");

        hapusButtonYes.setBackground(new java.awt.Color(255, 204, 51));
        hapusButtonYes.setFont(new java.awt.Font("Poppins SemiBold", 0, 12)); // NOI18N
        hapusButtonYes.setForeground(new java.awt.Color(51, 51, 51));
        hapusButtonYes.setText("Hapus");
        hapusButtonYes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hapusButtonYesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(hapusButtonYes)
                            .addComponent(jLabel7))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 530, Short.MAX_VALUE)
                        .addGap(20, 20, 20))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hapusButtonYes)
                .addGap(20, 20, 20))
        );

        jTabbedPane1.addTab("Hapus", jPanel4);

        getContentPane().add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, -30, 570, 530));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void statusCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusCheckBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_statusCheckBox1ActionPerformed

    private void statusCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusCheckBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_statusCheckBox2ActionPerformed

    private void statusCheckBox3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusCheckBox3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_statusCheckBox3ActionPerformed

    private void tambahButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambahButtonActionPerformed
        CustomJOption();
        try{
            String tenggat = new SimpleDateFormat("yyyy-MM-dd").format(dateChooser.getDate());
//            String status = "Belum Selesai";
            statusCheckBox1.setActionCommand("Belum Selesai");
            statusCheckBox2.setActionCommand("Sedang Dikerjakan");
            statusCheckBox3.setActionCommand("Sudah Selesai");
            String sql="INSERT INTO tb_tugas(Tenggat,Nama,Kategori,Status) values('"
                    +tenggat+"','"
                    +namaTugasField.getText()+"','"
                    +kategoriField.getText()+"','"
                    +buttonGroup1.getSelection().getActionCommand().toString()+"')";
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_tugas","root","12345");
            Pst = Con.prepareStatement(sql);
            dateChooser.setDate(null);
            namaTugasField.setText("");
            kategoriField.setText("");
            buttonGroup1.clearSelection();
            Pst.execute();
            
            JOptionPane.showMessageDialog(null, "Berhasil disimpan","Tambah Tugas",JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(null, "Gagal disimpan","Tambah Tugas",JOptionPane.INFORMATION_MESSAGE);
            System.out.println(e.getMessage());
        }
        UpdateTable();
        UpdateTable2();
        UpdateTable3();
    }//GEN-LAST:event_tambahButtonActionPerformed

    private void pilihButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pilihButtonActionPerformed
        try{
            int row = tabelData2.getSelectedRow();
            String eve = (tabelData2.getModel().getValueAt(row, 0).toString());
            String sql ="SELECT * FROM tb_tugas WHERE No='"+eve+"' ";
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_tugas","root","12345");
            Pst = Con.prepareStatement(sql);
            Rs = Pst.executeQuery(sql);
            if (Rs.next()){
                dateChooser.setDate(Rs.getDate("Tenggat"));
                namaTugasField.setText(Rs.getString("Nama"));
                kategoriField.setText(Rs.getString("Kategori"));
                String selectedOption = Rs.getString("Status");
                if(selectedOption.equals("Belum Selesai")){
                    statusCheckBox1.setSelected(true);
                } else if (selectedOption.equals("Sedang Dikerjakan")) {
                    statusCheckBox2.setSelected(true);
                } else if (selectedOption.equals("Sudah Selesai")){
                    statusCheckBox3.setSelected(true);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_pilihButtonActionPerformed

    private void hapusButtonYesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hapusButtonYesActionPerformed
        CustomJOption();
        DefaultTableModel model = (DefaultTableModel) tabelData3.getModel();
        int row = tabelData3.getSelectedRow();
        String eve = tabelData3.getModel().getValueAt(row, 0).toString();
        String delRow = "DELETE FROM tb_tugas WHERE No='"+eve+"' ";
        try {
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_tugas","root","12345");
            Pst = Con.prepareStatement(delRow);
//            Pst.executeUpdate();
            int confirm = JOptionPane.showConfirmDialog(this, "Apakah anda yakin ingin menghapus tugas ini?","Hapus Tugas",JOptionPane.INFORMATION_MESSAGE);
            if (confirm == 0) {
                Pst.executeUpdate();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,  ex.getMessage(),"Hapus Tugas",JOptionPane.INFORMATION_MESSAGE);
        }
        UpdateTable();
        UpdateTable2();
        UpdateTable3();
    }//GEN-LAST:event_hapusButtonYesActionPerformed

    private void homeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeButtonActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_homeButtonActionPerformed

    private void tamditButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tamditButtonActionPerformed
        jTabbedPane1.setSelectedIndex(1);
    }//GEN-LAST:event_tamditButtonActionPerformed

    private void hapusButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hapusButtonActionPerformed
        jTabbedPane1.setSelectedIndex(2);
    }//GEN-LAST:event_hapusButtonActionPerformed

    private void editButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButton1ActionPerformed
        CustomJOption();
        statusCheckBox1.setActionCommand("Belum Selesai");
        statusCheckBox2.setActionCommand("Sedang Dikerjakan");
        statusCheckBox3.setActionCommand("Sudah Selesai");
        int row = tabelData2.getSelectedRow();
        String eve = tabelData2.getModel().getValueAt(row, 0).toString();
        String tenggat = new SimpleDateFormat("yyyy-MM-dd").format(dateChooser.getDate());
        try{
            String sql = "UPDATE tb_tugas SET Tenggat='"+tenggat+
            "', Nama='"+namaTugasField.getText()+
            "', Kategori='"+kategoriField.getText()+
            "', Status='"+buttonGroup1.getSelection().getActionCommand().toString()+
            "' where No='"+eve+"'";
            Con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db_tugas","root","12345");
            Pst = Con.prepareStatement(sql);
            dateChooser.setDate(null);
            namaTugasField.setText("");
            kategoriField.setText("");
            buttonGroup1.clearSelection();
            Pst.executeUpdate();
            JOptionPane.showMessageDialog(null, "Berhasil diubah", "Edit Tugas",JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null,  e.getMessage(),"Edit Tugas",JOptionPane.INFORMATION_MESSAGE);
        }
        UpdateTable();
        UpdateTable2();
        UpdateTable3();
    }//GEN-LAST:event_editButton1ActionPerformed

    private void ingatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ingatButtonActionPerformed
        checkAndSendTaskReminders();
    }//GEN-LAST:event_ingatButtonActionPerformed
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    public com.toedter.calendar.JDateChooser dateChooser;
    private javax.swing.JButton editButton1;
    private javax.swing.JButton hapusButton;
    private javax.swing.JButton hapusButtonYes;
    private javax.swing.JButton homeButton;
    private javax.swing.JButton ingatButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    public javax.swing.JTextField kategoriField;
    public javax.swing.JTextField namaTugasField;
    private javax.swing.JButton pilihButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JCheckBox statusCheckBox1;
    private javax.swing.JCheckBox statusCheckBox2;
    private javax.swing.JCheckBox statusCheckBox3;
    private javax.swing.JTable tabelData;
    private javax.swing.JTable tabelData2;
    private javax.swing.JTable tabelData3;
    private javax.swing.JButton tambahButton;
    private javax.swing.JButton tamditButton;
    // End of variables declaration//GEN-END:variables
}
