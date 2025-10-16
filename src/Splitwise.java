import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.List;

public class Splitwise extends JFrame {

    // --- DB Configuration ---
    private static final String URL = "jdbc:mysql://localhost:3306/splitwise_db";
    private static final String USER = "root";
    private static final String PASS = "password";

    // --- GUI Models ---
    private DefaultListModel<String> memberListModel = new DefaultListModel<>();
    private JList<String> memberList = new JList<>(memberListModel);
    private DefaultTableModel expenseTableModel;
    private JTable expenseTable;
    private JTextArea balanceArea = new JTextArea();
    private JTextArea reportArea = new JTextArea();

    public Splitwise() {
        setTitle("Splitwise Desktop");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        UIManager.put("Panel.background", new Color(240, 247, 255));
        UIManager.put("Button.background", new Color(80, 130, 220));
        UIManager.put("Button.foreground", Color.white);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Members", membersPanel());
        tabs.addTab("Expenses", expensesPanel());
        tabs.addTab("Balances", balancesPanel());
        tabs.addTab("Reports", reportsPanel());
        add(tabs);

        refreshMembers();
        refreshExpenses();
        refreshBalances();
    }

    // ------------------ DATABASE ------------------
    public static Connection getConn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = getConn(); Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS members (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL)");
                stmt.execute("CREATE TABLE IF NOT EXISTS expenses (id INT AUTO_INCREMENT PRIMARY KEY, payer_id INT NOT NULL, amount DECIMAL(10,2), description VARCHAR(255), FOREIGN KEY (payer_id) REFERENCES members(id) ON DELETE CASCADE)");
                stmt.execute("CREATE TABLE IF NOT EXISTS expense_shares (id INT AUTO_INCREMENT PRIMARY KEY, expense_id INT NOT NULL, member_id INT NOT NULL, share DECIMAL(10,2), FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE, FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE)");
            }
            System.out.println("DB initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------ PANELS ------------------
    private JPanel membersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        memberList.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(new JScrollPane(memberList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Member");
        JButton delBtn = new JButton("Delete Member");

        addBtn.addActionListener(e -> addMember());
        delBtn.addActionListener(e -> deleteMember());

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel expensesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        String[] cols = {"ID", "Payer", "Amount", "Description", "Participants", "Share/Person"};
        expenseTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        expenseTable = new JTable(expenseTableModel);
        panel.add(new JScrollPane(expenseTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Expense");
        JButton delBtn = new JButton("Delete");
        JButton refBtn = new JButton("Refresh");

        addBtn.addActionListener(e -> addExpense());
        delBtn.addActionListener(e -> deleteExpense());
        refBtn.addActionListener(e -> { refreshExpenses(); refreshBalances(); });

        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        btnPanel.add(refBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel balancesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        balanceArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        balanceArea.setEditable(false);
        panel.add(new JScrollPane(balanceArea), BorderLayout.CENTER);
        JButton refBtn = new JButton("Recalculate");
        refBtn.addActionListener(e -> refreshBalances());
        panel.add(refBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel reportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        reportArea.setEditable(false);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        JButton gen = new JButton("Generate Report");
        gen.addActionListener(e -> generateReport());
        panel.add(gen, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------ MEMBER FUNCTIONS ------------------
    private void addMember() {
        String name = JOptionPane.showInputDialog(this, "Enter name:");
        if (name == null || name.trim().isEmpty()) return;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement("INSERT INTO members(name) VALUES(?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            refreshMembers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void deleteMember() {
        String selected = memberList.getSelectedValue();
        if (selected == null) return;
        int id = Integer.parseInt(selected.split("\\.")[0]);
        int c = JOptionPane.showConfirmDialog(this, "Delete " + selected + " ?");
        if (c != JOptionPane.YES_OPTION) return;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refreshMembers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void refreshMembers() {
        memberListModel.clear();
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, name FROM members ORDER BY id")) {
            while (rs.next()) {
                memberListModel.addElement(rs.getInt("id") + ". " + rs.getString("name"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ------------------ EXPENSE FUNCTIONS ------------------
    private void addExpense() {
        try {
            List<Integer> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id,name FROM members")) {
                while (rs.next()) { ids.add(rs.getInt("id")); names.add(rs.getString("name")); }
            }
            if (ids.isEmpty()) { JOptionPane.showMessageDialog(this, "Add members first."); return; }

            String amtStr = JOptionPane.showInputDialog(this, "Enter amount:");
            if (amtStr == null) return;
            BigDecimal amount = new BigDecimal(amtStr);
            String desc = JOptionPane.showInputDialog(this, "Enter description:");
            if (desc == null) desc = "";

            String payer = (String) JOptionPane.showInputDialog(this, "Select payer:", "Payer", JOptionPane.QUESTION_MESSAGE, null, names.toArray(), names.get(0));
            if (payer == null) return;
            int payerId = ids.get(names.indexOf(payer));

            // select participants
            JPanel panel = new JPanel(new GridLayout(0, 1));
            List<JCheckBox> cbs = new ArrayList<>();
            for (String n : names) {
                JCheckBox cb = new JCheckBox(n);
                cb.setSelected(true);
                cbs.add(cb);
                panel.add(cb);
            }
            int r = JOptionPane.showConfirmDialog(this, panel, "Select participants", JOptionPane.OK_CANCEL_OPTION);
            if (r != JOptionPane.OK_OPTION) return;
            List<Integer> partIds = new ArrayList<>();
            for (int i = 0; i < cbs.size(); i++) if (cbs.get(i).isSelected()) partIds.add(ids.get(i));

            BigDecimal share = amount.divide(BigDecimal.valueOf(partIds.size()), 2, BigDecimal.ROUND_HALF_UP);

            try (Connection conn = getConn()) {
                conn.setAutoCommit(false);
                int expId;
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO expenses(payer_id,amount,description) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, payerId);
                    ps.setBigDecimal(2, amount);
                    ps.setString(3, desc);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    rs.next();
                    expId = rs.getInt(1);
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO expense_shares(expense_id,member_id,share) VALUES(?,?,?)")) {
                    for (int pid : partIds) {
                        ps.setInt(1, expId);
                        ps.setInt(2, pid);
                        ps.setBigDecimal(3, share);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            }
            refreshExpenses();
            refreshBalances();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void deleteExpense() {
        int row = expenseTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an expense."); return; }
        int id = (int) expenseTableModel.getValueAt(row, 0);
        int c = JOptionPane.showConfirmDialog(this, "Delete expense ID " + id + "?");
        if (c != JOptionPane.YES_OPTION) return;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement("DELETE FROM expenses WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            refreshExpenses();
            refreshBalances();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }

    private void refreshExpenses() {
        expenseTableModel.setRowCount(0);
        String sql = "SELECT e.id, m.name AS payer, e.amount, e.description, GROUP_CONCAT(m2.name) AS participants, AVG(es.share) AS share_per_person " +
                "FROM expenses e JOIN members m ON e.payer_id = m.id JOIN expense_shares es ON e.id = es.expense_id JOIN members m2 ON es.member_id = m2.id " +
                "GROUP BY e.id ORDER BY e.id DESC";
        try (Connection conn = getConn(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                expenseTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("payer"),
                        rs.getBigDecimal("amount"),
                        rs.getString("description"),
                        rs.getString("participants"),
                        rs.getBigDecimal("share_per_person")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ------------------ BALANCES ------------------
    private void refreshBalances() {
        balanceArea.setText("");
        try (Connection conn = getConn()) {
            Map<Integer, String> members = new HashMap<>();
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id,name FROM members")) {
                while (rs.next()) members.put(rs.getInt("id"), rs.getString("name"));
            }
            Map<Integer, Map<Integer, BigDecimal>> owes = new HashMap<>();
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT e.payer_id, es.member_id, es.share FROM expense_shares es JOIN expenses e ON e.id = es.expense_id")) {
                while (rs.next()) {
                    int payer = rs.getInt("payer_id");
                    int member = rs.getInt("member_id");
                    BigDecimal share = rs.getBigDecimal("share");
                    if (payer == member) continue;
                    owes.computeIfAbsent(member, k -> new HashMap<>());
                    owes.get(member).merge(payer, share, BigDecimal::add);
                }
            }
            for (Map.Entry<Integer, Map<Integer, BigDecimal>> entry : owes.entrySet()) {
                String member = members.get(entry.getKey());
                for (Map.Entry<Integer, BigDecimal> o : entry.getValue().entrySet()) {
                    balanceArea.append(member + " owes " + members.get(o.getKey()) + " ₹" + o.getValue() + "\n");
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ------------------ REPORTS ------------------
    private void generateReport() {
        reportArea.setText("");
        try (Connection conn = getConn(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT SUM(amount) AS total, COUNT(*) AS count FROM expenses");
            if (rs.next()) {
                reportArea.append("Total Expenses: ₹" + rs.getBigDecimal("total") + "\nTotal Entries: " + rs.getInt("count") + "\n\n");
            }
            rs = stmt.executeQuery("SELECT m.name, SUM(e.amount) AS spent FROM expenses e JOIN members m ON e.payer_id=m.id GROUP BY m.id ORDER BY spent DESC");
            reportArea.append("Top Spenders:\n");
            while (rs.next()) {
                reportArea.append(rs.getString("name") + " → ₹" + rs.getBigDecimal("spent") + "\n");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    // ------------------ MAIN ------------------
    public static void main(String[] args) {
        initDB();
        SwingUtilities.invokeLater(() -> new Splitwise().setVisible(true));
    }
}
