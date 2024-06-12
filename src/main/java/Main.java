import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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
        JFrame janelaRelatorio = new JFrame("Relatório de Transações");
        janelaRelatorio.setSize(500, 400);

        try (PreparedStatement statement = conexao.prepareStatement("SELECT descricao, valor, tipo FROM transacoes");
             ResultSet resultSet = statement.executeQuery()) {
            DefaultTableModel modeloTabela = new DefaultTableModel();
            JTable tabelaRelatorio = new JTable(modeloTabela);
            modeloTabela.setColumnIdentifiers(new Object[]{"Descrição", "Valor (R$)", "Tipo"});

            while (resultSet.next()) {
                String descricao = resultSet.getString("descricao");
                BigDecimal valor = resultSet.getBigDecimal("valor");
                TipoTransacao tipo = TipoTransacao.valueOf(resultSet.getString("tipo"));
                modeloTabela.addRow(new Object[]{descricao, "R$ " + decimalFormat.format(valor), tipo});
            }

            janelaRelatorio.add(new JScrollPane(tabelaRelatorio));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex);
        }

        JButton botaoLimpar = criarBotao("Limpar Dados", ev -> {
            int confirmacao = JOptionPane.showConfirmDialog(janelaRelatorio, "Tem certeza de que deseja limpar todos os dados de transação?", "Confirmação", JOptionPane.YES_NO_OPTION);
            if (confirmacao == JOptionPane.YES_OPTION) {
                limparDadosTransacao();
                JOptionPane.showMessageDialog(janelaRelatorio, "Dados de transação limpos com sucesso.");
                janelaRelatorio.dispose();
                abrirJanelaPrincipal();
            }
        });

        JButton botaoVoltar = criarBotao("Voltar", ev -> {
            janelaRelatorio.dispose();
            abrirJanelaPrincipal();
        });

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        painelBotoes.add(botaoLimpar);
        painelBotoes.add(botaoVoltar);
        janelaRelatorio.add(painelBotoes, BorderLayout.PAGE_END);

        janelaRelatorio.setLocationRelativeTo(this);
        janelaRelatorio.setVisible(true);
    }

    // Limpa todos os dados de transações do banco de dados
    private void limparDadosTransacao() {
        try (Statement statement = conexao.createStatement()) {
            statement.executeUpdate("DELETE FROM transacoes");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, ex);
        }
        transacoes.clear();
        saldo = BigDecimal.ZERO;
        entradas = BigDecimal.ZERO;
        saidas = BigDecimal.ZERO;
        atualizarLabelSaldo();
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
            JOptionPane.showMessageDialog(null, ex);
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
            JOptionPane.showMessageDialog(null, ex);
        }
    }

    // Conecta ao banco de dados
    private void conectarBanco() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexao = DriverManager.getConnection("jdbc:mysql://www.welisondavi.com.br/welisond_nicolla","welisond_nicolla", "@Nico3044");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex);
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
