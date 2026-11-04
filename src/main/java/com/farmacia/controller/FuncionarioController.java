package com.farmacia.controller;

import com.farmacia.model.Funcionario;
import com.farmacia.repository.FuncionarioRepository;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.EstadoLista;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Mascaras;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoFuncionario;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.Predicate;

// Controller da lista de Funcionários (tema v2). Cadastro e edição ficam em FuncionarioFormView.
public class FuncionarioController {

    private static final String TODOS_OS_CARGOS = "Todos os cargos";
    private static final String SEM_CARGO = "Sem cargo definido";
    private static final String CHAVE_ESTADO = "funcionarios";

    private static boolean dicaDispensada = false;

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;
    @FXML
    private Button btnAjuda;
    @FXML
    private HBox dicaBox;
    @FXML
    private StackPane dicaIcone;
    @FXML
    private Label lblResumoLista;
    @FXML
    private TextField txtFiltroFuncionarios;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnNovoFuncionario;
    @FXML
    private ComboBox<String> cbCargoFiltro;
    @FXML
    private TableView<Funcionario> tableFuncionarios;
    @FXML
    private TableColumn<Funcionario, Funcionario> colCodigo;
    @FXML
    private TableColumn<Funcionario, Funcionario> colNome;
    @FXML
    private TableColumn<Funcionario, Funcionario> colCargo;
    @FXML
    private TableColumn<Funcionario, Funcionario> colTelefone;
    @FXML
    private TableColumn<Funcionario, Funcionario> colCpf;
    @FXML
    private TableColumn<Funcionario, Funcionario> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final FuncionarioRepository repositorio = FuncionarioRepository.getInstance();
    private FilteredList<Funcionario> funcionariosFiltrados;
    // Evita reagir às trocas de valor que o próprio controller faz ao remontar a lista de cargos.
    private boolean atualizandoCargos = false;

    // Sem ordenação escolhida na tabela, a lista segue a ordem alfabética (mais fácil de achar alguém no balcão).
    private static final Comparator<Funcionario> ORDEM_PADRAO = Comparator.comparing(
            (Funcionario f) -> minusculo(f.getNome()));

