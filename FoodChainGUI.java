package foodchain;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import utils.Utils;

/**
 * GUI para Food Chain - Cadeia Alimentar
 * Suporta criação de produtos (produtor) e transferências entre estações
 */
public class FoodChainGUI extends JFrame {

    private FoodUser user;
    private RemoteNodeInterface remoteObject;
    
    // Cores do design
    private static final Color HEADER_COLOR = new Color(26, 118, 188);      // Azul escuro
    private static final Color BUTTON_COLOR = new Color(41, 182, 246);      // Azul claro
    private static final Color PANEL_BG = new Color(245, 245, 245);         // Cinza muito claro
    private static final Color TEXT_COLOR = new Color(33, 33, 33);          // Cinza escuro
    private static final Color ACCENT_COLOR = new Color(76, 175, 80);       // Verde
    
    // Componentes UI
    private JTabbedPane tabbedPane;
    private JTextArea txtProductList;
    private JButton btnRefresh;
    
    // CREATE Product Tab
    private JTextField txtProducerCompany;
    private JTextField txtProductName, txtBatchId, txtCategory;
    private JTextField txtQuantity, txtUnit;
    private JPasswordField txtProducerPassword;
    private JButton btnCreateProduct;
    
    // WAREHOUSE Tab
    private JTextField txtWarehouseProductId, txtWarehouseReceiver;
    private JTextField txtWarehouseZone, txtWarehouseTemp;
    private JPasswordField txtWarehousePassword;
    private JButton btnWarehouseTransfer;
    
    // TRANSPORT Tab
    private JTextField txtTransportProductId, txtTransportReceiver;
    private JTextField txtTransportOrigin, txtTransportDestination, txtTransportVehicle, txtTransportTemp;
    private JPasswordField txtTransportPassword;
    private JButton btnTransportTransfer;
    
    // STORE Tab
    private JTextField txtStoreProductId, txtStoreReceiver;
    private JTextField txtStorePrice, txtStoreTemp;
    private JPasswordField txtStorePassword;
    private JButton btnStoreTransfer;
    
