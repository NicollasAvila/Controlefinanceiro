import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final LocalDate data;

    public Transacao(String descricao, BigDecimal valor, TipoTransacao tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.data = LocalDate.now(); // Data atual
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

    public LocalDate getData() {
        return data;
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
    private int usuarioLogado;

    // Declaração da variável tabelaTransacoes
    private JTable tabelaTransacoes;

    public ControleFinanceiro() {
        transacoes = new ArrayList<>();
        saldo = BigDecimal.ZERO;
        entradas = BigDecimal.ZERO;
        saidas = BigDecimal.ZERO;

        mostrarTelaLogin();
        conectarBanco();
    }

    public void mostrarTelaLogin() {
        JFrame telaLogin = new JFrame("Login");
        telaLogin.setSize(300, 200);
        telaLogin.setLayout(new GridLayout(3, 2));

        JLabel labelUsuario = new JLabel("Usuário:");
        JTextField campoUsuario = new JTextField();
        JLabel labelSenha = new JLabel("Senha:");
        JPasswordField campoSenha = new JPasswordField();

        JButton botaoEntrar = new JButton("Entrar");
        botaoEntrar.addActionListener(e -> {
            String usuario = campoUsuario.getText();
            String senha = new String(campoSenha.getPassword());
            if (validarLogin(usuario, senha)) {
                telaLogin.dispose();
                initComponents();
                atualizarSaldo();
                atualizarLabelSaldo();

            } else {
                JOptionPane.showMessageDialog(telaLogin, "Usuário ou senha inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton botaoNovoUsuario = new JButton("Novo Usuário");
        botaoNovoUsuario.addActionListener(e -> mostrarTelaRegistro(telaLogin));

        telaLogin.add(labelUsuario);
        telaLogin.add(campoUsuario);
        telaLogin.add(labelSenha);
        telaLogin.add(campoSenha);
        telaLogin.add(botaoEntrar);
        telaLogin.add(botaoNovoUsuario);

        telaLogin.setLocationRelativeTo(null);
        telaLogin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        telaLogin.setVisible(true);
    }

    private void mostrarTelaRegistro(JFrame telaLogin) {
        JFrame telaRegistro = new JFrame("Registrar Novo Usuário");
        telaRegistro.setSize(300, 200);
        telaRegistro.setLayout(new GridLayout(3, 2));

        JLabel labelNovoUsuario = new JLabel("Usuário:");
        JTextField campoNovoUsuario = new JTextField();
        JLabel labelNovaSenha = new JLabel("Senha:");
        JPasswordField campoNovaSenha = new JPasswordField();

        JButton botaoRegistrar = new JButton("Registrar");
        botaoRegistrar.addActionListener(e -> {
            String novoUsuario = campoNovoUsuario.getText();
            String novaSenha = new String(campoNovaSenha.getPassword());

            if (novoUsuario.isEmpty() || novaSenha.isEmpty()) {
                JOptionPane.showMessageDialog(telaRegistro, "Usuário e senha não podem estar vazios.", "Erro", JOptionPane.ERROR_MESSAGE);
            } else {
                if (registrarNovoUsuario(novoUsuario, novaSenha)) {
                    JOptionPane.showMessageDialog(telaRegistro, "Usuário registrado com sucesso.");
                    telaRegistro.dispose();
                    telaLogin.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(telaRegistro, "Erro ao registrar novo usuário.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton botaoCancelar = new JButton("Cancelar");
        botaoCancelar.addActionListener(e -> telaRegistro.dispose());

        telaRegistro.add(labelNovoUsuario);
        telaRegistro.add(campoNovoUsuario);
        telaRegistro.add(labelNovaSenha);
        telaRegistro.add(campoNovaSenha);
        telaRegistro.add(botaoRegistrar);
        telaRegistro.add(botaoCancelar);

        telaRegistro.setLocationRelativeTo(null);
        telaRegistro.setVisible(true);
    }


    private boolean validarLogin(String usuario, String senha) {
        String sql = "SELECT * FROM usuario WHERE nome = ? AND senha = ?";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setString(2, senha);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    usuarioLogado = resultSet.getInt("idUser");
                    return true;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao validar login: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private boolean registrarNovoUsuario(String usuario, String senha) {
        String sql = "INSERT INTO usuario (nome, senha) VALUES (?, ?)";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setString(2, senha);
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registrar novo usuário: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void initComponents() {
        setTitle("Controle Financeiro Pessoal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Tela cheia
        setLayout(new BorderLayout(20, 20)); // Aumenta as margens

        JLabel titulo = new JLabel("Controle Financeiro Pessoal", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.BOLD, 36)); // Fonte maior e negrito
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0)); // Margens no título
        add(titulo, BorderLayout.NORTH);

        saldoLabel = new JLabel("", SwingConstants.CENTER);
        saldoLabel.setFont(new Font("SansSerif", Font.PLAIN, 24)); // Fonte maior
        atualizarLabelSaldo();
        saldoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Margens no saldo
        add(saldoLabel, BorderLayout.CENTER);

        add(criarPainelBotoes(), BorderLayout.PAGE_END);
        setLocationRelativeTo(null);
        setVisible(true);
    }



    private JPanel criarPainelBotoes() {
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));

        painelBotoes.add(criarBotao("Registrar Transação", e -> abrirJanelaTransacao()));
        painelBotoes.add(criarBotao("Visualizar Transações", e -> abrirJanelaVisualizarTransacoes()));
        painelBotoes.add(criarBotao("Logout", e -> logout()));
        painelBotoes.add(criarBotao("Sair", e -> System.exit(0)));
        painelBotoes.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0)); // Margens no painel de botões
        return painelBotoes;
    }

    private JButton criarBotao(String texto, ActionListener actionListener) {
        JButton botao = new JButton(texto);
        botao.addActionListener(actionListener);
        return botao;
    }

    private void logout() {
        // Reinicializa as variáveis de saldo, entradas e saídas
        saldo = BigDecimal.ZERO;
        entradas = BigDecimal.ZERO;
        saidas = BigDecimal.ZERO;

        // Limpa o texto do saldoLabel
        saldoLabel.setText("");

        // Fecha a tela principal
        dispose();

        // Mostra a tela de login
        mostrarTelaLogin();
    }


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

    private void adicionarCampoEntrada(JPanel painel, GridBagConstraints gbc, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy++;
        painel.add(new JLabel(rotulo), gbc);

        gbc.gridx = 1;
        painel.add(campo, gbc);
    }

    private void abrirJanelaVisualizarTransacoes() {
        JFrame janelaVisualizarTransacoes = new JFrame("Visualizar Transações");
        janelaVisualizarTransacoes.setSize(800, 600);
        janelaVisualizarTransacoes.setExtendedState(JFrame.MAXIMIZED_BOTH); // Tela cheia
        janelaVisualizarTransacoes.setLayout(new BorderLayout(20, 20)); // Aumenta as margens

        // Painel de filtros
        JPanel painelFiltros = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 10); // Espaçamento entre os componentes

        JTextField campoDescricao = new JTextField(20); // Aumenta o tamanho dos campos de texto
        JTextField campoDataInicio = new JTextField(20);
        JTextField campoDataFim = new JTextField(20);
        JComboBox<TipoTransacao> comboTipo = new JComboBox<>(TipoTransacao.values());
        JButton botaoPesquisar = criarBotao("Pesquisar", ev -> atualizarTabelaTransacoes(campoDescricao.getText(), campoDataInicio.getText(), campoDataFim.getText(), (TipoTransacao) comboTipo.getSelectedItem()));

        adicionarCampoEntrada(painelFiltros, gbc, "Descrição:", campoDescricao);
        adicionarCampoEntrada(painelFiltros, gbc, "Data Início (yyyy-mm-dd):", campoDataInicio);
        adicionarCampoEntrada(painelFiltros, gbc, "Data Fim (yyyy-mm-dd):", campoDataFim);
        adicionarCampoEntrada(painelFiltros, gbc, "Tipo:", comboTipo);
        adicionarCampoEntrada(painelFiltros, gbc, "", botaoPesquisar);

        // Tabela de transações
        String[] colunas = {"Descrição", "Valor (R$)", "Tipo", "Data", ""}; // Coluna de checkbox sem título
        DefaultTableModel modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaTransacoes = new JTable(modeloTabela) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) {
                    return Boolean.class;
                }
                return super.getColumnClass(column);
            }
        };
        tabelaTransacoes.setRowHeight(25); // Aumenta a altura das linhas da tabela

        JScrollPane scrollPaneTabela = new JScrollPane(tabelaTransacoes);

        // Botão de gerar PDF
        JButton botaoGerarPdf = criarBotao("Gerar extrato", ev -> gerarRelatorioPDF(campoDataInicio.getText(), campoDataFim.getText(), (TipoTransacao) comboTipo.getSelectedItem()));

        // Botão de excluir transações selecionadas
        JButton botaoExcluir = criarBotao("Excluir Selecionadas", ev -> excluirTransacoesSelecionadas());

        // Botão de limpar dados
        JButton botaoLimparDados = criarBotao("Limpar Dados", e -> limparDadosTransacao());

        // Botão de voltar
        JButton botaoVoltar = criarBotao("Voltar", ev -> janelaVisualizarTransacoes.dispose());

        JPanel painelBotoesInferiores = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20)); // Espaçamento entre os botões
        painelBotoesInferiores.add(botaoGerarPdf);
        painelBotoesInferiores.add(botaoExcluir);
        painelBotoesInferiores.add(botaoLimparDados);
        painelBotoesInferiores.add(botaoVoltar);

        janelaVisualizarTransacoes.add(painelFiltros, BorderLayout.NORTH);
        janelaVisualizarTransacoes.add(scrollPaneTabela, BorderLayout.CENTER);
        janelaVisualizarTransacoes.add(painelBotoesInferiores, BorderLayout.SOUTH);

        janelaVisualizarTransacoes.setLocationRelativeTo(this);
        janelaVisualizarTransacoes.setVisible(true);
        atualizarTabelaTransacoes("", "", "", null);
    }

    private void atualizarTabelaTransacoes(String descricao, String dataInicio, String dataFim, TipoTransacao tipo) {
        DefaultTableModel modeloTabela = (DefaultTableModel) tabelaTransacoes.getModel();
        modeloTabela.setRowCount(0); // Limpar tabela

        String sql = "SELECT descricao, valor, tipo, data FROM transacoes WHERE IdUser = ?";
        if (!descricao.isEmpty()) {
            sql += " AND descricao LIKE ?";
        }
        if (!dataInicio.isEmpty()) {
            sql += " AND data >= ?";
        }
        if (!dataFim.isEmpty()) {
            sql += " AND data <= ?";
        }
        if (tipo != null) {
            sql += " AND tipo = ?";
        }

        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setInt(1, usuarioLogado); // Define o idUser
            int parameterIndex = 2;

            if (!descricao.isEmpty()) {
                statement.setString(parameterIndex++, "%" + descricao + "%");
            }
            if (!dataInicio.isEmpty()) {
                statement.setString(parameterIndex++, dataInicio);
            }
            if (!dataFim.isEmpty()) {
                statement.setString(parameterIndex++, dataFim);
            }
            if (tipo != null) {
                statement.setString(parameterIndex++, tipo.name());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String descricaoTransacao = resultSet.getString("descricao");
                    BigDecimal valor = resultSet.getBigDecimal("valor");
                    TipoTransacao tipoTransacao = TipoTransacao.valueOf(resultSet.getString("tipo"));
                    LocalDate data = resultSet.getDate("data").toLocalDate();

                    modeloTabela.addRow(new Object[]{descricaoTransacao, valor, tipoTransacao, data, false});
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar transações: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }




    private void excluirTransacoesSelecionadas() {
        DefaultTableModel modeloTabela = (DefaultTableModel) tabelaTransacoes.getModel();
        List<Integer> linhasParaExcluir = new ArrayList<>();

        for (int i = 0; i < modeloTabela.getRowCount(); i++) {
            Boolean excluir = (Boolean) modeloTabela.getValueAt(i, 4);
            if (excluir != null && excluir) {
                linhasParaExcluir.add(i);
            }
        }

        if (linhasParaExcluir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma transação selecionada para exclusão.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(this, "Tem certeza de que deseja excluir as transações selecionadas?", "Confirmação", JOptionPane.YES_NO_OPTION);
        if (confirmacao == JOptionPane.YES_OPTION) {
            try {
                conexao.setAutoCommit(false);
                for (int i = linhasParaExcluir.size() - 1; i >= 0; i--) {
                    int linha = linhasParaExcluir.get(i);
                    String descricao = (String) modeloTabela.getValueAt(linha, 0);
                    BigDecimal valor = (BigDecimal) modeloTabela.getValueAt(linha, 1);
                    TipoTransacao tipo = (TipoTransacao) modeloTabela.getValueAt(linha, 2);
                    LocalDate data = (LocalDate) modeloTabela.getValueAt(linha, 3);

                    String sql = "DELETE FROM transacoes WHERE descricao = ? AND valor = ? AND tipo = ? AND data = ?";
                    try (PreparedStatement statement = conexao.prepareStatement(sql)) {
                        statement.setString(1, descricao);
                        statement.setBigDecimal(2, valor);
                        statement.setString(3, tipo.name());
                        statement.setDate(4, Date.valueOf(data));
                        statement.executeUpdate();
                    }

                    modeloTabela.removeRow(linha); // Remover da tabela após excluir do banco
                }
                conexao.commit();
                conexao.setAutoCommit(true);
                atualizarSaldo();
                JOptionPane.showMessageDialog(this, "Transações excluídas com sucesso.");
            } catch (SQLException ex) {
                try {
                    conexao.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Erro ao excluir transações: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerarRelatorioPDF(String dataInicio, String dataFim, TipoTransacao tipo) {


        try {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("DATA_INICIO", dataInicio);
            parametros.put("DATA_FIM", dataFim);
            parametros.put("TIPO_TRANSACAO", tipo != null ? tipo.name() : null);
            parametros.put("ID_USER", usuarioLogado); // Adiciona o ID do usuário logado

            JasperReport relatorioCompilado = JasperCompileManager.compileReport("Relatorios/Extrato.jrxml");
            JasperPrint relatorioPreenchido = JasperFillManager.fillReport(relatorioCompilado, parametros, conexao);

            if (relatorioPreenchido.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(this, "O relatório está vazio. Verifique se há dados no banco de dados.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog telaRelatorio = new JDialog(this, "Relatório de Transações", true);
            telaRelatorio.setSize(800, 600);

            JRViewer painelRelatorio = new JRViewer(relatorioPreenchido);
            telaRelatorio.getContentPane().add(painelRelatorio);

            telaRelatorio.setVisible(true);
        } catch (JRException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar o relatório: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    private void limparDadosTransacao() {
        int confirmacao = JOptionPane.showConfirmDialog(this, "Tem certeza de que deseja limpar todos os dados de transação?", "Confirmação", JOptionPane.YES_NO_OPTION);
        if (confirmacao == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM transacoes WHERE IdUser = ?";
            try (PreparedStatement statement = conexao.prepareStatement(sql)) {
                statement.setInt(1, usuarioLogado);
                statement.executeUpdate();
                transacoes.clear();
                saldo = BigDecimal.ZERO;
                entradas = BigDecimal.ZERO;
                saidas = BigDecimal.ZERO;
                atualizarLabelSaldo();
                JOptionPane.showMessageDialog(this, "Dados de transação limpos com sucesso.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void registrarTransacao(Transacao transacao) {
        transacoes.add(transacao);
        inserirNoBanco(transacao);
        atualizarSaldo();
    }

    private void inserirNoBanco(Transacao transacao) {
        String sql = "INSERT INTO transacoes (descricao, valor, tipo, data, IdUser) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conexao.prepareStatement(sql)) {
            statement.setString(1, transacao.getDescricao());
            statement.setBigDecimal(2, transacao.getValor());
            statement.setString(3, transacao.getTipo().name());
            statement.setDate(4, java.sql.Date.valueOf(transacao.getData()));
            statement.setInt(5, usuarioLogado);
            statement.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao inserir transação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void atualizarSaldo() {
        try {
            String sql = "SELECT valor, tipo FROM transacoes WHERE idUser = ?";
            PreparedStatement statement = conexao.prepareStatement(sql);
            statement.setInt(1, usuarioLogado); // Definindo o parâmetro idUser

            ResultSet resultSet = statement.executeQuery();

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
                SwingUtilities.invokeLater(() -> {

                    saldoLabel.setText("<html><div style='text-align: center; font-size: 20px;'>Saldo Atual</div><br><div style='font-size: 36px; text-align: center;'>R$ " + decimalFormat.format(saldo) + "</div><br><div style='text-align: center;'>Entradas: <font color='green'>R$ " + decimalFormat.format(entradas) + "</font> | Saídas: <font color='red'>R$ " + decimalFormat.format(saidas) + "</font></div></html>");
                });
            }

            saldo = totalEntradas.subtract(totalSaidas);
            entradas = totalEntradas;
            saidas = totalSaidas;
            atualizarLabelSaldo();

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }




    private void conectarBanco() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexao = DriverManager.getConnection("jdbc:mysql://www.welisondavi.com.br/welisond_nicolla", "welisond_nicolla", "@Nico3044");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarLabelSaldo() {
        saldoLabel.setText(""); // Limpa o texto anterior
        saldoLabel.setText("<html><div style='text-align: center; font-size: 20px;'>Saldo Atual</div><br><div style='font-size: 36px; text-align: center;'>R$ " + decimalFormat.format(saldo) + "</div><br><div style='text-align: center;'>Entradas: <font color='green'>R$ " + decimalFormat.format(entradas) + "</font> | Saídas: <font color='red'>R$ " + decimalFormat.format(saidas) + "</font></div></html>");

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ControleFinanceiro().setVisible(true));
    }
}