    @FXML
    private void initialize() {
        com.farmacia.ui.Seletores.comIcone(cbCargoFiltro, "funcionarios");
        menuLateralController.marcarAtivo(MenuLateralController.FUNCIONARIOS);

        btnAjuda.setGraphic(Botoes.composto("ajuda", "Ajuda", "F1"));
        btnAjuda.setAccessibleText("Ajuda");
        btnNovoFuncionario.setGraphic(Botoes.composto("mais", "Novo funcionário", "Ctrl N"));
        btnNovoFuncionario.setAccessibleText("Novo funcionário");
        dicaIcone.getChildren().add(Icones.criar("info", 18));
        dicaBox.setVisible(!dicaDispensada);
        dicaBox.setManaged(!dicaDispensada);
        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");

        txtFiltroFuncionarios.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            atualizarTudo();
        });
        cbCargoFiltro.setAccessibleText("Filtrar por cargo");
        cbCargoFiltro.valueProperty().addListener((obs, antigo, novo) -> {
            if (!atualizandoCargos) {
                atualizarTudo();
            }
        });

        funcionariosFiltrados = new FilteredList<>(repositorio.getItens(), f -> true);
        atualizarCargos();
        configurarTabela();
        repositorio.getItens().addListener((ListChangeListener<Funcionario>) c -> {
            atualizarCargos();
            atualizarTudo();
        });
        AtalhosLista.instalar(rootStack, txtFiltroFuncionarios, tableFuncionarios, this::handleNovoFuncionario,
                this::editarFuncionario, this::pedirExclusao, this::handleAjuda);

        // Voltando de um cadastro, a lista continua com a busca, o cargo, a ordem e a linha de antes.
        EstadoLista.Estado anterior = EstadoLista.retomar(CHAVE_ESTADO);
        if (anterior != null) {
            if (anterior.extra("cargo") instanceof String cargo && cbCargoFiltro.getItems().contains(cargo)) {
                cbCargoFiltro.setValue(cargo);
            }
            EstadoLista.aplicar(anterior, txtFiltroFuncionarios, null, tableFuncionarios);
        } else {
            ordenarPeloPadrao();
        }
        atualizarTudo();
        exibirAvisoPendente();
    }

    private void guardarEstado() {
        EstadoLista.guardar(CHAVE_ESTADO, EstadoLista.capturar(txtFiltroFuncionarios, null, tableFuncionarios)
                .extra("cargo", cbCargoFiltro.getValue()));
    }

    // ===================== Tabela =====================

    private void configurarTabela() {
        Celulas.coluna(colCodigo, f -> Celulas.numero(f.getCodigo()), Comparator.comparing(Funcionario::getCodigo));
        Celulas.coluna(colNome, this::celulaNome, Comparator.comparing((Funcionario f) -> minusculo(f.getNome())));
        Celulas.coluna(colCargo, f -> Celulas.texto(f.getCargo()), Comparator.comparing((Funcionario f) -> minusculo(f.getCargo())));
        Celulas.coluna(colTelefone, f -> Celulas.texto(f.getTelefone()), null);
        Celulas.coluna(colCpf, f -> Celulas.texto(mascararCpf(f.getCpf())), null);
        Celulas.coluna(colAcoes, this::celulaAcoes, null);
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        SortedList<Funcionario> ordenados = new SortedList<>(funcionariosFiltrados);
        ordenados.comparatorProperty().bind(Bindings.createObjectBinding(
                () -> tableFuncionarios.getComparator() == null ? ORDEM_PADRAO : tableFuncionarios.getComparator(),
                tableFuncionarios.comparatorProperty()));
        // Quem ordena é a SortedList (acima); sem isto a política padrão da tabela reverte o "sem ordenação" ao
        // tirar a ordem da coluna, porque o comparador padrão não é o da própria tabela.
        tableFuncionarios.setSortPolicy(tabela -> true);
        tableFuncionarios.setItems(ordenados);

        tableFuncionarios.setRowFactory(tv -> {
            TableRow<Funcionario> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editarFuncionario(linha.getItem());
                }
            });
            return linha;
        });
    }

    // Ordem inicial: nome crescente, com a seta no título da coluna (como em Receitas); é a mesma ordem de ORDEM_PADRAO.
    // Voltando de um cadastro, vale a ordem guardada pelo EstadoLista.
    private void ordenarPeloPadrao() {
        colNome.setSortType(TableColumn.SortType.ASCENDING);
        tableFuncionarios.getSortOrder().add(colNome);
    }

    // Nome em destaque e, logo abaixo, o login em fonte discreta (é um identificador, não o dado principal).
    private Node celulaNome(Funcionario f) {
        Label nome = new Label(f.getNome());
        nome.getStyleClass().add("v2-link-neutro");
        nome.setTooltip(new Tooltip(f.getNome()));
        HBox linha1 = new HBox(8, nome);
        linha1.setAlignment(Pos.CENTER_LEFT);
        VBox caixa = new VBox(2, linha1);

        Label login = new Label(vazio(f.getLogin()) ? "sem login" : f.getLogin());
        login.getStyleClass().add("v2-sub");
        login.setTooltip(new Tooltip("Login: " + (vazio(f.getLogin()) ? "não informado" : f.getLogin())));
        caixa.getChildren().add(login);

        String faltas = camposFaltando(f);
        if (!faltas.isEmpty()) {
            Node alerta = Icones.criar("alerta", 16);
            alerta.getStyleClass().add("v2-icone-alerta");
            Label marca = new Label();
            marca.setGraphic(alerta);
            marca.setTooltip(new Tooltip("Cadastro incompleto: " + faltas));
            marca.setAccessibleText("Cadastro incompleto: " + faltas);
            linha1.getChildren().add(marca);
        }
        return caixa;
    }

    // Itens que fazem falta para um cadastro completo (aparecem no aviso do ícone de alerta).
    private String camposFaltando(Funcionario f) {
        List<String> faltas = new ArrayList<>();
        if (vazio(f.getCpf())) {
            faltas.add("CPF não informado");
        }
        if (vazio(f.getTelefone())) {
            faltas.add("telefone não informado");
        }
        return String.join("; ", faltas);
    }

    private Node celulaAcoes(Funcionario f) {
        Button editar = Botoes.icone("editar", "Editar cadastro de " + f.getNome(), false);
        editar.setOnAction(e -> editarFuncionario(f));
        Button excluir = Botoes.icone("lixeira", "Mover para a lixeira (Delete)", true);
        excluir.setOnAction(e -> pedirExclusao(f));
        return Celulas.acoes(editar, excluir);
    }

    // CPF na listagem: só os extremos aparecem (123.***.***-00). O número completo fica no cadastro.
    private String mascararCpf(String cpf) {
        if (vazio(cpf)) {
            return null;
        }
        String digitos = Mascaras.somenteDigitos(cpf);
        if (digitos.length() != 11) {
            return "***.***.***-**";
        }
        return digitos.substring(0, 3) + ".***.***-" + digitos.substring(9);
    }

    // ===================== Filtros e contagens =====================

    private Predicate<Funcionario> filtroBusca() {
        String termo = txtFiltroFuncionarios.getText() == null ? "" : txtFiltroFuncionarios.getText().trim().toLowerCase(Locale.ROOT);
        // Quem digita só números (ou formato de telefone/CPF) acha o telefone e o CPF pelos dígitos, com ou sem
        // pontuação. O código (FUN0001) é achado pelo texto, inteiro ou só pelo número.
        String digitos = termo.matches("[\\d\\s().+-]+") ? Mascaras.somenteDigitos(termo) : "";
        return f -> termo.isEmpty()
                || contem(f.getNome(), termo) || contem(f.getLogin(), termo) || contem(f.getCargo(), termo)
                || contem(f.getTelefone(), termo) || contem(f.getCodigo(), termo)
                || (!digitos.isEmpty() && Mascaras.somenteDigitos(f.getTelefone()).contains(digitos))
                || (!digitos.isEmpty() && Mascaras.somenteDigitos(f.getCpf()).contains(digitos));
    }

    private Predicate<Funcionario> filtroCargo() {
        String escolhido = cbCargoFiltro.getValue();
        if (escolhido == null || TODOS_OS_CARGOS.equals(escolhido)) {
            return f -> true;
        }
        if (SEM_CARGO.equals(escolhido)) {
            return f -> vazio(f.getCargo());
        }
        return f -> f.getCargo() != null && f.getCargo().trim().equalsIgnoreCase(escolhido.trim());
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String minusculo(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT);
    }

    private boolean filtroDeCargoAtivo() {
        String escolhido = cbCargoFiltro.getValue();
        return escolhido != null && !TODOS_OS_CARGOS.equals(escolhido);
    }

    // Monta o filtro de cargo a partir dos cargos que existem hoje. O cargo escolhido permanece na lista mesmo que
    // ninguém o tenha mais (assim o filtro não "pula" sozinho para Todos e o estado vazio explica o que houve).
    private void atualizarCargos() {
        String atual = cbCargoFiltro.getValue();
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        ordem.setStrength(Collator.SECONDARY);
        TreeMap<String, String> cargos = new TreeMap<>(ordem);
        boolean haSemCargo = false;
        for (Funcionario f : repositorio.getItens()) {
            if (vazio(f.getCargo())) {
                haSemCargo = true;
            } else {
                cargos.putIfAbsent(f.getCargo().trim(), f.getCargo().trim());
            }
        }
        boolean atualEspecial = atual == null || TODOS_OS_CARGOS.equals(atual) || SEM_CARGO.equals(atual);
        if (!atualEspecial) {
            cargos.putIfAbsent(atual.trim(), atual.trim());
        }
        List<String> opcoes = new ArrayList<>();
        opcoes.add(TODOS_OS_CARGOS);
        opcoes.addAll(cargos.values());
        if (haSemCargo || SEM_CARGO.equals(atual)) {
            opcoes.add(SEM_CARGO);
        }
        String selecionar = atual == null ? TODOS_OS_CARGOS : atual;
        if (opcoes.equals(cbCargoFiltro.getItems()) && selecionar.equals(atual)) {
            return;
        }
        atualizandoCargos = true;
        try {
            cbCargoFiltro.getItems().setAll(opcoes);
            cbCargoFiltro.setValue(selecionar);
        } finally {
            atualizandoCargos = false;
        }
    }

    private void atualizarTudo() {
        if (funcionariosFiltrados == null) {
            return;
        }
        Predicate<Funcionario> busca = filtroBusca();
        Predicate<Funcionario> cargo = filtroCargo();
        funcionariosFiltrados.setPredicate(f -> busca.test(f) && cargo.test(f));

        int total = repositorio.getItens().size();
        long incompletos = repositorio.getItens().stream().filter(f -> !camposFaltando(f).isEmpty()).count();
        lblResumoLista.setText(total + (total == 1 ? " funcionário" : " funcionários")
                + (incompletos > 0 ? " · " + incompletos + " com cadastro incompleto" : ""));
        int exibidos = funcionariosFiltrados.size();
        lblRodapeContagem.setText(total == 0 ? "Nenhum funcionário cadastrado"
                : "Mostrando " + exibidos + " de " + total + (total == 1 ? " funcionário" : " funcionários"));
        tableFuncionarios.setPlaceholder(estadoVazio(total == 0));
    }

    private Node estadoVazio(boolean semNenhum) {
        if (semNenhum) {
            Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar primeiro funcionário", null);
            novo.setOnAction(e -> handleNovoFuncionario());
            return Celulas.estadoVazio("funcionarios", "Nenhum funcionário cadastrado ainda",
                    "Cadastre quem trabalha na farmácia. Cada funcionário precisa de um login único para ser identificado no sistema.",
                    novo);
        }
        String termo = txtFiltroFuncionarios.getText() == null ? "" : txtFiltroFuncionarios.getText().trim();
        boolean comCargo = filtroDeCargoAtivo();
        String rotuloLimpar = termo.isEmpty() ? "Limpar filtro" : comCargo ? "Limpar busca e filtro" : "Limpar busca";
        Button limpar = Botoes.botao("v2-btn-ghost", null, rotuloLimpar, null);
        limpar.setOnAction(e -> {
            txtFiltroFuncionarios.clear();
            cbCargoFiltro.setValue(TODOS_OS_CARGOS);
        });
        Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar novo funcionário", null);
        novo.setOnAction(e -> handleNovoFuncionario());
        return Celulas.estadoVazio("buscar",
                termo.isEmpty() ? "Nenhum funcionário neste cargo" : "Nenhum funcionário para “" + termo + "”",
                "Verifique a grafia, busque pelo código, CPF, login ou telefone, ou escolha outro cargo.",
                limpar, novo);
    }

    // ===================== Ações =====================

    @FXML
    private void handleNovoFuncionario() {
        ContextoFuncionario.iniciarNovo();
        abrirFormulario("Novo Funcionário");
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroFuncionarios.clear();
        txtFiltroFuncionarios.requestFocus();
    }

    @FXML
    private void handleDispensarDica() {
        dicaDispensada = true;
        dicaBox.setVisible(false);
        dicaBox.setManaged(false);
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Funcionários",
                List.of("O login identifica cada funcionário no sistema e não pode se repetir; maiúsculas e minúsculas contam como iguais.",
                        "O CPF aparece parcialmente na lista (123.***.***-00). O número completo fica só no cadastro.",
                        "O ícone de alerta ao lado do nome indica cadastro incompleto (falta CPF ou telefone); passe o mouse para ver o que falta.",
                        "Use o filtro de cargo junto com a busca para achar rapidamente quem você procura.",
                        "Funcionários excluídos vão para a Lixeira; logo depois de excluir, use Desfazer para voltar atrás."),
                List.of(new Caixas.Atalho("Ctrl N", "Novo funcionário"),
                        new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Abrir o cadastro selecionado"),
                        new Caixas.Atalho("Delete", "Mover o funcionário selecionado para a lixeira"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    private void editarFuncionario(Funcionario funcionario) {
        if (funcionario == null) {
            return;
        }
        ContextoFuncionario.iniciarEdicao(funcionario);
        abrirFormulario("Editar Funcionário");
    }

    private void abrirFormulario(String titulo) {
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/FuncionarioFormView.fxml", "Farmácia Homeopática — " + titulo);
    }

    // ===================== Exclusão reversível =====================

    private void pedirExclusao(Funcionario funcionario) {
        if (funcionario == null) {
            return;
        }
        List<String> partes = new ArrayList<>();
        partes.add("login " + funcionario.getLogin());
        if (!vazio(funcionario.getCargo())) {
            partes.add(funcionario.getCargo());
        }
        Caixas.confirmar(rootStack, "lixeira", true,
                "Mover " + funcionario.getNome() + " para a lixeira?",
                "O cadastro sai da lista, mas fica guardado na Lixeira. Você pode desfazer logo em seguida ou restaurá-lo depois.",
                funcionario.getNome() + " · " + String.join(" · ", partes),
                "Mover para a lixeira", () -> moverParaLixeira(funcionario));
    }

    private void moverParaLixeira(Funcionario funcionario) {
        if (!repositorio.getItens().contains(funcionario)) {
            return;
        }
        repositorio.moverParaLixeira(funcionario);
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, funcionario.getNome() + " movido para a lixeira.", "Desfazer",
                () -> restaurar(funcionario));
    }

    private void restaurar(Funcionario funcionario) {
        repositorio.restaurar(funcionario);
        tableFuncionarios.getSelectionModel().select(funcionario);
        tableFuncionarios.scrollTo(funcionario);
        Toast.mostrar(rootStack, Toast.Tipo.INFO, funcionario.getNome() + " restaurado.");
    }

    private void exibirAvisoPendente() {
        ContextoFuncionario.Aviso aviso = ContextoFuncionario.consumirAviso();
        if (aviso == null) {
            return;
        }
        Toast.mostrar(rootStack, aviso.tipo() == ContextoFuncionario.TipoAviso.SUCESSO ? Toast.Tipo.SUCESSO : Toast.Tipo.INFO, aviso.texto());
        Funcionario destaque = aviso.destaque();
        if (destaque != null) {
            if (!funcionariosFiltrados.contains(destaque)) {
                txtFiltroFuncionarios.clear();
                cbCargoFiltro.setValue(TODOS_OS_CARGOS);
            }
            tableFuncionarios.getSelectionModel().select(destaque);
            tableFuncionarios.scrollTo(destaque);
        }
    }
}
