package com.farmacia.controller;

import com.farmacia.model.Medico;
import com.farmacia.model.Receita;
import com.farmacia.repository.MedicoRepository;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.EstadoLista;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoMedico;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// Controller da lista de Médicos (tema v2): busca, receitas por médico e exclusão reversível. Cadastro em MedicoFormView.
public class MedicoController {

    private static final String CHAVE_ESTADO = "medicos";

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
    private TextField txtFiltroMedicos;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnNovoMedico;
    @FXML
    private TableView<Medico> tableMedicos;
    @FXML
    private TableColumn<Medico, Medico> colCodigo;
    @FXML
    private TableColumn<Medico, Medico> colNome;
    @FXML
    private TableColumn<Medico, Medico> colCrm;
    @FXML
    private TableColumn<Medico, Medico> colTelefone;
    @FXML
    private TableColumn<Medico, Medico> colReceitas;
    @FXML
    private TableColumn<Medico, Medico> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final MedicoRepository repositorio = MedicoRepository.getInstance();
    private FilteredList<Medico> medicosFiltrados;

    // Escutas fracas: os repositórios são únicos e vivem mais que a tela, então não podem segurar o controller.
    private final ListChangeListener<Medico> aoMudarMedicos = c -> atualizarTudo();
    private final ListChangeListener<Receita> aoMudarReceitas = c -> {
        atualizarTudo();
        tableMedicos.refresh();
    };

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.MEDICOS);

        btnAjuda.setGraphic(Botoes.composto("ajuda", "Ajuda", "F1"));
        btnNovoMedico.setGraphic(Botoes.composto("mais", "Novo médico", "Ctrl N"));
        dicaIcone.getChildren().add(Icones.criar("info", 18));
        dicaBox.setVisible(!dicaDispensada);
        dicaBox.setManaged(!dicaDispensada);
        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");

        txtFiltroMedicos.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            atualizarTudo();
        });

        medicosFiltrados = new FilteredList<>(repositorio.getItens(), m -> true);
        configurarTabela();
        repositorio.getItens().addListener(new WeakListChangeListener<>(aoMudarMedicos));
        ReceitaRepository.getInstance().getItens().addListener(new WeakListChangeListener<>(aoMudarReceitas));
        AtalhosLista.instalar(rootStack, txtFiltroMedicos, tableMedicos, this::handleNovoMedico,
                this::editarMedico, this::pedirExclusao, this::handleAjuda);

        // Voltando de um cadastro, a lista continua com a busca, a ordem e a linha de antes.
        EstadoLista.Estado anterior = EstadoLista.retomar(CHAVE_ESTADO);
        if (anterior != null) {
            EstadoLista.aplicar(anterior, txtFiltroMedicos, null, tableMedicos);
        } else {
            ordenarPeloPadrao();
        }
        atualizarTudo();
        exibirAvisoPendente();
    }

    private void guardarEstado() {
        EstadoLista.guardar(CHAVE_ESTADO, EstadoLista.capturar(txtFiltroMedicos, null, tableMedicos));
    }

    // ===================== Tabela =====================

    private void configurarTabela() {
        Celulas.coluna(colCodigo, m -> Celulas.numero(m.getCodigo()), Comparator.comparing(Medico::getCodigo));
        Celulas.coluna(colNome, this::celulaNome, Comparator.comparing((Medico m) -> minusculas(m.getNome())));
        Celulas.coluna(colCrm, m -> Celulas.texto(m.getCrm()), Comparator.comparing((Medico m) -> minusculas(m.getCrm())));
        Celulas.coluna(colTelefone, m -> Celulas.texto(m.getTelefone()), Comparator
                .comparing((Medico m) -> m.getTelefone() == null || m.getTelefone().isBlank())
                .thenComparing(m -> minusculas(m.getTelefone())));
        Celulas.coluna(colReceitas, this::celulaReceitas, Comparator.comparingInt(repositorio::contarReceitas));
        Celulas.coluna(colAcoes, this::celulaAcoes, null);
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        SortedList<Medico> ordenados = new SortedList<>(medicosFiltrados);
        ordenados.comparatorProperty().bind(tableMedicos.comparatorProperty());
        tableMedicos.setItems(ordenados);

        tableMedicos.setRowFactory(tv -> {
            TableRow<Medico> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editarMedico(linha.getItem());
                }
            });
            return linha;
        });
    }

    // Ordem inicial: código crescente, com a seta no título da coluna (como em Receitas). Voltando de um cadastro,
    // vale a ordem guardada pelo EstadoLista, inclusive "sem ordenação" se o usuário a tirou.
    private void ordenarPeloPadrao() {
        colCodigo.setSortType(TableColumn.SortType.ASCENDING);
        tableMedicos.getSortOrder().add(colCodigo);
    }

    private static String minusculas(String texto) {
        return texto == null ? "" : texto.toLowerCase(Locale.ROOT);
    }

    private Node celulaNome(Medico m) {
        Label nome = new Label(m.getNome());
        nome.getStyleClass().add("v2-link-neutro");
        nome.setTooltip(new Tooltip(m.getNome()));
        return nome;
    }

    // "N receitas" quando há; "0 receitas" em tom discreto quando o médico ainda não aparece em nenhuma
    // (o traço fica reservado para dado não informado, como nas demais telas).
    private Node celulaReceitas(Medico m) {
        int n = repositorio.contarReceitas(m);
        if (n == 0) {
            Label zero = new Label("0 receitas");
            zero.getStyleClass().add("v2-vazio-texto");
            zero.setTooltip(new Tooltip(m.getNome() + " ainda não emitiu receitas cadastradas"));
            return zero;
        }
        Label l = new Label(n + (n == 1 ? " receita" : " receitas"));
        l.setTooltip(new Tooltip(n + (n == 1 ? " receita emitida por " : " receitas emitidas por ") + m.getNome()));
        return l;
    }

    private Node celulaAcoes(Medico m) {
        Button editar = Botoes.icone("editar", "Editar cadastro de " + m.getNome(), false);
        editar.setOnAction(e -> editarMedico(m));
        Button excluir = Botoes.icone("lixeira", "Mover para a lixeira (Delete)", true);
        excluir.setOnAction(e -> pedirExclusao(m));
        return Celulas.acoes(editar, excluir);
    }

    // ===================== Busca e contagens =====================

    private Predicate<Medico> filtroBusca() {
        String termo = txtFiltroMedicos.getText() == null ? "" : txtFiltroMedicos.getText().trim().toLowerCase(Locale.ROOT);
        // Termo só com dígitos e pontuação: compara também só os dígitos do telefone ("11 3456" acha "(11) 3456-7890").
        String digitos = termo.matches("[\\d\\s().+\\-]+") ? termo.replaceAll("\\D", "") : "";
        return m -> termo.isEmpty()
                || contem(m.getNome(), termo) || contem(m.getCrm(), termo)
                || contem(m.getTelefone(), termo) || contem(m.getCodigo(), termo)
                || (digitos.length() >= 3 && m.getTelefone() != null && m.getTelefone().replaceAll("\\D", "").contains(digitos));
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private void atualizarTudo() {
        if (medicosFiltrados == null) {
            return;
        }
        medicosFiltrados.setPredicate(filtroBusca());

        int total = repositorio.getItens().size();
        lblResumoLista.setText(total + (total == 1 ? " médico" : " médicos"));
        int exibidos = medicosFiltrados.size();
        lblRodapeContagem.setText(total == 0 ? "Nenhum médico cadastrado"
                : "Mostrando " + exibidos + " de " + total + (total == 1 ? " médico" : " médicos"));
        tableMedicos.setPlaceholder(estadoVazio(total == 0));
    }

    private Node estadoVazio(boolean semNenhum) {
        if (semNenhum) {
            Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar primeiro médico", null);
            novo.setOnAction(e -> handleNovoMedico());
            return Celulas.estadoVazio("medicos", "Nenhum médico cadastrado ainda",
                    "Cadastre os médicos que prescrevem para a farmácia. Depois eles aparecem como sugestão ao preencher uma receita.", novo);
        }
        String termo = txtFiltroMedicos.getText() == null ? "" : txtFiltroMedicos.getText().trim();
        Button limpar = Botoes.botao("v2-btn-ghost", null, "Limpar busca", null);
        limpar.setOnAction(e -> txtFiltroMedicos.clear());
        Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar novo médico", null);
        novo.setOnAction(e -> handleNovoMedico());
        return Celulas.estadoVazio("buscar", "Nenhum médico para “" + termo + "”",
                "Verifique a grafia ou busque pelo CRM, telefone ou código.", limpar, novo);
    }

    // ===================== Ações =====================

    @FXML
    private void handleNovoMedico() {
        ContextoMedico.iniciarNovo();
        abrirFormulario("Novo Médico");
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroMedicos.clear();
        txtFiltroMedicos.requestFocus();
    }

    @FXML
    private void handleDispensarDica() {
        dicaDispensada = true;
        dicaBox.setVisible(false);
        dicaBox.setManaged(false);
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Médicos",
                List.of("Os médicos cadastrados aparecem como sugestão ao preencher o médico de uma receita.",
                        "A coluna Receitas conta as receitas emitidas pelo médico. Clique no título de uma coluna para ordenar.",
                        "Excluir um médico não apaga as receitas dele: elas continuam no histórico.",
                        "Médicos excluídos vão para a Lixeira e podem ser restaurados."),
                List.of(new Caixas.Atalho("Ctrl N", "Novo médico"),
                        new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Abrir o cadastro selecionado"),
                        new Caixas.Atalho("Delete", "Mover o médico selecionado para a lixeira"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    private void editarMedico(Medico medico) {
        if (medico == null) {
            return;
        }
        ContextoMedico.iniciarEdicao(medico);
        abrirFormulario("Editar Médico");
    }

    private void abrirFormulario(String titulo) {
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/MedicoFormView.fxml", "Farmácia Homeopática — " + titulo);
    }

    // ===================== Exclusão reversível =====================

    private void pedirExclusao(Medico medico) {
        if (medico == null) {
            return;
        }
        int n = repositorio.contarReceitas(medico);
        String impacto = n == 0 ? "nenhuma receita associada"
                : n + (n == 1 ? " receita associada continua" : " receitas associadas continuam") + " no histórico";
        Caixas.confirmar(rootStack, "lixeira", true,
                "Mover " + medico.getNome() + " para a lixeira?",
                "O cadastro sai da lista e deixa de ser sugerido nas receitas novas, mas fica guardado na Lixeira. "
                        + "Você pode desfazer logo em seguida ou restaurá-lo depois.",
                medico.getNome() + " · " + impacto,
                "Mover para a lixeira", () -> moverParaLixeira(medico));
    }

    private void moverParaLixeira(Medico medico) {
        repositorio.moverParaLixeira(medico);
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, medico.getNome() + " movido para a lixeira.", "Desfazer", () -> {
            repositorio.restaurar(medico);
            tableMedicos.getSelectionModel().select(medico);
            Toast.mostrar(rootStack, Toast.Tipo.INFO, medico.getNome() + " restaurado.");
        });
    }

    private void exibirAvisoPendente() {
        ContextoMedico.Aviso aviso = ContextoMedico.consumirAviso();
        if (aviso == null) {
            return;
        }
        Toast.mostrar(rootStack, aviso.tipo() == ContextoMedico.TipoAviso.SUCESSO ? Toast.Tipo.SUCESSO : Toast.Tipo.INFO, aviso.texto());
        Medico destaque = aviso.destaque();
        if (destaque != null) {
            if (!medicosFiltrados.contains(destaque)) {
                txtFiltroMedicos.clear();
            }
            tableMedicos.getSelectionModel().select(destaque);
            tableMedicos.scrollTo(destaque);
        }
    }
}
