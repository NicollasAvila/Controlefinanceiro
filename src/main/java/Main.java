import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.swing.JRViewer;

// Enum para representar os tipos de transação
enum TipoTransacao {
    RECEITA, DESPESA
}

// Classe que representa uma transação
class Transacao {
    private final String descricao;
    private final BigDecimal valor;
    private final TipoTransacao tipo;

    public Transacao(String descricao, BigDecimal valor, TipoTransacao tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }
}

// Classe principal do aplicativo de controle financeiro
class ControleFinanceiro extends JFrame {
    private final List<Transacao> transacoes;
    private BigDecimal saldo;
    private BigDecimal entradas;
    private BigDecimal saidas;
    private JLabel saldoLabel;
    private Connection conexao;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public ControleFinanceiro() {
        transacoes = new ArrayList<>();
        saldo = BigDecimal.ZERO;
        entradas = BigDecimal.ZERO;
        saidas = BigDecimal.ZERO;
        initComponents();
        conectarBanco();
        atualizarSaldo();
    }

    // Inicializa os componentes da interface gráfica
    private void initComponents() {
        setTitle("Controle Financeiro Pessoal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);

        JLabel titulo = new JLabel("Controle Financeiro Pessoal", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.PLAIN, 24));
        add(titulo, BorderLayout.NORTH);

        saldoLabel = new JLabel("", SwingConstants.CENTER);
        saldoLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        atualizarLabelSaldo();
        add(saldoLabel, BorderLayout.CENTER);

        add(criarPainelBotoes(), BorderLayout.PAGE_END);
        setLocationRelativeTo(null);
    }

