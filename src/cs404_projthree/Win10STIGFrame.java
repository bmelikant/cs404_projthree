package cs404_projthree;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Date;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/*
 * @author Ben Melikant, Zainab Alalshaikh, Alex Way, Gavin Spellmeyer
 */
public class Win10STIGFrame extends javax.swing.JFrame implements Runnable {

    // 
    public static final int PASS = 0;
    public static final int FAIL = 1;
    public static final int INVALID = 2;
    
    // thread item (destroy on close!)
    Thread t;
    
    /*
     * Creates new form Win10STIGFrame
     */
    public Win10STIGFrame() {
        initComponents();
    }
    
    // void checkStigRequirements (): Check to see whether the system meets all STIG guidelines
    // inputs: None
    // returns: none
    private void checkStigRequirements () {

        // set the text for the status bar
        this.jmi_RunReportItem.setEnabled(false);
        jl_StatusBarLabel.setText ("Running STIG checks");
        
        // build the table data model
        DefaultTableModel tm = new DefaultTableModel (null, new Object [] { "Configuration", "Result" });
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking filesystem type)");
        // check for NTFS on C:
        int result = checkForNTFS ();
        if (result == PASS)
            tm.addRow (new Object [] { "C:\\ formatted as NTFS", "SUCCESS" });
        else if (result == FAIL)
            tm.addRow (new Object [] { "C:\\ formatted as NTFS", "FAILURE" });
        else if (result == INVALID)
            tm.addRow (new Object [] { "C:\\ formatted as NTFS", "INVALID COMMAND" });
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking SSDP service)");
        int result_stopped = checkServiceState ("SSDPSRV", "stopped");
        int result_disabled = checkServiceState ("SSDPSRV", "disabled");
        
        if (result_stopped == PASS || result_disabled == PASS)
            tm.addRow (new Object [] { "SSDP service is stopped", "SUCCESS" });
        else if (result_stopped == FAIL)
            tm.addRow (new Object [] { "SSDP service is stopped", "FAILURE" });
        else if (result_stopped == INVALID)
            tm.addRow (new Object [] { "SSDP service is stopped", "INVALID COMMAND" });
        
        if (result_disabled == PASS)
            tm.addRow (new Object [] { "SSDP service is disabled", "SUCCESS" });
        else if (result_disabled == FAIL)
             tm.addRow (new Object [] { "SSDP service is disabled", "FAILURE" });
        else if (result_disabled == INVALID)
             tm.addRow (new Object [] { "SSDP service is disabled", "INVALID COMMAND" });
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking uPnP host service)");
        result_stopped = checkServiceState ("UPNPHOST", "stopped");
        result_disabled = checkServiceState ("UPNPHOST", "disabled");
        
        if (result_stopped == PASS || result_disabled == PASS)
            tm.addRow (new Object [] { "uPnP service is stopped", "SUCCESS" });
        else if (result_stopped == FAIL)
            tm.addRow (new Object [] { "uPnP service is stopped", "FAILURE" });
        else if (result_stopped == INVALID)
            tm.addRow (new Object [] { "uPnP service is stopped", "INVALID COMMAND" });
        
        if (result_disabled == PASS)
            tm.addRow (new Object [] { "uPnP service is disabled", "SUCCESS" });
        else if (result_disabled == FAIL)
             tm.addRow (new Object [] { "uPnP service is disabled", "FAILURE" });
        else if (result_disabled == INVALID)
             tm.addRow (new Object [] { "uPnP service is disabled", "INVALID COMMAND" });
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking Windows 10 version)");
        result = checkForWindowsVer ();
        tm.addRow (new Object [] { "Windows Version is Win10 Enterprise", ((result == PASS) ? "SUCCESS" : ((result == FAIL) ? "FAILURE" : "INVALID COMMAND"))});
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking if IIS is installed)");
        result = checkForIISInstall ();
        tm.addRow (new Object [] { "IIS is NOT installed", ((result == PASS) ? "SUCCESS" : ((result == FAIL) ? "FAILURE" : "INVALID COMMAND"))});
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking for expired / inactive user accounts)");
        result = checkForInactive ();
        tm.addRow (new Object [] { "No inactive user accounts", ((result == PASS) ? "SUCCESS" : ((result == FAIL) ? "FAILURE" : "INVALID COMMAND"))});
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking for password expiry parameter)");
        result = checkPasswordExpiry ();
        tm.addRow (new Object [] { "Passwords must expire", ((result == PASS) ? "SUCCESS" : ((result == FAIL) ? "FAILURE" : "INVALID COMMAND"))});
        
        jl_StatusBarLabel.setText ("Running STIG checks (checking for password history length)");
        result = checkPasswordHistory();
        tm.addRow (new Object [] { "Password history is 24", ((result == PASS) ? "SUCCESS" : ((result == FAIL) ? "FAILURE" : "INVALID COMMAND"))});
        
        this.jt_ReportOutTable.setModel (tm);
        this.jmi_RunReportItem.setEnabled(true);
        jl_StatusBarLabel.setText ("Done running STIG checks!");
    }
    
    //Checks for the windows OS name
    //Must run Windows Enterprise
    private int checkForWindowsVer () {
        try {
            
            byte [] result = executeCommand ("systeminfo");
            
            if (result == null)
                return INVALID;
           
            String cmdOutput = new String (result);
            
            if (cmdOutput.contains ("Windows 10 Enterprise"))
                return PASS;
            else
                return FAIL;
            
        } catch (IOException w) {
            
            return INVALID;
        }
    }

       //Not completed, must find working information on working execution
    private int checkForIISInstall(){
        
        try {
            
            byte [] result = executeCommand ("net start w3svc");
            
            String cmdOutput = new String (result);
            int idx = cmdOutput.indexOf("");
            
            if (cmdOutput.contains ("service name is invalid"))
                return PASS;
            else
                return FAIL;
            
        } catch (IOException e) {
            
            System.err.println ("Service check error: " + e.toString ());
            return INVALID;
        }
    }
    
    //use powershell
    //WN10-00-000065 on the STIG Document
    private int checkForInactive(){
        
        try {
        
            int status = PASS;
            
            Process p = Runtime.getRuntime().exec ("powershell -command \"([ADSI]('WinNT://{0}' -f $env:COMPUTERNAME)).Children | Where { $_.SchemaClassName -eq 'user' } | ForEach {"
                    + "$user = ([ADSI]$_.Path);"
                    + "$lastLogin = $user.Properties.LastLogin.Value;"
                    + "$enabled = ($user.Properties.UserFlags.Value -band 0x2) -ne 0x2;"
                    + "if ($lastLogin -eq $null) {"
                    + "$lastLogin = 'Never';"
                    + "}"
                    + "Write-Host $user.Name $lastLogin $enabled;"
                    + "}\"");
            
            BufferedReader rdr = new BufferedReader (new InputStreamReader (p.getInputStream()));
            
            String line;
            while ((line = rdr.readLine ()) != null) {
            
                // if the line says "True" the account is active
                if (line.contains ("True")) {
                    
                    if (line.contains ("Never"))
                        return FAIL;
                    
                    String [] fields = line.split (" ");
                    String [] datevals = fields[1].split ("/");
                    
                    int month = Integer.parseInt(datevals[0]);
                    int day = Integer.parseInt (datevals[1]);
                    int year = Integer.parseInt (datevals[2]);
                    
                    LocalDate lastLogin = LocalDate.of (month, day, year);
                    LocalDate now = LocalDate.now ();
                    
                    System.out.println (now.compareTo (lastLogin));
                    
                    if (now.compareTo (lastLogin) > 35)
                        return FAIL;
                }
            }
            
            rdr.close ();
            p.getOutputStream().close ();
            
            return status;
            
        } catch (IOException e) {
            
            System.out.println ("Error executing command: " + e.toString ());
            return INVALID;
        }
    }
    
    // boolean checkForNTFS ()
    // inputs: none, returns: PASS OR FAIL
    private int checkForNTFS () {
        
        try {
            
            byte [] result = executeCommand ("fsutil fsinfo volumeInfo C:");
            
            if (result == null)                
                return INVALID;
            
            String cmdOutput = new String (result);
            int idx = cmdOutput.indexOf("File System Name : ");
            
            if (idx >= 0) {
                
                String dataLine = cmdOutput.substring(idx+"File System Name : ".length(),
                        cmdOutput.indexOf("\n", idx+"File System Name : ".length())).trim();
                
                if (dataLine.equals ("NTFS"))
                    return PASS;
                else
                    return FAIL;
            }
            
            else
                return INVALID;
            
        } catch (IOException e) {
            
            return INVALID;
        }
    }
    
    // int checkServiceState (): See if the service state matches the requested state
    // inputs: svc - name of service, state - state required
    // returns: PASS, FAIL, INVALID
    private int checkServiceState (String svc, String state) {
        
        try {
            
            byte [] cmdResult = executeCommand ("sc query \"" + svc + "\"");
            
            if (cmdResult == null)
                return INVALID;
            
            String cmdData = new String (cmdResult);
            String [] cmdLines = cmdData.split("\r\n");
            
            // print each line of the service data (testing purposes)
            for (String s : cmdLines) {
                
                if (s.contains ("STATE") && s.contains (state.toUpperCase()))
                    return PASS;   
            }
            
            return FAIL;
            
        } catch (IOException e) {
            
            return INVALID;
        }
    }
       // int checkPasswordExpiry (): See if any of the user accounts have passwords that do not expire
    // inputs: user - name of the user you wish to examine
    // returns: PASS, FAIL, INVALID
    // NEEDS TESTING
    private int checkPasswordExpiry ()
    {
        try
        {
            byte [] result = executeCommand ("net accounts");
            
            if (result == null)
                return INVALID;
            
            String cmdData = new String (result);
            String [] cmdLines = cmdData.split("\r\n");
            
            // Prints each line of command output into Java output
            for (String s : cmdLines)
            {
                System.out.println (s);
                
                if(s.contains("Maximum password age") && s.contains ("Unlimited"))
                    return FAIL;
            }
            
            return PASS;
        }
        
        catch (IOException e)
        {
            return INVALID;
        }
    }
    
    // int checkPasswordHistory (): See how many previously-entered passwords the machine saves at one time
    // inputs: None
    // returns: PASS, FAIL, INVALID
    // NEEDS TESTING
    private int checkPasswordHistory ()
    {
        try
        {
            byte [] result = executeCommand ("net accounts");
            
            if(result == null)
            {
                return INVALID;
            }
            
            // Prints each line of command output into Java output
            String cmdData = new String (result);
            String [] cmdLines = cmdData.split("\r\n");
            
            for(String s : cmdLines)
            {
                if(s.contains("Length of password history maintained") && s.contains("24"))
                    return PASS;
            }
            
            return FAIL;
        }
        
        catch (IOException e)
        {
            return INVALID;
        }
    }
    
    // byte [] executeCommand (): Get the stdout data from a command
    // inputs: cmd - command to run
    // returns: stdout from the program, null if no output
    private byte [] executeCommand (String cmd) throws IOException {
        
        try {
        
            // fsutil fsinfo volumeInfo C:
            Process p = Runtime.getRuntime().exec (cmd);
            p.waitFor ();
            
            byte [] pstdin = new byte [p.getInputStream().available()];
            byte [] pstderr = new byte [p.getErrorStream().available()];
            
            p.getInputStream().read(pstdin);
            p.getErrorStream().read(pstderr);
            
            if (p.exitValue () > 0)
                return pstderr;
            else
                return pstdin;
        
        } catch (InterruptedException e) {
         
            JOptionPane.showMessageDialog (null, "Interrupted while waiting for process: " + e.toString(),
                    "Error:", JOptionPane.ERROR_MESSAGE);
        }
        
        return null;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jt_ReportOutTable = new javax.swing.JTable();
        jp_StatusBarPanel = new javax.swing.JPanel();
        jl_StatusBarLabel = new javax.swing.JLabel();
        jmb_MainMenuBar = new javax.swing.JMenuBar();
        jm_FileMenu = new javax.swing.JMenu();
        jmi_ExitItem = new javax.swing.JMenuItem();
        jm_ReportMenu = new javax.swing.JMenu();
        jmi_RunReportItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jt_ReportOutTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Configuration", "Result"
            }
        ));
        jt_ReportOutTable.setRequestFocusEnabled(false);
        jScrollPane2.setViewportView(jt_ReportOutTable);

        javax.swing.GroupLayout jp_StatusBarPanelLayout = new javax.swing.GroupLayout(jp_StatusBarPanel);
        jp_StatusBarPanel.setLayout(jp_StatusBarPanelLayout);
        jp_StatusBarPanelLayout.setHorizontalGroup(
            jp_StatusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jp_StatusBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jl_StatusBarLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_StatusBarPanelLayout.setVerticalGroup(
            jp_StatusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jl_StatusBarLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE)
        );

        jm_FileMenu.setText("File");

        jmi_ExitItem.setLabel("Exit");
        jmi_ExitItem.setName(""); // NOI18N
        jmi_ExitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmi_ExitItemActionPerformed(evt);
            }
        });
        jm_FileMenu.add(jmi_ExitItem);

        jmb_MainMenuBar.add(jm_FileMenu);

        jm_ReportMenu.setText("Report");

        jmi_RunReportItem.setText("Run Report");
        jmi_RunReportItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmi_RunReportItemActionPerformed(evt);
            }
        });
        jm_ReportMenu.add(jmi_RunReportItem);

        jmb_MainMenuBar.add(jm_ReportMenu);

        setJMenuBar(jmb_MainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                    .addComponent(jp_StatusBarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jp_StatusBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jmi_ExitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmi_ExitItemActionPerformed
        
        // check if the thread is running
        if (t != null && t.isAlive()) {
            
            try {
            
                t.join();
            
            } catch (InterruptedException e) {
            
                System.err.println ("Thread interrupted: " + e.toString());
            }
        }
        System.exit (0);
    }//GEN-LAST:event_jmi_ExitItemActionPerformed

    private void jmi_RunReportItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmi_RunReportItemActionPerformed
        
        // run the checks in a thread!
        t = new Thread(this);
        t.start();
    }//GEN-LAST:event_jmi_RunReportItemActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        if (t != null && t.isAlive()) {
            
            try {
            
                t.join();
            
            } catch (InterruptedException e) {
            
                System.err.println ("Thread interrupted: " + e.toString());
            }
        }
    }//GEN-LAST:event_formWindowClosing

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
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Win10STIGFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Win10STIGFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jl_StatusBarLabel;
    private javax.swing.JMenu jm_FileMenu;
    private javax.swing.JMenu jm_ReportMenu;
    private javax.swing.JMenuBar jmb_MainMenuBar;
    private javax.swing.JMenuItem jmi_ExitItem;
    private javax.swing.JMenuItem jmi_RunReportItem;
    private javax.swing.JPanel jp_StatusBarPanel;
    private javax.swing.JTable jt_ReportOutTable;
    // End of variables declaration//GEN-END:variables

    @Override
    public void run() { checkStigRequirements (); }
}