package com.farmacia.controller;

import com.farmacia.model.Cliente;
import com.farmacia.model.ItemReceita;
import com.farmacia.model.Receita;
import com.farmacia.model.StatusReceita;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.EstadoLista;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Sobreposicao;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoCliente;
import com.farmacia.util.ContextoReceita;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

// Controller da lista de Receitas (tema v2). Criar, editar e duplicar abrem a tela ReceitaFormView.
public class ReceitaController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String TODOS_MEDICOS = "Todos os médicos";
    private static final String CHAVE_ESTADO = "receitas";

    private enum Periodo {
        ULTIMOS_30("Últimos 30 dias"), ULTIMOS_90("Últimos 90 dias"), ESTE_ANO("Este ano"), TODO("Todo o período");

        private final String rotulo;

        Periodo(String rotulo) {
            this.rotulo = rotulo;
        }

        boolean contem(LocalDate data) {
            if (this == TODO) {
                return true;
            }
            if (data == null) {
                return false;
            }
            LocalDate hoje = LocalDate.now();
            return switch (this) {
                case ULTIMOS_30 -> !data.isBefore(hoje.minusDays(30));
                case ULTIMOS_90 -> !data.isBefore(hoje.minusDays(90));
                case ESTE_ANO -> data.getYear() == hoje.getYear();
                default -> true;
            };
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    // A dica do cabeçalho some depois que o usuário diz que entendeu, até o programa ser reaberto.
    private static boolean dicaDispensada = false;

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;

    @FXML
    private Label lnkInicio;
    @FXML
    private Button btnAjuda;
    @FXML
    private HBox dicaBox;
    @FXML
    private StackPane dicaIcone;

    @FXML
    private Label lblResumoLista;
    @FXML
    private TextField txtFiltroReceitas;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnNovaReceita;
    @FXML
    private ToggleGroup grupoStatus;
    @FXML
    private ToggleButton tgTodas;
    @FXML
    private ToggleButton tgManipulacao;
    @FXML
    private ToggleButton tgProntas;
    @FXML
    private ToggleButton tgEntregues;
    @FXML
    private ToggleButton tgCanceladas;
    @FXML
    private ComboBox<Periodo> cbPeriodo;
    @FXML
    private ComboBox<String> cbMedico;

    @FXML
    private TableView<Receita> tableReceitas;
    @FXML
    private TableColumn<Receita, Receita> colCodigo;
    @FXML
    private TableColumn<Receita, Receita> colData;
    @FXML
    private TableColumn<Receita, Receita> colPaciente;
    @FXML
    private TableColumn<Receita, Receita> colMedico;
    @FXML
    private TableColumn<Receita, Receita> colMedicamentos;
    @FXML
    private TableColumn<Receita, Receita> colStatus;
    @FXML
    private TableColumn<Receita, Receita> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final ReceitaRepository repositorio = ReceitaRepository.getInstance();
    private FilteredList<Receita> receitasFiltradas;
    private final Map<StatusReceita, ToggleButton> botoesStatus = new EnumMap<>(StatusReceita.class);
    private final Map<ToggleButton, Label> contadores = new java.util.HashMap<>();
    private boolean atualizandoMedicos = false;

    @FXML
    private void initialize() {
        com.farmacia.ui.Seletores.comIcone(cbPeriodo, "calendario");
        com.farmacia.ui.Seletores.comIcone(cbMedico, "medicos");
        menuLateralController.marcarAtivo(MenuLateralController.RECEITAS);

        montarCabecalho();
        configurarFiltros();
        configurarTabela();
        repositorio.getItens().addListener((ListChangeListener<Receita>) c -> atualizarTudo());
        rootStack.addEventFilter(KeyEvent.KEY_PRESSED, this::tratarAtalhos);

        // Voltando de um cadastro, a lista continua com a busca, os filtros, a ordem e a linha de antes.
        EstadoLista.Estado anterior = EstadoLista.retomar(CHAVE_ESTADO);
        if (anterior != null) {
            atualizarMedicos();
            if (anterior.extra("periodo") instanceof Periodo periodo) {
                cbPeriodo.setValue(periodo);
            }
            if (anterior.extra("medico") instanceof String medico && cbMedico.getItems().contains(medico)) {
                cbMedico.setValue(medico);
            }
            EstadoLista.aplicar(anterior, txtFiltroReceitas, grupoStatus, tableReceitas);
        }
        atualizarTudo();
        exibirAvisoPendente();
    }

    private void guardarEstado() {
        EstadoLista.guardar(CHAVE_ESTADO, EstadoLista.capturar(txtFiltroReceitas, grupoStatus, tableReceitas)
                .extra("periodo", cbPeriodo.getValue())
                .extra("medico", cbMedico.getValue()));
    }

    // ===================== Cabeçalho, busca e botões compostos =====================

    private void montarCabecalho() {
        lnkInicio.setOnMouseClicked(e -> tableReceitas.requestFocus());
        btnAjuda.setGraphic(botaoComposto("ajuda", "Ajuda", "F1"));
        btnNovaReceita.setGraphic(botaoComposto("mais", "Nova receita", "Ctrl N"));
        dicaIcone.getChildren().add(Icones.criar("info", 18));
        dicaBox.setVisible(!dicaDispensada);
        dicaBox.setManaged(!dicaDispensada);

        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");
        // Com texto digitado, o botão X substitui a dica de teclado.
        txtFiltroReceitas.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            atualizarTudo();
        });
    }

    // Botão com ícone, rótulo e dica de teclado ao lado.
    private Node botaoComposto(String icone, String texto, String teclas) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("v2-btn-rotulo");
        HBox caixa = new HBox(8, Icones.criar(icone, 18), rotulo);
        if (teclas != null) {
            Label kbd = new Label(teclas);
            kbd.getStyleClass().add("v2-kbd");
            caixa.getChildren().add(kbd);
        }
        caixa.setAlignment(Pos.CENTER);
        return caixa;
    }

    @FXML
    private void handleDispensarDica() {
        dicaDispensada = true;
        dicaBox.setVisible(false);
        dicaBox.setManaged(false);
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroReceitas.clear();
        txtFiltroReceitas.requestFocus();
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Receitas",
                List.of("Use a busca para encontrar uma receita pelo paciente, responsável, médico, código ou medicamento.",
                        "Os filtros de status, período e médico se combinam com a busca.",
                        "Clique no status de uma receita para alterá-lo (por exemplo, de Em manipulação para Pronta).",
                        "Duplicar cria um novo pedido a partir de uma receita anterior; a cópia só é salva quando você confirma.",
                        "Receitas excluídas vão para a Lixeira e podem ser restauradas."),
                List.of(new Caixas.Atalho("Ctrl N", "Nova receita"),
                        new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Abrir a receita selecionada"),
                        new Caixas.Atalho("Ctrl D", "Duplicar a receita selecionada"),
                        new Caixas.Atalho("Delete", "Mover a receita selecionada para a lixeira"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    // ===================== Filtros =====================

    private void configurarFiltros() {
        botoesStatus.put(StatusReceita.EM_MANIPULACAO, tgManipulacao);
        botoesStatus.put(StatusReceita.PRONTA, tgProntas);
        botoesStatus.put(StatusReceita.ENTREGUE, tgEntregues);
        botoesStatus.put(StatusReceita.CANCELADA, tgCanceladas);
        for (ToggleButton botao : List.of(tgTodas, tgManipulacao, tgProntas, tgEntregues, tgCanceladas)) {
            Label contador = new Label("0");
            contador.getStyleClass().add("v2-cnt");
            botao.setGraphic(contador);
            botao.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            contadores.put(botao, contador);
        }
        grupoStatus.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
                return;
            }
            atualizarTudo();
        });

        cbPeriodo.getItems().setAll(Periodo.values());
        cbPeriodo.setValue(Periodo.TODO);
        cbPeriodo.valueProperty().addListener((obs, antigo, novo) -> atualizarTudo());
        cbMedico.valueProperty().addListener((obs, antigo, novo) -> {
            if (!atualizandoMedicos) {
                atualizarTudo();
            }
        });
        receitasFiltradas = new FilteredList<>(repositorio.getItens(), r -> true);
    }

    // Todos os filtros menos o status (usados para as contagens dos botões de status).
    private Predicate<Receita> filtroBase() {
        String termo = txtFiltroReceitas.getText() == null ? "" : txtFiltroReceitas.getText().trim().toLowerCase(Locale.ROOT);
        Periodo periodo = cbPeriodo.getValue() == null ? Periodo.TODO : cbPeriodo.getValue();
        String medico = cbMedico.getValue();
        boolean todosMedicos = medico == null || TODOS_MEDICOS.equals(medico);
        return r -> periodo.contem(r.getData())
                && (todosMedicos || medico.equals(r.getNomeMedico()))
                && (termo.isEmpty()
                || contem(r.getCodigo(), termo)
                || contem(r.getNomePaciente(), termo)
                || contem(r.getNomeResponsavel(), termo)
                || contem(r.getNomeMedico(), termo)
                || r.textoDoReceituario().contains(termo));
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private StatusReceita statusSelecionado() {
        for (Map.Entry<StatusReceita, ToggleButton> e : botoesStatus.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }

    // Recalcula lista, contagens, lista de médicos, resumo e o estado vazio.
    private void atualizarTudo() {
        if (receitasFiltradas == null) {
            return;
        }
        atualizarMedicos();
        Predicate<Receita> base = filtroBase();
        StatusReceita status = statusSelecionado();
        receitasFiltradas.setPredicate(r -> base.test(r) && (status == null || r.getStatus() == status));

        List<Receita> pelaBase = repositorio.getItens().stream().filter(base).toList();
        ((Label) tgTodas.getGraphic()).setText(String.valueOf(pelaBase.size()));
        botoesStatus.forEach((st, botao) -> ((Label) botao.getGraphic()).setText(
                String.valueOf(pelaBase.stream().filter(r -> r.getStatus() == st).count())));

        int exibidas = receitasFiltradas.size();
        int total = repositorio.getItens().size();
        lblResumoLista.setText(exibidas == total
                ? total + (total == 1 ? " receita" : " receitas") + " · da mais recente para a mais antiga"
                : exibidas + " de " + total + " receitas · da mais recente para a mais antiga");
        lblRodapeContagem.setText(total == 0 ? "Nenhuma receita cadastrada"
                : "Mostrando " + exibidas + " de " + total + (total == 1 ? " receita" : " receitas"));
        tableReceitas.setPlaceholder(criarEstadoVazio(total == 0));
    }

    private void atualizarMedicos() {
        atualizandoMedicos = true;
        try {
            String atual = cbMedico.getValue();
            List<String> nomes = repositorio.getItens().stream()
                    .map(Receita::getNomeMedico)
                    .filter(n -> n != null && !n.isBlank())
                    .distinct().sorted().toList();
            cbMedico.getItems().setAll(TODOS_MEDICOS);
            cbMedico.getItems().addAll(nomes);
            cbMedico.setValue(atual != null && cbMedico.getItems().contains(atual) ? atual : TODOS_MEDICOS);
        } finally {
            atualizandoMedicos = false;
        }
    }

    // Estado vazio útil: explica o que aconteceu e oferece o próximo passo.
    private Node criarEstadoVazio(boolean semNenhumaReceita) {
        VBox caixa = new VBox(10);
        caixa.setAlignment(Pos.CENTER);
        caixa.setPadding(new Insets(40, 24, 40, 24));

        StackPane medalhao = new StackPane(Icones.criar(semNenhumaReceita ? "receitas" : "buscar", 26));
        medalhao.getStyleClass().add("v2-vazio-medalhao");

        Label titulo = new Label();
        titulo.getStyleClass().add("v2-vazio-titulo");
        Label texto = new Label();
        texto.getStyleClass().add("v2-apoio");
        texto.setWrapText(true);
        texto.setMaxWidth(440);
        texto.setAlignment(Pos.CENTER);
        texto.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox acoes = new HBox(10);
        acoes.setAlignment(Pos.CENTER);
        if (semNenhumaReceita) {
            titulo.setText("Nenhuma receita cadastrada ainda");
            texto.setText("Comece cadastrando a primeira receita. Depois, é possível duplicar um pedido anterior com um clique.");
        } else {
            String termo = txtFiltroReceitas.getText() == null ? "" : txtFiltroReceitas.getText().trim();
            titulo.setText(termo.isEmpty() ? "Nenhuma receita com esses filtros" : "Nenhuma receita para “" + termo + "”");
            texto.setText("Verifique a grafia, busque pelo código da receita ou ajuste os filtros de status, período e médico.");
            Button limpar = new Button("Limpar filtros");
            limpar.getStyleClass().addAll("v2-btn", "v2-btn-ghost");
            limpar.setOnAction(e -> limparFiltros());
            acoes.getChildren().add(limpar);
        }
        Button nova = new Button("Cadastrar nova receita");
        nova.getStyleClass().addAll("v2-btn", "v2-btn-primary");
        nova.setOnAction(e -> handleNovaReceita());
        acoes.getChildren().add(nova);

        caixa.getChildren().addAll(medalhao, titulo, texto, acoes);
        return caixa;
    }

    private void limparFiltros() {
        txtFiltroReceitas.clear();
        tgTodas.setSelected(true);
        cbPeriodo.setValue(Periodo.TODO);
        cbMedico.setValue(TODOS_MEDICOS);
    }

    // ===================== Tabela =====================

    private void configurarTabela() {
        configurarColuna(colCodigo, this::celulaCodigo, Comparator.comparing(Receita::getCodigo, Comparator.nullsFirst(String::compareTo)));
        configurarColuna(colData, this::celulaData, Comparator.comparing(Receita::getData, Comparator.nullsFirst(LocalDate::compareTo)));
        configurarColuna(colPaciente, this::celulaPaciente, Comparator.comparing(
                (Receita r) -> r.getNomePaciente() == null ? "" : r.getNomePaciente().toLowerCase(Locale.ROOT)));
        configurarColuna(colMedico, this::celulaMedico, Comparator.comparing(
                (Receita r) -> r.getNomeMedico() == null ? "" : r.getNomeMedico().toLowerCase(Locale.ROOT)));
        configurarColuna(colMedicamentos, this::celulaMedicamentos, null);
        configurarColuna(colStatus, this::celulaStatus, Comparator.comparing(Receita::getStatus));
        configurarColuna(colAcoes, this::celulaAcoes, null);
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        SortedList<Receita> ordenadas = new SortedList<>(receitasFiltradas);
        ordenadas.comparatorProperty().bind(tableReceitas.comparatorProperty());
        tableReceitas.setItems(ordenadas);
        tableReceitas.setSortPolicy(tabela -> true);
        // Ordem inicial: mais recente primeiro.
        colData.setSortType(TableColumn.SortType.DESCENDING);
        tableReceitas.getSortOrder().add(colData);

        tableReceitas.setRowFactory(tv -> {
            TableRow<Receita> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editarReceita(linha.getItem());
                }
            });
            return linha;
        });
    }

    private interface FabricaCelula {
        Node criar(Receita receita);
    }

    // Cada coluna recebe a receita inteira; a célula decide como desenhá-la.
    private void configurarColuna(TableColumn<Receita, Receita> coluna, FabricaCelula fabrica, Comparator<Receita> ordem) {
        coluna.setCellValueFactory(dados -> new ReadOnlyObjectWrapper<>(dados.getValue()));
        if (ordem != null) {
            coluna.setComparator(ordem);
        }
        coluna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Receita receita, boolean vazio) {
                super.updateItem(receita, vazio);
                if (vazio || receita == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(fabrica.criar(receita));
            }
        });
    }

    private Node celulaCodigo(Receita r) {
        Label l = new Label(r.getCodigo());
        l.getStyleClass().add("v2-num");
        return l;
    }

    private Node celulaData(Receita r) {
        return new Label(r.getData() == null ? "—" : FORMATO_DATA.format(r.getData()));
    }

    private Node celulaPaciente(Receita r) {
        Label nome = new Label(r.getNomePaciente());
        // Com cadastro, o nome tem cara de link (verde e sublinhado, como o "Início" da trilha) e abre a ficha.
        nome.getStyleClass().addAll("v2-link", "v2-trilha-link");
        VBox caixa = new VBox(2, nome);
        Cliente paciente = r.getPaciente();
        if (paciente != null) {
            nome.setTooltip(new Tooltip("Abrir o cadastro de " + paciente.getNome()));
            nome.setOnMouseClicked(e -> abrirCadastroDoPaciente(paciente));
        } else {
            nome.getStyleClass().removeAll("v2-link", "v2-trilha-link");
        }
        Integer idade = paciente == null ? null : Cliente.calcularIdade(paciente.getDataNascimento());
        if (idade != null) {
            Label l = new Label(idade + (idade == 1 ? " ano" : " anos"));
            l.getStyleClass().add("v2-sub");
            caixa.getChildren().add(l);
        }
        // O responsável fica numa linha própria, para o nome aparecer inteiro sem precisar passar o mouse.
        if (r.getNomeResponsavel() != null) {
            Label l = new Label("resp. " + r.getNomeResponsavel());
            l.getStyleClass().add("v2-sub");
            l.setTooltip(new Tooltip("Responsável: " + r.getNomeResponsavel()));
            caixa.getChildren().add(l);
        }
        return caixa;
    }

    private Node celulaMedico(Receita r) {
        VBox caixa = new VBox(2, new Label(r.getNomeMedico()));
        if (r.getCrm() != null) {
            Label crm = new Label(r.getCrm());
            crm.getStyleClass().add("v2-sub");
            caixa.getChildren().add(crm);
        }
        return caixa;
    }

    private Node celulaMedicamentos(Receita r) {
        FlowPane chips = new FlowPane(6, 6);
        for (ItemReceita item : r.getItens()) {
            Label chip = new Label(item.getNomeRemedio() + (item.getDinamizacao() == null ? "" : " " + item.getDinamizacao()));
            chip.getStyleClass().add("v2-chip");
            chips.getChildren().add(chip);
        }
        if (r.getItens().isEmpty()) {
            Label vazio = new Label("—");
            vazio.getStyleClass().add("v2-sub");
            chips.getChildren().add(vazio);
        }
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.setPrefWrapLength(210);
        return chips;
    }

    private Node celulaStatus(Receita r) {
        StatusReceita status = r.getStatus();
        Region ponto = new Region();
        ponto.getStyleClass().addAll("v2-ponto", "v2-ponto-" + sufixo(status));
        // A seta ▾ e a mãozinha indicam que o selo abre o menu para trocar o status.
        Label selo = new Label(status.getRotulo() + " ▾", ponto);
        selo.getStyleClass().addAll("v2-selo", "v2-selo-" + sufixo(status));
        selo.setCursor(Cursor.HAND);
        Tooltip dica = new Tooltip("Clique para alterar o status");
        dica.setShowDelay(Duration.millis(250));
        selo.setTooltip(dica);
        selo.setOnMouseClicked(e -> abrirMenuDeStatus(r, selo));
        selo.setAccessibleText("Status: " + status.getRotulo() + ". Clique para alterar.");
        return selo;
    }

    private String sufixo(StatusReceita status) {
        return switch (status) {
            case EM_MANIPULACAO -> "manip";
            case PRONTA -> "pronta";
            case ENTREGUE -> "entregue";
            case CANCELADA -> "cancelada";
        };
    }

    private Node celulaAcoes(Receita r) {
        Button editar = botaoIcone("editar", "Editar receita " + r.getCodigo(), false);
        editar.setOnAction(e -> editarReceita(r));
        Button duplicar = botaoIcone("duplicar", "Duplicar como novo pedido (Ctrl D)", false);
        duplicar.setOnAction(e -> duplicarReceita(r));
        Region separador = new Region();
        separador.getStyleClass().add("v2-vsep");
        Button excluir = botaoIcone("lixeira", "Mover para a lixeira (Delete)", true);
        excluir.setOnAction(e -> pedirExclusao(r));
        HBox caixa = new HBox(2, editar, duplicar, separador, excluir);
        HBox.setMargin(separador, new Insets(0, 6, 0, 6));
        caixa.setAlignment(Pos.CENTER_RIGHT);
        return caixa;
    }

    private Button botaoIcone(String icone, String dica, boolean perigo) {
        Button botao = new Button();
        botao.setGraphic(Icones.criar(icone, 18));
        botao.getStyleClass().add("v2-ib");
        if (perigo) {
            botao.getStyleClass().add("v2-ib-perigo");
        }
        Tooltip tooltip = new Tooltip(dica);
        tooltip.setShowDelay(Duration.millis(250));
        botao.setTooltip(tooltip);
        botao.setAccessibleText(dica);
        return botao;
    }

    // ===================== Status =====================

    private void abrirMenuDeStatus(Receita receita, Node ancora) {
        ContextMenu menu = new ContextMenu();
        for (StatusReceita opcao : StatusReceita.values()) {
            MenuItem item = new MenuItem((opcao == receita.getStatus() ? "✓  " : "     ") + opcao.getRotulo());
            item.setDisable(opcao == receita.getStatus());
            item.setOnAction(e -> alterarStatus(receita, opcao));
            menu.getItems().add(item);
        }
        menu.show(ancora, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void alterarStatus(Receita receita, StatusReceita novo) {
        StatusReceita anterior = receita.getStatus();
        receita.setStatus(novo);
        repositorio.atualizar(receita);
        tableReceitas.refresh();
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, "Receita " + receita.getCodigo() + ": " + novo.getRotulo() + ".",
                "Desfazer", () -> {
                    receita.setStatus(anterior);
                    repositorio.atualizar(receita);
                    tableReceitas.refresh();
                    Toast.mostrar(rootStack, Toast.Tipo.INFO, "Status da receita " + receita.getCodigo() + " voltou para "
                            + anterior.getRotulo() + ".");
                });
    }

    // ===================== Ações =====================

    @FXML
    private void handleNovaReceita() {
        ContextoReceita.iniciarNova();
        abrirFormulario("Nova Receita");
    }

    private void editarReceita(Receita receita) {
        if (receita == null) {
            return;
        }
        ContextoReceita.iniciarEdicao(receita);
        abrirFormulario("Editar Receita");
    }

    private void abrirCadastroDoPaciente(Cliente paciente) {
        if (!com.farmacia.repository.ClienteRepository.getInstance().getItens().contains(paciente)) {
            Toast.mostrar(rootStack, Toast.Tipo.INFO, paciente.getNome() + " está na Lixeira. Restaure o cadastro para abri-lo.");
            return;
        }
        ContextoCliente.iniciarEdicao(paciente);
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/ClienteFormView.fxml", "Farmácia Homeopática — Editar Cliente");
    }

    // Receita com um único medicamento segue direto; com vários, o usuário escolhe quais duplicar.
    private void duplicarReceita(Receita origem) {
        if (origem == null) {
            return;
        }
        if (origem.getItens().size() <= 1) {
            ContextoReceita.iniciarDuplicacao(origem, origem.getItens().stream().map(ItemReceita::copiar).toList());
            abrirFormulario("Duplicar Receita");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DuplicarReceitaView.fxml"));
            Parent conteudo = loader.load();
            DuplicarReceitaController controle = loader.getController();
            controle.configurar(origem);
            Caixas.formulario(rootStack, "duplicar", "Duplicar receita " + origem.getCodigo(), (Region) conteudo,
                    "Criar nova receita", controle.nenhumSelecionado(), () -> {
                        ContextoReceita.iniciarDuplicacao(origem, controle.itensSelecionados());
                        abrirFormulario("Duplicar Receita");
                    });
        } catch (IOException | RuntimeException e) {
            Toast.mostrar(rootStack, Toast.Tipo.INFO, "Não foi possível abrir a seleção de itens.");
        }
    }

    // Exclusão reversível: confirmação própria e, depois, aviso com "Desfazer".
    private void pedirExclusao(Receita receita) {
        if (receita == null) {
            return;
        }
        String resumo = receita.getNomePaciente() + " · " + (receita.getResumoItens().isBlank() ? "sem medicamentos" : receita.getResumoItens());
        Caixas.confirmar(rootStack, "lixeira", true,
                "Mover a receita " + receita.getCodigo() + " para a lixeira?",
                "Ela sai da lista, mas fica guardada na Lixeira. Você pode desfazer logo em seguida ou restaurá-la depois.",
                resumo, "Mover para a lixeira", () -> moverParaLixeira(receita));
    }

    private void moverParaLixeira(Receita receita) {
        repositorio.moverParaLixeira(receita);
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, "Receita " + receita.getCodigo() + " movida para a lixeira.",
                "Desfazer", () -> {
                    repositorio.restaurar(receita);
                    tableReceitas.getSelectionModel().select(receita);
                    Toast.mostrar(rootStack, Toast.Tipo.INFO, "Receita " + receita.getCodigo() + " restaurada.");
                });
    }

    private void abrirFormulario(String titulo) {
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — " + titulo);
    }

    // Mostra o resultado da tela anterior (ex.: "Receita salva") e destaca a receita afetada.
    private void exibirAvisoPendente() {
        ContextoReceita.Aviso aviso = ContextoReceita.consumirAviso();
        if (aviso == null) {
            return;
        }
        Toast.mostrar(rootStack,
                aviso.tipo() == ContextoReceita.TipoAviso.SUCESSO ? Toast.Tipo.SUCESSO : Toast.Tipo.INFO, aviso.texto());
        Receita destaque = aviso.destaque();
        if (destaque != null && repositorio.getItens().contains(destaque)) {
            if (!receitasFiltradas.contains(destaque)) {
                limparFiltros();
            }
            tableReceitas.getSelectionModel().select(destaque);
            tableReceitas.scrollTo(destaque);
        }
    }

    // ===================== Atalhos de teclado =====================

    private void tratarAtalhos(KeyEvent e) {
        if (Sobreposicao.temCaixaAberta(rootStack)) {
            return;
        }
        Node foco = rootStack.getScene() == null ? null : rootStack.getScene().getFocusOwner();
        boolean digitando = foco instanceof TextInputControl || (foco != null && foco.getParent() instanceof ComboBox<?> cb && cb.isEditable());
        boolean ctrl = e.isControlDown() || e.isMetaDown();
        Receita selecionada = tableReceitas.getSelectionModel().getSelectedItem();

        if (e.getCode() == KeyCode.F1) {
            e.consume();
            handleAjuda();
        } else if (ctrl && e.getCode() == KeyCode.N) {
            e.consume();
            handleNovaReceita();
        } else if (ctrl && e.getCode() == KeyCode.F) {
            e.consume();
            focarBusca();
        } else if (ctrl && e.getCode() == KeyCode.D && selecionada != null) {
            e.consume();
            duplicarReceita(selecionada);
        } else if (!ctrl && !digitando && e.getCode() == KeyCode.SLASH) {
            e.consume();
            focarBusca();
        } else if (e.getCode() == KeyCode.ESCAPE && foco == txtFiltroReceitas && !txtFiltroReceitas.getText().isEmpty()) {
            e.consume();
            txtFiltroReceitas.clear();
        } else if (foco == tableReceitas && selecionada != null) {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                editarReceita(selecionada);
            } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                e.consume();
                pedirExclusao(selecionada);
            }
        }
    }

    private void focarBusca() {
        txtFiltroReceitas.requestFocus();
        txtFiltroReceitas.selectAll();
    }
}