    // Cria o painel de botões na parte inferior da janela
    private JPanel criarPainelBotoes() {
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));

        painelBotoes.add(criarBotao("Registrar Transação", e -> abrirJanelaTransacao()));
        painelBotoes.add(criarBotao("Gerar Relatório", e -> abrirJanelaRelatorio()));
        painelBotoes.add(criarBotao("Limpar Dados", e -> limparDadosTransacao()));
        painelBotoes.add(criarBotao("Sair", e -> System.exit(0)));

        return painelBotoes;
    }

    // Método auxiliar para criar um botão com ação associada
    private JButton criarBotao(String texto, ActionListener actionListener) {
        JButton botao = new JButton(texto);
        botao.addActionListener(actionListener);
        return botao;
    }

    // Abre a janela para registrar uma nova transação
    private void abrirJanelaTransacao() {
        JFrame janelaTransacao = new JFrame("Registrar Transação");
        janelaTransacao.setSize(400, 300);
        janelaTransacao.setLayout(new BorderLayout());

        JPanel painelEntrada = new JPanel();
        painelEntrada.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField campoDescricao = new JTextField(20);
        JTextField campoValor = new JTextField(20);
        JComboBox<TipoTransacao> comboTipo = new JComboBox<>(TipoTransacao.values());

        adicionarCampoEntrada(painelEntrada, gbc, "Descrição:", campoDescricao);
        adicionarCampoEntrada(painelEntrada, gbc, "Valor (R$):", campoValor);
        adicionarCampoEntrada(painelEntrada, gbc, "Tipo:", comboTipo);

        JButton botaoConfirmar = criarBotao("Confirmar", ev -> {
            try {
                String valorTexto = campoValor.getText().replace(",", "."); // Substitui vírgula por ponto
                BigDecimal valor = new BigDecimal(valorTexto);
                Transacao transacao = new Transacao(campoDescricao.getText(), valor, (TipoTransacao) comboTipo.getSelectedItem());
                registrarTransacao(transacao);
                JOptionPane.showMessageDialog(janelaTransacao, "Transação registrada com sucesso.");
                janelaTransacao.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(janelaTransacao, "Por favor, insira um valor válido.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton botaoCancelar = criarBotao("Cancelar", ev -> janelaTransacao.dispose());

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotoes.add(botaoConfirmar);
        painelBotoes.add(botaoCancelar);

        janelaTransacao.add(painelEntrada, BorderLayout.CENTER);
        janelaTransacao.add(painelBotoes, BorderLayout.PAGE_END);

        janelaTransacao.setLocationRelativeTo(this);
        janelaTransacao.setVisible(true);
    }

    // Adiciona um campo de entrada no painel
    private void adicionarCampoEntrada(JPanel painel, GridBagConstraints gbc, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy++;
        painel.add(new JLabel(rotulo), gbc);

        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    // Abre a janela de relatório de transações
    private void abrirJanelaRelatorio() {
        try {
            // Compila o relatório a partirado arquivo .jrxml
            JasperReport relatorioCompilado = JasperCompileManager.compileReport("Relatorios/Extrato.jrxml");

            // Preenche o relatório com os dados do banco de dados
            JasperPrint relatorioPreenchido = JasperFillManager.fillReport(relatorioCompilado, null, conexao);

            // Verifica se há páginas no relatório preenchido
            if (relatorioPreenchido.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(this, "O relatório está vazio. Verifique se há dados no banco de dados.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Cria uma janela de diálogo para exibir o relatório
            JDialog telaRelatorio = new JDialog(this, "Relatório de Transações", true);
            telaRelatorio.setSize(800, 600);

            // Adiciona o painel do visualizador de relatórios Jasper na janela de diálogo
            JRViewer painelRelatorio = new JRViewer(relatorioPreenchido);
            telaRelatorio.getContentPane().add(painelRelatorio);

            // Define a janela de diálogo como visível
            telaRelatorio.setVisible(true);
        } catch (JRException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar o relatório: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }

        // Cria uma lista de parâmetros, se necessário (pode ser null se não houver parâmetros)
        Map<String, Object> parametros = new HashMap<>();

        // Cria uma lista de dados para o relatório
        List<Map<String, ?>> data = new ArrayList<>();
        for (Transacao transacao : transacoes) {
            Map<String, Object> map = new HashMap<>();
            map.put("descricao", transacao.getDescricao());
            map.put("valor", transacao.getValor());
            map.put("tipo", transacao.getTipo().name());
            data.add(map);
        }
    }

    // Limpa todos os dados de transações do banco de dados
    private void limparDadosTransacao() {
        int confirmacao = JOptionPane.showConfirmDialog(this, "Tem certeza de que deseja limpar todos os dados de transação?", "Confirmação", JOptionPane.YES_NO_OPTION);
        if (confirmacao == JOptionPane.YES_OPTION) {
            try (Statement statement = conexao.createStatement()) {
                statement.executeUpdate("DELETE FROM transacoes");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
            transacoes.clear();
            saldo = BigDecimal.ZERO;
            entradas = BigDecimal.ZERO;
            saidas = BigDecimal.ZERO;
            atualizarLabelSaldo();
            JOptionPane.showMessageDialog(this, "Dados de transação limpos com sucesso.");
        }
    }

    // Reabre a janela principal
    private void abrirJanelaPrincipal() {
        this.setVisible(true);
    }

    // Registra uma nova transação no banco de dados e atualiza o saldo
    private void registrarTransacao(Transacao transacao) {
        transacoes.add(transacao);
        inserirNoBanco(transacao);
        atualizarSaldo();
    }

    // Insere uma nova transação no banco de dados
    private void inserirNoBanco(Transacao transacao) {
        try (PreparedStatement statement = conexao.prepareStatement("INSERT INTO transacoes (descricao, valor, tipo) VALUES (?, ?, ?)")) {
            statement.setString(1, transacao.getDescricao());
            statement.setBigDecimal(2, transacao.getValor());
            statement.setString(3, transacao.getTipo().name());
            statement.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Atualiza o saldo com base nas transações registradas
    private void atualizarSaldo() {
        try (PreparedStatement statement = conexao.prepareStatement("SELECT valor, tipo FROM transacoes");
             ResultSet resultSet = statement.executeQuery()) {
            BigDecimal totalEntradas = BigDecimal.ZERO;
            BigDecimal totalSaidas = BigDecimal.ZERO;

            while (resultSet.next()) {
                BigDecimal valor = resultSet.getBigDecimal("valor");
                TipoTransacao tipo = TipoTransacao.valueOf(resultSet.getString("tipo"));
                if (tipo == TipoTransacao.RECEITA) {
                    totalEntradas = totalEntradas.add(valor);
                } else {
                    totalSaidas = totalSaidas.add(valor);
                }
            }

            saldo = totalEntradas.subtract(totalSaidas);
            entradas = totalEntradas;
            saidas = totalSaidas;
            atualizarLabelSaldo();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Conecta ao banco de dados
    private void conectarBanco() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexao = DriverManager.getConnection("jdbc:mysql://www.welisondavi.com.br/welisond_nicolla", "welisond_nicolla", "@Nico3044");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Atualiza o rótulo do saldo na interface gráfica
    private void atualizarLabelSaldo() {
        saldoLabel.setText("<html><div style='text-align: center; font-size: 20px;'>Saldo Atual</div><br><div style='font-size: 36px; text-align: center;'>R$ " + decimalFormat.format(saldo) + "</div><br><div style='text-align: center;'>Entradas: <font color='green'>R$ " + decimalFormat.format(entradas) + "</font> | Saídas: <font color='red'>R$ " + decimalFormat.format(saidas) + "</font></div></html>");
    }

    // Método principal para iniciar a aplicação
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControleFinanceiro().setVisible(true));
    }
}