    public FoodChainGUI(FoodUser user, RemoteNodeInterface remoteObject) {
        this.user = user;
        this.remoteObject = remoteObject;
        
        setTitle("🌾 Food Chain - " + user.getUserName() + " [" + user.getUserTypeName() + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(PANEL_BG);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = HEADER_COLOR;
                lightHighlight = BUTTON_COLOR;
                shadow = new Color(100, 100, 100);
                darkShadow = new Color(50, 50, 50);
                focus = HEADER_COLOR;
            }
        });
        
        // Tab 0: Criar Produto (Produtor)
        JPanel tabIcon0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon0.setBackground(PANEL_BG);
        tabIcon0.add(new JLabel("📦"));
        tabIcon0.add(new JLabel("Produtor"));
        tabbedPane.addTab(null, createProductPanel());
        tabbedPane.setTabComponentAt(0, tabIcon0);
        
        // Tab 1: Armazém
        JPanel tabIcon1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon1.setBackground(PANEL_BG);
        tabIcon1.add(new JLabel("🏭"));
        tabIcon1.add(new JLabel("Armazém"));
        tabbedPane.addTab(null, createWarehousePanel());
        tabbedPane.setTabComponentAt(1, tabIcon1);
        
        // Tab 2: Transporte
        JPanel tabIcon2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon2.setBackground(PANEL_BG);
        tabIcon2.add(new JLabel("🚚"));
        tabIcon2.add(new JLabel("Transporte"));
        tabbedPane.addTab(null, createTransportPanel());
        tabbedPane.setTabComponentAt(2, tabIcon2);
        
        // Tab 3: Loja/Retalhista
        JPanel tabIcon3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon3.setBackground(PANEL_BG);
        tabIcon3.add(new JLabel("🏪"));
        tabIcon3.add(new JLabel("Loja"));
        tabbedPane.addTab(null, createStorePanel());
        tabbedPane.setTabComponentAt(3, tabIcon3);
        
        // Tab 4: Lista de Produtos
        JPanel tabIcon4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon4.setBackground(PANEL_BG);
        tabIcon4.add(new JLabel("📋"));
        tabIcon4.add(new JLabel("Blockchain"));
        tabbedPane.addTab(null, createProductListPanel());
        tabbedPane.setTabComponentAt(4, tabIcon4);
        
        // Tab 5: Cliente (Rastreamento de Produtos à Venda)
        JPanel tabIcon5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        tabIcon5.setBackground(PANEL_BG);
        tabIcon5.add(new JLabel("👤"));
        tabIcon5.add(new JLabel("Cliente"));
        tabbedPane.addTab(null, createClientPanel());
        tabbedPane.setTabComponentAt(5, tabIcon5);
        
        // Controlar acesso às abas conforme o tipo de utilizador
        controlAbasByUserType();
        
        add(tabbedPane);
    }
    
    private void controlAbasByUserType() {
        // Desabilitar abas conforme o tipo de utilizador
        switch(user.getUserType()) {
            case FoodUser.TYPE_PRODUCER:    // Produtor (1)
                // Produtor só pode usar a aba de Produtor e Blockchain
                disableTab(1);  // Armazém
                disableTab(2);  // Transporte
                disableTab(3);  // Loja
                break;
                
            case FoodUser.TYPE_WAREHOUSE:   // Armém (2)
                // Armazém só pode usar a aba de Armazém e Blockchain
                disableTab(0);  // Produtor
                disableTab(2);  // Transporte
                disableTab(3);  // Loja
                tabbedPane.setSelectedIndex(1);     // Selecionar aba Armazém
                break;
                
            case FoodUser.TYPE_DISTRIBUTOR: // Distribuidor (3)
                // Distribuidor só pode usar a aba de Transporte e Blockchain
                disableTab(0);  // Produtor
                disableTab(1);  // Armazém
                disableTab(3);  // Loja
                tabbedPane.setSelectedIndex(2);     // Selecionar aba Transporte
                break;
                
            case FoodUser.TYPE_STORE:       // Loja (4)
                // Loja só pode usar a aba de Loja e Blockchain
                disableTab(0);  // Produtor
                disableTab(1);  // Armazém
                disableTab(2);  // Transporte
                tabbedPane.setSelectedIndex(3);     // Selecionar aba Loja
                break;
        }
    }
    
    private void disableTab(int index) {
        tabbedPane.setEnabledAt(index, false);
        // Mudar cor do componente da aba para indicar que está desabilitada
        JPanel tabComponent = (JPanel) tabbedPane.getTabComponentAt(index);
        if (tabComponent != null) {
            tabComponent.setBackground(new Color(220, 220, 220));
            for (Component comp : tabComponent.getComponents()) {
                comp.setForeground(new Color(128, 128, 128));
            }
        }
    }
    
    // Método helper para criar labels com emojis visíveis
    private JLabel createEmojiLabel(String emoji, String text) {
        // Criar um JPanel para melhor controle do layout
        JLabel label = new JLabel() {
            @Override
            public void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
            }
        };
        
        // HTML para renderizar emoji grande + texto
        String html = "<html><font size='5'>" + emoji + "</font>&nbsp;&nbsp;" + text + "</html>";
        label.setText(html);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }
    
    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Painel de Título
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("<html><font size='6'>🌱</font> CRIAR NOVO PRODUTO</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(headerPanel, gbc);
        
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridwidth = 1;
        int row = 1;

        // Produtor / Empresa
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🏢", "Produtor/Empresa:"), gbc);
        txtProducerCompany = new JTextField(user.getUserName(), 25);
        txtProducerCompany.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtProducerCompany.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtProducerCompany, gbc);
        row++;

        // Nome do Produto
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("📝", "Produto:"), gbc);
        txtProductName = new JTextField("Maçãs", 25);
        txtProductName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtProductName.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtProductName, gbc);
        row++;
        
        // Lote
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🔢", "Lote:"), gbc);
        txtBatchId = new JTextField("LOTE-" + System.currentTimeMillis(), 25);
        txtBatchId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBatchId.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtBatchId, gbc);
        row++;
        
        // Categoria
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🏷️", "Categoria:"), gbc);
        txtCategory = new JTextField("Fruta", 25);
        txtCategory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtCategory.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtCategory, gbc);
        row++;
        
        // Quantidade
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("�", "Qtd:"), gbc);
        txtQuantity = new JTextField("100", 25);
        txtQuantity.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtQuantity.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtQuantity, gbc);
        row++;
        
        // Unidade
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("⚖️", "Un:"), gbc);
        txtUnit = new JTextField("kg", 25);
        txtUnit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtUnit.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtUnit, gbc);
        row++;
        
        // Password
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🔒", "Pass:"), gbc);
        txtProducerPassword = new JPasswordField("123qwe", 25);
        txtProducerPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtProducerPassword.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtProducerPassword, gbc);
        row++;
        
        // Botão Criar
        btnCreateProduct = new JButton("✅ CRIAR PRODUTO");
        btnCreateProduct.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnCreateProduct.setBackground(ACCENT_COLOR);
        btnCreateProduct.setForeground(Color.WHITE);
        btnCreateProduct.setFocusPainted(false);
        btnCreateProduct.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnCreateProduct.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateProduct.addActionListener(e -> createProduct());
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(btnCreateProduct, gbc);
        
        // Adicionar espaço em branco no final
        JPanel spacer = new JPanel();
        spacer.setBackground(PANEL_BG);
        gbc.gridx = 0; gbc.gridy = row + 1; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(spacer, gbc);
        
        return panel;
    }
    
    private JPanel createWarehousePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Painel de Título
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("<html><font size='6'>📦</font> RECEBER DO PRODUTOR</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(headerPanel, gbc);
        
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridwidth = 1;
        int row = 1;
        
        // Product ID
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🆔", "Product ID:"), gbc);
        JPanel idPanel = new JPanel(new BorderLayout(5, 0));
        idPanel.setBackground(PANEL_BG);
        txtWarehouseProductId = new JTextField(25);
        txtWarehouseProductId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtWarehouseProductId.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JButton btnSearch = new JButton("🔍");
        btnSearch.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnSearch.setBackground(BUTTON_COLOR);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> showProductSelectorForWarehouse());
        idPanel.add(txtWarehouseProductId, BorderLayout.CENTER);
        idPanel.add(btnSearch, BorderLayout.EAST);
        gbc.gridx = 1;
        panel.add(idPanel, gbc);
        row++;
        
        // Receptor
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("👤", "Para:"), gbc);
        txtWarehouseReceiver = new JTextField(25);
        txtWarehouseReceiver.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtWarehouseReceiver.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtWarehouseReceiver, gbc);
        row++;
        
        // Zona de Armazenamento
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🗂️", "Zona:"), gbc);
        txtWarehouseZone = new JTextField("Zona Refrigerada A1", 25);
        txtWarehouseZone.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtWarehouseZone.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtWarehouseZone, gbc);
        row++;
        
        // Temperatura
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🌡️", "Temp(°C):"), gbc);
        txtWarehouseTemp = new JTextField("4.0", 25);
        txtWarehouseTemp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtWarehouseTemp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtWarehouseTemp, gbc);
        row++;
        
        // Password
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🔒", "Pass:"), gbc);
        txtWarehousePassword = new JPasswordField("123qwe", 25);
        txtWarehousePassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtWarehousePassword.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtWarehousePassword, gbc);
        row++;
        
        // Botão
        btnWarehouseTransfer = new JButton("✅ RECEBER PRODUTO");
        btnWarehouseTransfer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnWarehouseTransfer.setBackground(ACCENT_COLOR);
        btnWarehouseTransfer.setForeground(Color.WHITE);
        btnWarehouseTransfer.setFocusPainted(false);
        btnWarehouseTransfer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnWarehouseTransfer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnWarehouseTransfer.addActionListener(e -> warehouseTransfer());
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(btnWarehouseTransfer, gbc);
        
        // Espaço em branco
        JPanel spacer = new JPanel();
        spacer.setBackground(PANEL_BG);
        gbc.gridx = 0; gbc.gridy = row + 1; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(spacer, gbc);
        
        return panel;
    }
    
    private JPanel createTransportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Painel de Título
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("<html><font size='6'>🚚</font> MOVIMENTAR PRODUTOS</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(headerPanel, gbc);
        
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridwidth = 1;
        int row = 1;
        
        // Product ID
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🆔", "Product ID:"), gbc);
        JPanel idPanel = new JPanel(new BorderLayout(5, 0));
        idPanel.setBackground(PANEL_BG);
        txtTransportProductId = new JTextField(25);
        txtTransportProductId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportProductId.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JButton btnSearch = new JButton("🔍");
        btnSearch.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnSearch.setBackground(BUTTON_COLOR);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> showProductSelectorForTransport());
        idPanel.add(txtTransportProductId, BorderLayout.CENTER);
        idPanel.add(btnSearch, BorderLayout.EAST);
        gbc.gridx = 1;
        panel.add(idPanel, gbc);
        row++;
        
        // Receptor
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("👤", "Para:"), gbc);
        txtTransportReceiver = new JTextField(25);
        txtTransportReceiver.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportReceiver.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportReceiver, gbc);
        row++;
        
        // Origem
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("📍", "Origem:"), gbc);
        txtTransportOrigin = new JTextField("Lisboa", 25);
        txtTransportOrigin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportOrigin.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportOrigin, gbc);
        row++;
        
        // Destino
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🎯", "Destino:"), gbc);
        txtTransportDestination = new JTextField("Porto", 25);
        txtTransportDestination.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportDestination.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportDestination, gbc);
        row++;
        
        // Veículo
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("�", "Veículo:"), gbc);
        txtTransportVehicle = new JTextField("Camião Refrigerado AB-12-CD", 25);
        txtTransportVehicle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportVehicle.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportVehicle, gbc);
        row++;
        
        // Temperatura
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🌡️", "Temp(°C):"), gbc);
        txtTransportTemp = new JTextField("5.0", 25);
        txtTransportTemp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportTemp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportTemp, gbc);
        row++;
        
        // Password
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🔒", "Pass:"), gbc);
        txtTransportPassword = new JPasswordField("123qwe", 25);
        txtTransportPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTransportPassword.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtTransportPassword, gbc);
        row++;
        
        // Botão
        btnTransportTransfer = new JButton("✅ INICIAR TRANSPORTE");
        btnTransportTransfer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnTransportTransfer.setBackground(ACCENT_COLOR);
        btnTransportTransfer.setForeground(Color.WHITE);
        btnTransportTransfer.setFocusPainted(false);
        btnTransportTransfer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnTransportTransfer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTransportTransfer.addActionListener(e -> transportTransfer());
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(btnTransportTransfer, gbc);
        
        // Espaço em branco
        JPanel spacer = new JPanel();
        spacer.setBackground(PANEL_BG);
        gbc.gridx = 0; gbc.gridy = row + 1; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(spacer, gbc);
        
        return panel;
    }
    
    private JPanel createStorePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Painel de Título
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        JLabel title = new JLabel("<html><font size='6'>🎪</font> VENDER AO CLIENTE</html>");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 15, 0);
        panel.add(headerPanel, gbc);
        
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridwidth = 1;
        int row = 1;

        // Product ID
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🆔", "Product ID:"), gbc);
        JPanel idPanel = new JPanel(new BorderLayout(5, 0));
        idPanel.setBackground(PANEL_BG);
        txtStoreProductId = new JTextField(25);
        txtStoreProductId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtStoreProductId.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JButton btnSearch = new JButton("🔍");
        btnSearch.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnSearch.setBackground(BUTTON_COLOR);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> showProductSelectorForStore());
        idPanel.add(txtStoreProductId, BorderLayout.CENTER);
        idPanel.add(btnSearch, BorderLayout.EAST);
        gbc.gridx = 1;
        panel.add(idPanel, gbc);
        row++;

        // Vendedor/Responsável
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("👤", "Vendedor:"), gbc);
        txtStoreReceiver = new JTextField(user.getUserName(), 25);
        txtStoreReceiver.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtStoreReceiver.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtStoreReceiver, gbc);
        row++;

        // Preço de Venda
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("💰", "Preço(€):"), gbc);
        txtStorePrice = new JTextField("2.50", 25);
        txtStorePrice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtStorePrice.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtStorePrice, gbc);
        row++;
        
        // Temperatura
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🌡️", "Temp(°C):"), gbc);
        txtStoreTemp = new JTextField("6.0", 25);
        txtStoreTemp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtStoreTemp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtStoreTemp, gbc);
        row++;
        
        // Password
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(createEmojiLabel("🔒", "Pass:"), gbc);
        txtStorePassword = new JPasswordField("123qwe", 25);
        txtStorePassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtStorePassword.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        gbc.gridx = 1;
        panel.add(txtStorePassword, gbc);
        row++;
        
        // Botão
        btnStoreTransfer = new JButton("✅ RECEBER NA LOJA + QR");
        btnStoreTransfer.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        btnStoreTransfer.setBackground(ACCENT_COLOR);
        btnStoreTransfer.setForeground(Color.WHITE);
        btnStoreTransfer.setFocusPainted(false);
        btnStoreTransfer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnStoreTransfer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStoreTransfer.addActionListener(e -> storeTransfer());
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        panel.add(btnStoreTransfer, gbc);
        
        // Espaço em branco
        JPanel spacer = new JPanel();
        spacer.setBackground(PANEL_BG);
        gbc.gridx = 0; gbc.gridy = row + 1; gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(spacer, gbc);
        
        return panel;
    }
    
    private JPanel createProductListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        
        // Painel de Título e Botão
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel headerLabel = new JLabel("📋 HISTÓRICO DA BLOCKCHAIN");
        headerLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        btnRefresh = new JButton("🔄 ATUALIZAR");
        btnRefresh.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        btnRefresh.setBackground(BUTTON_COLOR);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> refreshProductList());
        headerPanel.add(btnRefresh, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        txtProductList = new JTextArea();
        txtProductList.setEditable(false);
        txtProductList.setFont(new Font("Courier New", Font.PLAIN, 11));
        txtProductList.setBackground(new Color(240, 240, 240));
        txtProductList.setForeground(TEXT_COLOR);
        txtProductList.setMargin(new Insets(10, 10, 10, 10));
        txtProductList.setLineWrap(true);
        txtProductList.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtProductList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        panel.add(scroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void createProduct() {
        try {
            String producerCompany = txtProducerCompany.getText();
            String productName = txtProductName.getText();
            String batchId = txtBatchId.getText();
            String category = txtCategory.getText();
            double quantity = Double.parseDouble(txtQuantity.getText());
            String unit = txtUnit.getText();
            String password = new String(txtProducerPassword.getPassword());
            
            long now = System.currentTimeMillis();
            long expiry = now + 14L * 24 * 60 * 60 * 1000; // 14 dias
            
            FoodProduct product = new FoodProduct(
                productName, batchId, category, quantity, unit,
                producerCompany, now, expiry
            );
            
            FoodTransaction tx = new FoodTransaction(user.getUserName(), product, password);
            remoteObject.addTransaction(Utils.ObjectToBase64(tx));
            
            JOptionPane.showMessageDialog(this, 
                "Produto criado com sucesso!\nProduct ID: " + product.getProductId(),
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
            // Limpar campos
            txtBatchId.setText("LOTE-" + System.currentTimeMillis());
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao criar produto: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void warehouseTransfer() {
        try {
            String productId = txtWarehouseProductId.getText();
            String receiver = txtWarehouseReceiver.getText();
            String password = new String(txtWarehousePassword.getPassword());
            
            FoodProduct originalProduct = findProductById(productId);
            if (originalProduct == null) {
                JOptionPane.showMessageDialog(this, 
                    "Produto não encontrado: " + productId,
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String notes = "Armazenado em: " + txtWarehouseZone.getText();
            FoodProduct updatedProduct = originalProduct.updateForTransfer(
                "Armazém",
                txtWarehouseZone.getText(),
                "Armazenado",
                notes,
                Double.parseDouble(txtWarehouseTemp.getText())
            );
            
            FoodTransaction tx = new FoodTransaction(
                user.getUserName(), receiver, updatedProduct, password
            );
            
            remoteObject.addTransaction(Utils.ObjectToBase64(tx));
            
            JOptionPane.showMessageDialog(this, 
                "Produto recebido no armazém com sucesso!",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao receber produto: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void transportTransfer() {
        try {
            String productId = txtTransportProductId.getText();
            String receiver = txtTransportReceiver.getText();
            String password = new String(txtTransportPassword.getPassword());
            
            FoodProduct originalProduct = findProductById(productId);
            if (originalProduct == null) {
                JOptionPane.showMessageDialog(this, 
                    "Produto não encontrado: " + productId,
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String notes = "Transporte: " + txtTransportOrigin.getText() + " → " + 
                          txtTransportDestination.getText() + " | Veículo: " + txtTransportVehicle.getText();
            FoodProduct updatedProduct = originalProduct.updateForTransfer(
                "Transporte",
                txtTransportDestination.getText(),
                "Em Trânsito",
                notes,
                Double.parseDouble(txtTransportTemp.getText())
            );
            
            FoodTransaction tx = new FoodTransaction(
                user.getUserName(), receiver, updatedProduct, password
            );
            
            remoteObject.addTransaction(Utils.ObjectToBase64(tx));
            
            JOptionPane.showMessageDialog(this, 
                "Transporte iniciado com sucesso!",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao iniciar transporte: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void storeTransfer() {
        try {
            String productId = txtStoreProductId.getText();
            String receiver = txtStoreReceiver.getText();
            String password = new String(txtStorePassword.getPassword());
            
            FoodProduct originalProduct = findProductById(productId);
            if (originalProduct == null) {
                JOptionPane.showMessageDialog(this, 
                    "Produto não encontrado: " + productId,
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String notes = "Preço de venda: €" + txtStorePrice.getText();
            FoodProduct updatedProduct = originalProduct.updateForTransferWithPrice(
                "Loja",
                "Loja",
                "À Venda",
                notes,
                Double.parseDouble(txtStoreTemp.getText()),
                Double.parseDouble(txtStorePrice.getText())
            );
            
            FoodTransaction tx = new FoodTransaction(
                user.getUserName(), receiver, updatedProduct, password
            );

            String txB64 = Utils.ObjectToBase64(tx);
            if (txB64 == null) {
                JOptionPane.showMessageDialog(this,
                    "Falha ao serializar transação para a blockchain.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            remoteObject.addTransaction(txB64);

            // Gerar hash curto da transação para referenciar no QR
            String txHash = sha256Hex(txB64).substring(0, 16);
            String payload = "lot=" + urlEncode(updatedProduct.getBatchId()) +
                             "&tx=" + urlEncode(txHash);

            // Gerar QR offline (ZXing). Se não houver ZXing, guardar payload em TXT como fallback.
            BufferedImage qrImage = generateQrImageOffline(payload, 320);
            Path outFile = null;

            if (qrImage != null) {
                // Guardar PNG localmente
                Path outDir = Path.of("qr");
                Files.createDirectories(outDir);
                String safeLot = updatedProduct.getBatchId().replaceAll("[^a-zA-Z0-9_-]", "_");
                outFile = outDir.resolve("qr_" + safeLot + ".png");
                ImageIO.write(qrImage, "png", outFile.toFile());

                // Mostrar QR num diálogo
                Image scaled = qrImage.getScaledInstance(240, 240, Image.SCALE_SMOOTH);
                JLabel qrLabel = new JLabel(new ImageIcon(scaled));
                qrLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                JOptionPane.showMessageDialog(this,
                    qrLabel,
                    "QR gerado — lote " + updatedProduct.getBatchId(),
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Fallback: guardar payload num TXT para gerar QR externamente, sem falhar a venda
                Path outDir = Path.of("qr");
                Files.createDirectories(outDir);
                String safeLot = updatedProduct.getBatchId().replaceAll("[^a-zA-Z0-9_-]", "_");
                outFile = outDir.resolve("qr_" + safeLot + ".txt");
                Files.writeString(outFile, payload, StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this,
                    "Transação gravada. ZXing não disponível para gerar QR.\n" +
                    "Payload guardado em: " + outFile.toAbsolutePath() + "\n" +
                    "Gera o QR offline com qualquer ferramenta usando esse texto.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            }

            String qrInfo = (outFile != null) ? "\nQR/Payload guardado em: " + outFile.toAbsolutePath() : "";

            JOptionPane.showMessageDialog(this, 
                "Produto recebido na loja com sucesso!\nPreço: €" + txtStorePrice.getText() + qrInfo,
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erro ao receber produto na loja: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    // Download da imagem do QR com timeout e user-agent simples
    private static BufferedImage fetchQrImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "FoodChain/1.0");
            conn.connect();
            try {
                return ImageIO.read(conn.getInputStream());
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        
        // Painel de Título e Botão
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel headerLabel = new JLabel("👤 RASTREAR PRODUTOS À VENDA");
        headerLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        JButton btnRefresh = new JButton("🔄 ATUALIZAR");
        btnRefresh.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        btnRefresh.setBackground(BUTTON_COLOR);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> refreshClientProductList());
        headerPanel.add(btnRefresh, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Painel com lista de produtos à venda (clickable)
        JPanel listPanel = new JPanel(new GridBagLayout());
        listPanel.setBackground(PANEL_BG);
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Armazenar referência para atualizar
        panel.putClientProperty("listPanel", listPanel);
        
        refreshClientProductList();
        
        return panel;
    }
    
    private void refreshClientProductList() {
        try {
            List<String> txtList = remoteObject.getRegisteredTransactions();
            java.util.List<FoodTransaction> storeProducts = new java.util.ArrayList<>();
            java.util.Map<String, List<FoodTransaction>> productHistory = new java.util.HashMap<>();
            
            // Recolher transações e agrupar por produto
            for (String txt : txtList) {
                try {
                    Object obj = Utils.base64ToObject(txt);
                    if (obj instanceof FoodTransaction) {
                        FoodTransaction tx = (FoodTransaction) obj;
                        String productId = tx.getProduct().getProductId();
                        productHistory.computeIfAbsent(productId, k -> new ArrayList<>()).add(tx);
                        
                        // Apenas adicionar se tiver transação de loja ("À Venda")
                        if ("À Venda".equals(tx.getProduct().getStatus())) {
                            storeProducts.add(tx);
                        }
                    }
                } catch (Exception e) {
                }
            }
            
            // Encontrar o componente listPanel no cliente
            JPanel clientTab = null;
            for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                if (tabbedPane.getComponent(i) instanceof JPanel) {
                    JPanel comp = (JPanel) tabbedPane.getComponent(i);
                    if (comp.getClientProperty("listPanel") != null) {
                        clientTab = comp;
                        break;
                    }
                }
            }
            
            if (clientTab != null) {
                JScrollPane scroll = (JScrollPane) clientTab.getComponent(1);
                JPanel listPanel = (JPanel) scroll.getViewport().getView();
                listPanel.removeAll();
                listPanel.setLayout(new GridBagLayout());
                
                if (storeProducts.isEmpty()) {
                    JLabel msgLabel = new JLabel("Nenhum produto à venda no momento.");
                    msgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                    msgLabel.setForeground(new Color(100, 100, 100));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 0;
                    gbc.insets = new Insets(20, 10, 20, 10);
                    listPanel.add(msgLabel, gbc);
                } else {
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridx = 0;
                    gbc.weightx = 1.0;
                    gbc.insets = new Insets(5, 5, 5, 5);
                    
                    for (int idx = 0; idx < storeProducts.size(); idx++) {
                        FoodTransaction tx = storeProducts.get(idx);
                        FoodProduct product = tx.getProduct();
                        
                        // Criar botão clickável estilo loja virtual (sem emoji)
                        JButton productBtn = new JButton("<html>" +
                            "<b style='font-size:14px'>" + product.getProductName() + "</b><br>" +
                            "<font size='3' color='gray'>" + product.getCategory() + "</font><br>" +
                            "<b style='font-size:13px' color='green'>€" + String.format("%.2f", product.getPrice()) + "</b>" +
                            "</html>");
                        productBtn.setHorizontalAlignment(SwingConstants.CENTER);
                        productBtn.setVerticalAlignment(SwingConstants.CENTER);
                        productBtn.setBackground(new Color(230, 250, 240));
                        productBtn.setForeground(TEXT_COLOR);
                        productBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        productBtn.setBorder(BorderFactory.createLineBorder(new Color(76, 175, 80), 3));
                        productBtn.setFocusPainted(false);
                        productBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        productBtn.setPreferredSize(new Dimension(200, 150));
                        
                        String productId = product.getProductId();
                        productBtn.addActionListener(e -> showProductTraceForClient(productId, productHistory));
                        
                        gbc.gridy = idx;
                        listPanel.add(productBtn, gbc);
                    }
                    
                    // Adicionar espaço em branco no final
                    JPanel spacer = new JPanel();
                    spacer.setBackground(PANEL_BG);
                    gbc.gridy = storeProducts.size();
                    gbc.weighty = 1.0;
                    listPanel.add(spacer, gbc);
                }
                
                listPanel.revalidate();
                listPanel.repaint();
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void showProductTraceForClient(String productId, java.util.Map<String, List<FoodTransaction>> productHistory) {
        try {
            List<FoodTransaction> history = productHistory.get(productId);
            if (history == null || history.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Histórico não encontrado.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Ordenar por timestamp
            history.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            
            // Construir o histórico em texto legível
            FoodProduct product = history.get(0).getProduct();
            StringBuilder trace = new StringBuilder();
            trace.append("═══════════════════════════════════════════════════════════\n");
            trace.append("🌾 RASTREAMENTO DO PRODUTO\n");
            trace.append("═══════════════════════════════════════════════════════════\n\n");
            trace.append("Produtor: ").append(product.getProductName()).append("\n");
            trace.append("Lote: ").append(product.getBatchId()).append("\n");
            trace.append("Produto: ").append(product.getCategory()).append("\n");
            trace.append("Quantidade: ").append(product.getQuantity()).append(" ").append(product.getUnit()).append("\n");
            trace.append("Origem: ").append(product.getOrigin()).append("\n");
            trace.append("\n───────────────────────────────────────────────────────────\n");
            trace.append("📍 CAMINHO DO PRODUTO\n");
            trace.append("───────────────────────────────────────────────────────────\n\n");
            
            for (int i = 0; i < history.size(); i++) {
                FoodTransaction tx = history.get(i);
                FoodProduct p = tx.getProduct();
                
                trace.append(String.format("%d. ", i + 1));
                if (tx.isCreateTransaction()) {
                    trace.append("🌱 CRIADO\n");
                    trace.append("   Produtor: ").append(tx.getTxtSender()).append("\n");
                } else {
                    // Para a última etapa, destacar Loja/Vendedor em vez de genérico Transferência
                    if (i == history.size() - 1) {
                        trace.append("🏪 LOJA/VENDEDOR\n");
                        trace.append("   Vendedor: ").append(tx.getTxtReceiver()).append("\n");
                    } else {
                        trace.append("🔄 TRANSFERÊNCIA\n");
                        trace.append("   De: ").append(tx.getTxtSender()).append(" → Para: ").append(tx.getTxtReceiver()).append("\n");
                    }
                }
                trace.append("   Estação: ").append(p.getCurrentStation()).append("\n");
                trace.append("   Estado: ").append(p.getStatus()).append("\n");
                trace.append("   Localização: ").append(p.getCurrentLocation()).append("\n");
                trace.append("   Temperatura: ").append(p.getTemperature()).append("°C\n");
                trace.append("   Notas: ").append(p.getNotes()).append("\n\n");
            }
            
            trace.append("═══════════════════════════════════════════════════════════\n");
            
            // Mostrar num diálogo com texto grande
            JTextArea textArea = new JTextArea(trace.toString());
            textArea.setFont(new Font("Courier New", Font.PLAIN, 11));
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBackground(new Color(245, 245, 245));
            textArea.setMargin(new Insets(10, 10, 10, 10));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 500));
            
            JOptionPane.showMessageDialog(this,
                scrollPane,
                "Rastreamento: " + product.getProductName() + " [" + product.getBatchId() + "]",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao mostrar histórico: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Tenta gerar QR offline usando ZXing (se estiver no classpath). Caso contrário, retorna null.
     * Para ativar, adicionar zxing-core e zxing-javase às libs do projeto.
     */
    private static BufferedImage generateQrImageOffline(String payload, int size) {
        try {
            Class<?> writerCls = Class.forName("com.google.zxing.qrcode.QRCodeWriter");
            Class<?> barcodeFormatCls = Class.forName("com.google.zxing.BarcodeFormat");
            Object qrCodeEnum = Enum.valueOf((Class<Enum>) barcodeFormatCls.asSubclass(Enum.class), "QR_CODE");
            Object writer = writerCls.getDeclaredConstructor().newInstance();
            Object bitMatrix = writerCls
                .getMethod("encode", String.class, barcodeFormatCls, int.class, int.class)
                .invoke(writer, payload, qrCodeEnum, size, size);

            Class<?> bitMatrixCls = bitMatrix.getClass();
            int width = (int) bitMatrixCls.getMethod("getWidth").invoke(bitMatrix);
            int height = (int) bitMatrixCls.getMethod("getHeight").invoke(bitMatrix);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    boolean on = (boolean) bitMatrixCls.getMethod("get", int.class, int.class).invoke(bitMatrix, x, y);
                    image.setRGB(x, y, on ? 0x000000 : 0xFFFFFF);
                }
            }
            return image;
        } catch (ClassNotFoundException e) {
            return null; // ZXing não está disponível
        } catch (Exception e) {
            return null;
        }
    }
    
    private FoodProduct findProductById(String productId) {
        FoodProduct latest = null;
        try {
            List<String> txtList = remoteObject.getRegisteredTransactions();
            for (String txt : txtList) {
                try {
                    Object obj = Utils.base64ToObject(txt);
                    if (obj instanceof FoodTransaction) {
                        FoodTransaction tx = (FoodTransaction) obj;
                        if (tx.getProduct().getProductId().equals(productId)) {
                            // Manter a última ocorrência (transação mais recente) deste productId
                            latest = tx.getProduct();
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception ex) {
        }
        return latest;
    }
    
    private void showProductSelectorForWarehouse() {
        selectProductAndFill(txtWarehouseProductId, txtWarehouseZone, txtWarehouseTemp, "Armazém");
    }
    
    private void showProductSelectorForTransport() {
        selectProductAndFill(txtTransportProductId, txtTransportDestination, txtTransportTemp, "Transporte");
    }
    
    private void showProductSelectorForStore() {
        selectProductAndFill(txtStoreProductId, null, txtStoreTemp, "Loja");
    }
    
    private void selectProductAndFill(JTextField productIdField, JTextField locationField, 
                                      JTextField tempField, String stationType) {
        try {
            List<String> txtList = remoteObject.getRegisteredTransactions();
            java.util.Map<String, FoodTransaction> latestTransactions = new java.util.LinkedHashMap<>();
            
            // Agrupar por productId e manter apenas a ÚLTIMA transação de cada produto
            for (String txt : txtList) {
                try {
                    Object obj = Utils.base64ToObject(txt);
                    if (obj instanceof FoodTransaction) {
                        FoodTransaction tx = (FoodTransaction) obj;
                        String productId = tx.getProduct().getProductId();
                        // Guardar a última transação de cada produto (última no loop = mais recente)
                        latestTransactions.put(productId, tx);
                    }
                } catch (Exception e) {
                }
            }
            
            if (latestTransactions.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Nenhum produto encontrado na blockchain.",
                    "Procurar Produtos", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            java.util.List<FoodTransaction> products = new java.util.ArrayList<>(latestTransactions.values());
            
            // Criar lista de produtos para seleção (mostrar o Product ID para confirmar que não muda)
            String[] options = new String[products.size()];
            for (int i = 0; i < products.size(); i++) {
                FoodTransaction tx = products.get(i);
                FoodProduct p = tx.getProduct();
                options[i] = String.format("%d. %s [%s] - %s @ %s | ID: %s",
                    i + 1,
                    p.getProductName(),
                    p.getBatchId(),
                    p.getStatus(),
                    p.getCurrentStation(),
                    p.getProductId()
                );
            }
            
            String selected = (String) JOptionPane.showInputDialog(
                this,
                "Selecione o produto para " + stationType + ":",
                "Procurar Produtos - " + stationType,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (selected != null) {
                // Extrair índice da seleção
                int index = Integer.parseInt(selected.split("\\.")[0]) - 1;
                FoodProduct selectedProduct = products.get(index).getProduct();
                
                // Preencher campos automaticamente
                productIdField.setText(selectedProduct.getProductId());
                if (locationField != null) {
                    locationField.setText(selectedProduct.getCurrentLocation());
                }
                tempField.setText(String.valueOf(selectedProduct.getTemperature()));
                
                JOptionPane.showMessageDialog(this,
                    "Produto selecionado:\n" +
                    "Nome: " + selectedProduct.getProductName() + "\n" +
                    "Lote: " + selectedProduct.getBatchId() + "\n" +
                    "Estado: " + selectedProduct.getStatus() + "\n" +
                    "Estação Atual: " + selectedProduct.getCurrentStation(),
                    "Produto Carregado - " + stationType,
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao procurar produtos: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshProductList() {
        try {
            List<String> txtList = remoteObject.getRegisteredTransactions();
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("PRODUTOS NA BLOCKCHAIN\n");
            sb.append("=".repeat(80)).append("\n\n");
            
            int count = 0;
            for (String txt : txtList) {
                try {
                    Object obj = Utils.base64ToObject(txt);
                    if (obj instanceof FoodTransaction) {
                        FoodTransaction tx = (FoodTransaction) obj;
                        count++;
                        sb.append(count).append(". ").append(tx.toString()).append("\n");
                        sb.append("-".repeat(80)).append("\n");
                    }
                } catch (Exception e) {
                }
            }
            
            if (count == 0) {
                sb.append("Nenhum produto encontrado na blockchain.\n");
            }
            
            txtProductList.setText(sb.toString());
            
        } catch (Exception ex) {
            txtProductList.setText("Erro ao carregar produtos: " + ex.getMessage());
        }
    }
}
