package com.farmacia.controller;

import com.farmacia.model.Cliente;
import com.farmacia.model.ItemReceita;
import com.farmacia.model.Medico;
import com.farmacia.model.Receita;
import com.farmacia.model.StatusReceita;
import com.farmacia.repository.ClienteRepository;
import com.farmacia.repository.MedicoRepository;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.Alteracoes;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.ErroInline;
import com.farmacia.ui.Guarda;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoReceita;
import com.farmacia.util.Datas;
import com.farmacia.util.ListaSugestoes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Controller do cadastro de receita (tema v2), nos modos NOVA (opcionalmente já com paciente), EDITAR e DUPLICAR.
public class ReceitaFormController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;
    @FXML
    private Label lnkLista;
    @FXML
    private Label lblTrilhaAtual;
    @FXML
    private Label lblTituloPagina;
    @FXML
    private Label lblSubtituloPagina;
    @FXML
    private HBox dicaDuplicacao;
    @FXML
    private Label lblDuplicacao;

    @FXML
    private TextField txtCodigo;
    @FXML
    private DatePicker dpData;
    @FXML
    private TextField txtPaciente;
    @FXML
    private ListView<Cliente> listSugestoesPaciente;
    @FXML
    private TextField txtRgPaciente;
    @FXML
    private TextField txtResponsavel;
    @FXML
    private TextField txtMedico;
    @FXML
    private ListView<Medico> listSugestoesMedico;
    @FXML
    private TextField txtCrm;
    @FXML
    private Button btnCadastrarPaciente;
    @FXML
    private Button btnCadastrarMedico;
    @FXML
    private ComboBox<StatusReceita> cbStatus;

    @FXML
    private TextField txtItemRemedio;
    @FXML
    private ComboBox<String> cbItemTipo;
    @FXML
    private TextField txtItemPeso;
    @FXML
    private ComboBox<String> cbItemDinamizacao;
    @FXML
    private Spinner<Integer> spItemQuantidade;
    @FXML
    private TextField txtItemPosologia;
    @FXML
    private DatePicker dpItemFabricacao;
    @FXML
    private DatePicker dpItemValidade;
    @FXML
    private TextField txtItemValor;
    @FXML
    private Button btnAdicionarItem;
    @FXML
    private TableView<ItemReceita> tableItensReceituario;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemRemedio;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemTipo;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemPeso;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemDinamizacao;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemQuantidade;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemPosologia;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemFabricacao;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemValidade;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemValor;
    @FXML
    private TableColumn<ItemReceita, ItemReceita> colItemAcoes;

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvarReceita;

    private final ReceitaRepository repositorio = ReceitaRepository.getInstance();
    private final ClienteRepository clienteRepositorio = ClienteRepository.getInstance();
    private final MedicoRepository medicoRepositorio = MedicoRepository.getInstance();

    private final ObservableList<ItemReceita> itensFormulario = FXCollections.observableArrayList();
    private final ErroInline.Grupo erros = new ErroInline.Grupo();
    private final ErroInline.Grupo errosItem = new ErroInline.Grupo();
    private Alteracoes alteracoes;

    private Receita receitaEmEdicao;
    // Cadastro do paciente escolhido na lista (null se o nome foi digitado à mão).
    private Cliente pacienteSelecionado;
    // Idem para o médico: a receita só é salva com paciente e médico que existam nos cadastros.
    private Medico medicoSelecionado;
    // O que a tela foi pedida para fazer; guardado junto com o rascunho ao desviar para cadastrar paciente ou médico.
    private ContextoReceita.Pedido pedidoAtual;
    // Evita que o preenchimento programático do formulário reabra a lista de sugestões.
    private boolean atualizandoFormularioProgramaticamente = false;

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.RECEITAS);

        btnCancelar.setGraphic(Botoes.composto(null, "Cancelar", "Esc"));
        btnSalvarReceita.setGraphic(Botoes.composto("ok", "Salvar receita", "Ctrl S"));
        btnAdicionarItem.setGraphic(Botoes.composto("mais", "Adicionar item", null));
        lnkLista.setOnMouseClicked(e -> handleCancelar());

        tableItensReceituario.setItems(itensFormulario);
        configurarColunasItem();
        configurarAutocompletePaciente();
        configurarAutocompleteMedico();
        cbStatus.getItems().setAll(StatusReceita.values());

        ContextoReceita.Pedido pedido = ContextoReceita.consumirPedido();
        ContextoReceita.Rascunho rascunho = ContextoReceita.consumirRascunho();
        if (rascunho != null) {
            restaurarRascunho(rascunho);
        } else {
            aplicarPedido(pedido);
        }

        alteracoes = new Alteracoes(dpData, txtPaciente, txtMedico, cbStatus, txtItemRemedio, txtItemPeso, txtItemPosologia,
                txtItemValor, dpItemFabricacao, dpItemValidade);
        itensFormulario.addListener((javafx.collections.ListChangeListener<ItemReceita>) c -> {
            alteracoes.sujoProperty().set(true);
            erros.limpar(tableItensReceituario);
        });
        alteracoes.zerar();
        // Um rascunho devolvido pelo cadastro continua sendo trabalho não salvo: sair ainda pergunta antes de descartar.
        if (rascunho != null) {
            alteracoes.sujoProperty().set(true);
        }
        Guarda.ativar(rootStack, alteracoes::isSujo);
        AtalhosLista.instalarFormulario(rootStack, this::handleSalvarReceita, this::handleCancelar);
    }

    // ===================== Modos =====================

    private void aplicarPedido(ContextoReceita.Pedido pedido) {
        pedidoAtual = pedido;
        switch (pedido.modo()) {
            case EDITAR -> prepararEdicao(pedido.receita());
            case DUPLICAR -> prepararDuplicacao(pedido.receita(), pedido.itens());
            default -> prepararNova(pedido.paciente());
        }
    }

    private void prepararNova(Cliente paciente) {
        receitaEmEdicao = null;
        definirCabecalho("Nova receita", "Nova receita", "Preencha os dados da receita e adicione os medicamentos do receituário.");
        preencher(repositorio.proximoCodigo(), null, "", "", "", "", "", List.of());
        pacienteSelecionado = null;
        medicoSelecionado = null;
        cbStatus.setValue(StatusReceita.EM_MANIPULACAO);
        if (paciente != null) {
            selecionarPaciente(paciente);
        }
        mostrarDicaDuplicacao(null);
    }

    private void prepararEdicao(Receita receita) {
        receitaEmEdicao = receita;
        definirCabecalho("Editar receita " + receita.getCodigo(), "Editar receita " + receita.getCodigo(), "Altere os dados necessários e salve.");
        preencher(receita.getCodigo(), receita.getData(),
                nz(receita.getNomePaciente()), nz(receita.getRgPaciente()), nz(receita.getNomeResponsavel()),
                nz(receita.getNomeMedico()), nz(receita.getCrm()), receita.getItens());
        pacienteSelecionado = receita.getPaciente();
        exibirDadosDoPaciente();
        medicoSelecionado = buscarMedicoPorNome(receita.getNomeMedico());
        cbStatus.setValue(receita.getStatus());
        mostrarDicaDuplicacao(null);
        atualizarAcoesDeCadastro();
    }

    // Rascunho: nova receita com paciente, médico e itens escolhidos; só é gravada quando o usuário confirma.
    private void prepararDuplicacao(Receita origem, List<ItemReceita> itens) {
        receitaEmEdicao = null;
        definirCabecalho("Duplicar receita", "Duplicar receita", "Rascunho criado a partir da receita " + origem.getCodigo() + ".");
        preencher(repositorio.proximoCodigo(), LocalDate.now(),
                nz(origem.getNomePaciente()), nz(origem.getRgPaciente()), nz(origem.getNomeResponsavel()),
                nz(origem.getNomeMedico()), nz(origem.getCrm()), itens);
        pacienteSelecionado = origem.getPaciente();
        exibirDadosDoPaciente();
        medicoSelecionado = buscarMedicoPorNome(origem.getNomeMedico());
        cbStatus.setValue(StatusReceita.EM_MANIPULACAO);
        atualizarAcoesDeCadastro();
        mostrarDicaDuplicacao("Esta cópia da receita " + origem.getCodigo() + " (" + itens.size() + (itens.size() == 1 ? " item" : " itens")
                + ") ainda não foi salva. Confira os dados; fabricação, validade e valor de cada item ficam em branco para você preencher.");
    }

    private void definirCabecalho(String titulo, String trilha, String subtitulo) {
        lblTituloPagina.setText(titulo);
        lblTrilhaAtual.setText(trilha);
        lblSubtituloPagina.setText(subtitulo);
    }

    private void mostrarDicaDuplicacao(String texto) {
        boolean mostrar = texto != null;
        dicaDuplicacao.setVisible(mostrar);
        dicaDuplicacao.setManaged(mostrar);
        if (mostrar) {
            lblDuplicacao.setText(texto);
        }
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }

    private void preencher(String codigo, LocalDate data, String paciente, String rg, String responsavel,
                           String medico, String crm, List<ItemReceita> itens) {
        atualizandoFormularioProgramaticamente = true;
        try {
            txtCodigo.setText(codigo);
            Datas.definir(dpData, data);
            txtPaciente.setText(paciente);
            txtRgPaciente.setText(rg);
            txtResponsavel.setText(responsavel);
            txtMedico.setText(medico);
            txtCrm.setText(crm);
            itensFormulario.setAll(itens);
            limparFormularioItem();
        } finally {
            atualizandoFormularioProgramaticamente = false;
        }
        ListaSugestoes.ocultar(listSugestoesPaciente);
        ListaSugestoes.ocultar(listSugestoesMedico);
    }

    // ===================== Tabela de itens =====================

    private void configurarColunasItem() {
        Celulas.coluna(colItemRemedio, i -> Celulas.texto(i.getNomeRemedio()), null);
        Celulas.coluna(colItemTipo, i -> Celulas.texto(i.getTipo()), null);
        Celulas.coluna(colItemPeso, i -> Celulas.texto(i.getPeso()), null);
        Celulas.coluna(colItemDinamizacao, i -> Celulas.texto(i.getDinamizacao()), null);
        Celulas.coluna(colItemQuantidade, i -> Celulas.texto(i.getQuantidade() == null ? null : String.valueOf(i.getQuantidade())), null);
        Celulas.coluna(colItemPosologia, i -> Celulas.texto(i.getPosologia()), null);
        Celulas.coluna(colItemFabricacao, i -> Celulas.texto(i.getDataFabricacao() == null ? null : FORMATO_DATA.format(i.getDataFabricacao())), null);
        Celulas.coluna(colItemValidade, i -> Celulas.texto(i.getDataValidade() == null ? null : FORMATO_DATA.format(i.getDataValidade())), null);
        Celulas.coluna(colItemValor, i -> Celulas.texto(i.getValor() == null ? null : FORMATO_MOEDA.format(i.getValor())), null);
        Celulas.coluna(colItemAcoes, i -> {
            Button remover = Botoes.icone("lixeira", "Remover este item", true);
            remover.setOnAction(e -> itensFormulario.remove(i));
            return Celulas.acoes(remover);
        }, null);
        colItemAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");
        tableItensReceituario.setPlaceholder(Celulas.estadoVazio("receitas", "Nenhum medicamento adicionado",
                "Preencha os campos acima e use Adicionar item para compor o receituário."));
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    // ===================== Autocomplete (Paciente / Médico) =====================

    private void configurarAutocompletePaciente() {
        txtPaciente.textProperty().addListener((obs, antigo, novoTexto) -> {
            if (atualizandoFormularioProgramaticamente) {
                return;
            }
            // Mudou o paciente digitando: o RG e o responsável anteriores não valem mais.
            txtRgPaciente.clear();
            txtResponsavel.clear();
            pacienteSelecionado = null;
            atualizarAcoesDeCadastro();
            String termo = novoTexto == null ? "" : novoTexto.trim();
            if (termo.isEmpty()) {
                ocultarLista(listSugestoesPaciente);
                return;
            }
            String termoMinusculo = termo.toLowerCase(Locale.ROOT);
            List<Cliente> resultados = clienteRepositorio.getItens().stream()
                    .filter(c -> contem(c.getNome(), termoMinusculo))
                    .collect(Collectors.toList());
            if (resultados.isEmpty()) {
                ocultarLista(listSugestoesPaciente);
            } else {
                listSugestoesPaciente.setItems(FXCollections.observableArrayList(resultados));
                ListaSugestoes.exibir(listSugestoesPaciente, txtPaciente);
            }
        });

        listSugestoesPaciente.setCellFactory(
                ListaSugestoes.fabricaCelulas(this::descreverPaciente));

        listSugestoesPaciente.setOnMouseClicked(e ->
                selecionarPaciente(listSugestoesPaciente.getSelectionModel().getSelectedItem()));
        listSugestoesPaciente.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                selecionarPaciente(listSugestoesPaciente.getSelectionModel().getSelectedItem());
            }
        });
        txtPaciente.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ocultarLista(listSugestoesPaciente);
            }
        });

        ListaSugestoes.ocultarAoPerderFoco(txtPaciente, listSugestoesPaciente);
    }

    // Dependentes aparecem com o responsável, o que também distingue homônimos.
    private String descreverPaciente(Cliente cliente) {
        if (cliente.isDependente()) {
            return cliente.getNome() + "  —  dependente de " + cliente.getNomeResponsavel();
        }
        return cliente.getNome() + "  —  " + descreverDocumento(cliente);
    }

    private String descreverDocumento(Cliente cliente) {
        if (cliente.getRg() != null) {
            return "RG " + cliente.getRg();
        }
        if (cliente.getCpf() != null) {
            return "CPF " + cliente.getCpf();
        }
        return "sem documento";
    }

    private void selecionarPaciente(Cliente paciente) {
        if (paciente == null) {
            return;
        }
        pacienteSelecionado = paciente;
        atualizandoFormularioProgramaticamente = true;
        try {
            txtPaciente.setText(paciente.getNome());
            // RG e responsável vêm do cadastro do paciente (campos só de leitura); sem RG no cadastro, fica em branco.
            txtRgPaciente.setText(nz(paciente.getRg()));
            txtResponsavel.setText(nz(paciente.getNomeResponsavel()));
        } finally {
            atualizandoFormularioProgramaticamente = false;
        }
        erros.limpar(txtPaciente);
        ocultarLista(listSugestoesPaciente);
        atualizarAcoesDeCadastro();
        txtPaciente.positionCaret(txtPaciente.getText().length());
    }

    // Na edição e na duplicação, RG e responsável também vêm do cadastro atual do paciente, como na escolha pela lista.
    private void exibirDadosDoPaciente() {
        if (pacienteSelecionado == null) {
            return;
        }
        atualizandoFormularioProgramaticamente = true;
        try {
            txtRgPaciente.setText(nz(pacienteSelecionado.getRg()));
            txtResponsavel.setText(nz(pacienteSelecionado.getNomeResponsavel()));
        } finally {
            atualizandoFormularioProgramaticamente = false;
        }
    }

    private void configurarAutocompleteMedico() {
        txtMedico.textProperty().addListener((obs, antigo, novoTexto) -> {
            if (atualizandoFormularioProgramaticamente) {
                return;
            }
            // Mudou o médico digitando: o CRM e o cadastro anteriores não valem mais.
            medicoSelecionado = null;
            txtCrm.clear();
            atualizarAcoesDeCadastro();
            String termo = novoTexto == null ? "" : novoTexto.trim();
            if (termo.isEmpty()) {
                ocultarLista(listSugestoesMedico);
                return;
            }
            String termoMinusculo = termo.toLowerCase(Locale.ROOT);
            List<Medico> resultados = medicoRepositorio.getItens().stream()
                    .filter(m -> contem(m.getNome(), termoMinusculo))
                    .collect(Collectors.toList());
            if (resultados.isEmpty()) {
                ocultarLista(listSugestoesMedico);
            } else {
                listSugestoesMedico.setItems(FXCollections.observableArrayList(resultados));
                ListaSugestoes.exibir(listSugestoesMedico, txtMedico);
            }
        });

        listSugestoesMedico.setCellFactory(
                ListaSugestoes.fabricaCelulas(medico -> medico.getNome() + "  —  " + medico.getCrm()));

        listSugestoesMedico.setOnMouseClicked(e ->
                selecionarMedico(listSugestoesMedico.getSelectionModel().getSelectedItem()));
        listSugestoesMedico.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                selecionarMedico(listSugestoesMedico.getSelectionModel().getSelectedItem());
            }
        });
        txtMedico.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ocultarLista(listSugestoesMedico);
            }
        });

        ListaSugestoes.ocultarAoPerderFoco(txtMedico, listSugestoesMedico);
    }

    private void selecionarMedico(Medico medico) {
        if (medico == null) {
            return;
        }
        medicoSelecionado = medico;
        atualizandoFormularioProgramaticamente = true;
        try {
            txtMedico.setText(medico.getNome());
            txtCrm.setText(medico.getCrm());
        } finally {
            atualizandoFormularioProgramaticamente = false;
        }
        erros.limpar(txtMedico);
        ocultarLista(listSugestoesMedico);
        atualizarAcoesDeCadastro();
        txtMedico.positionCaret(txtMedico.getText().length());
    }

    // ===================== Vínculo com os cadastros =====================

    private static String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private Medico buscarMedicoPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        List<Medico> iguais = medicoRepositorio.getItens().stream()
                .filter(m -> m.getNome() != null && m.getNome().equalsIgnoreCase(nome.trim())).toList();
        return iguais.size() == 1 ? iguais.get(0) : null;
    }

    // O cadastro vale se o nome ainda é o do escolhido; senão, só um médico com exatamente esse nome.
    private Medico resolverMedico(String nome) {
        if (medicoSelecionado != null && nome.equalsIgnoreCase(nz(medicoSelecionado.getNome()))) {
            return medicoSelecionado;
        }
        return buscarMedicoPorNome(nome);
    }

    // Ao editar, o paciente ou o médico já gravados na receita continuam valendo enquanto o nome não for trocado
    // (receitas antigas podem ter sido gravadas sem vínculo ou ter médico que depois saiu do cadastro).
    private boolean pacienteJaGravado(String nome) {
        return receitaEmEdicao != null && nome.equalsIgnoreCase(nz(receitaEmEdicao.getNomePaciente()));
    }

    private boolean medicoJaGravado(String nome) {
        return receitaEmEdicao != null && nome.equalsIgnoreCase(nz(receitaEmEdicao.getNomeMedico()));
    }

    // Quem digita um nome que não está nos cadastros recebe, logo abaixo do campo, o atalho para cadastrá-lo.
    private void atualizarAcoesDeCadastro() {
        String paciente = texto(txtPaciente);
        boolean pacienteSemCadastro = !paciente.isEmpty() && resolverPaciente(paciente) == null && !pacienteJaGravado(paciente);
        btnCadastrarPaciente.setVisible(pacienteSemCadastro);
        btnCadastrarPaciente.setManaged(pacienteSemCadastro);
        if (pacienteSemCadastro) {
            btnCadastrarPaciente.setText("Não encontrou? Cadastrar “" + paciente + "” como novo cliente");
        }
        String medico = texto(txtMedico);
        boolean medicoSemCadastro = !medico.isEmpty() && resolverMedico(medico) == null && !medicoJaGravado(medico);
        btnCadastrarMedico.setVisible(medicoSemCadastro);
        btnCadastrarMedico.setManaged(medicoSemCadastro);
        if (medicoSemCadastro) {
            btnCadastrarMedico.setText("Não encontrou? Cadastrar “" + medico + "” como novo médico");
        }
    }

    // Guarda o que já foi preenchido e abre o cadastro; ao salvar ou cancelar, a receita volta como estava.
    private void guardarRascunho() {
        Datas.confirmarTextoDigitado(dpData);
        ContextoReceita.guardarRascunho(new ContextoReceita.Rascunho(pedidoAtual, dpData.getValue(),
                txtPaciente.getText(), pacienteSelecionado, txtRgPaciente.getText(), txtResponsavel.getText(),
                txtMedico.getText(), medicoSelecionado, txtCrm.getText(), cbStatus.getValue(),
                List.copyOf(itensFormulario), dicaDuplicacao.isVisible() ? lblDuplicacao.getText() : null));
        Guarda.limpar();
    }

    @FXML
    private void handleCadastrarPaciente() {
        String nome = texto(txtPaciente);
        guardarRascunho();
        com.farmacia.util.ContextoCliente.iniciarNovoParaReceita(nome);
        MenuLateralController.navegar(rootStack, "/fxml/ClienteFormView.fxml", "Farmácia Homeopática — Novo Cliente");
    }

    @FXML
    private void handleCadastrarMedico() {
        String nome = texto(txtMedico);
        guardarRascunho();
        com.farmacia.util.ContextoMedico.iniciarNovoParaReceita(nome);
        MenuLateralController.navegar(rootStack, "/fxml/MedicoFormView.fxml", "Farmácia Homeopática — Novo Médico");
    }

    private void restaurarRascunho(ContextoReceita.Rascunho r) {
        aplicarPedido(r.pedido());
        preencher(txtCodigo.getText(), r.data(), nz(r.nomePaciente()), nz(r.rgPaciente()), nz(r.responsavel()),
                nz(r.nomeMedico()), nz(r.crm()), r.itens());
        pacienteSelecionado = r.paciente();
        medicoSelecionado = r.medico();
        cbStatus.setValue(r.status());
        mostrarDicaDuplicacao(r.dicaDuplicacao());

        // Os avisos do cadastro seriam mostrados na lista depois; aqui o próprio formulário confirma o que aconteceu.
        com.farmacia.util.ContextoCliente.consumirAviso();
        com.farmacia.util.ContextoMedico.consumirAviso();
        Object novo = ContextoReceita.consumirCadastroRecemCriado();
        if (novo instanceof Cliente cliente) {
            selecionarPaciente(cliente);
            Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, cliente.getNome() + " cadastrado e escolhido como paciente.");
        } else if (novo instanceof Medico medico) {
            selecionarMedico(medico);
            Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, medico.getNome() + " cadastrado e escolhido como médico.");
        }
        atualizarAcoesDeCadastro();
    }

    private void ocultarLista(ListView<?> lista) {
        ListaSugestoes.ocultar(lista);
    }

    // ===================== Itens do receituário =====================

    @FXML
    private void handleAdicionarItem() {
        errosItem.limpar();
        boolean fabricacaoValida = Datas.confirmarTextoDigitado(dpItemFabricacao);
        boolean validadeValida = Datas.confirmarTextoDigitado(dpItemValidade);

        String remedio = txtItemRemedio.getText();
        if (remedio == null || remedio.isBlank()) {
            errosItem.mostrar(txtItemRemedio, "Informe o medicamento.");
        }
        if (!fabricacaoValida) {
            errosItem.mostrar(dpItemFabricacao, "Data inválida: use dd/mm/aaaa.");
        }
        if (!validadeValida) {
            errosItem.mostrar(dpItemValidade, "Data inválida: use dd/mm/aaaa.");
        } else if (fabricacaoValida && dpItemFabricacao.getValue() != null && dpItemValidade.getValue() != null
                && dpItemValidade.getValue().isBefore(dpItemFabricacao.getValue())) {
            errosItem.mostrar(dpItemValidade, "A validade não pode ser anterior à fabricação.");
        }
        BigDecimal valorInformado = null;
        try {
            valorInformado = parseValor(txtItemValor.getText());
        } catch (NumberFormatException ex) {
            errosItem.mostrar(txtItemValor, "Valor inválido: use números, por exemplo 12,50.");
        }
        if (errosItem.temErros()) {
            errosItem.campos().get(0).requestFocus();
            return;
        }

        ItemReceita item = new ItemReceita();
        item.setNomeRemedio(remedio.trim());
        item.setTipo(valorOuNulo(cbItemTipo.getValue()));
        item.setPeso(valorOuNulo(txtItemPeso.getText()));
        item.setDinamizacao(valorOuNulo(cbItemDinamizacao.getValue()));
        item.setQuantidade(spItemQuantidade.getValue());
        item.setPosologia(valorOuNulo(txtItemPosologia.getText()));
        item.setDataFabricacao(dpItemFabricacao.getValue());
        item.setDataValidade(dpItemValidade.getValue());
        item.setValor(valorInformado);

        itensFormulario.add(item);
        limparFormularioItem();
        txtItemRemedio.requestFocus();
    }

    private void limparFormularioItem() {
        errosItem.limpar();
        txtItemRemedio.clear();
        cbItemTipo.setValue(null);
        txtItemPeso.clear();
        cbItemDinamizacao.setValue(null);
        spItemQuantidade.getValueFactory().setValue(1);
        txtItemPosologia.clear();
        Datas.definir(dpItemFabricacao, null);
        Datas.definir(dpItemValidade, null);
        txtItemValor.clear();
    }

    private String valorOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    // ===================== Ações =====================

    @FXML
    private void handleSalvarReceita() {
        erros.limpar();
        boolean dataValida = Datas.confirmarTextoDigitado(dpData);

        if (!dataValida) {
            erros.mostrar(dpData, "Data inválida: use o formato dd/mm/aaaa.");
        } else if (dpData.getValue() == null) {
            erros.mostrar(dpData, "Informe a data da prescrição.");
        }
        String paciente = texto(txtPaciente);
        if (paciente.isEmpty()) {
            erros.mostrar(txtPaciente, "Informe o paciente: digite o nome e escolha um cadastro da lista.");
        } else if (resolverPaciente(paciente) == null && !pacienteJaGravado(paciente)) {
            erros.mostrar(txtPaciente, "Escolha um cliente da lista de sugestões. Se a pessoa ainda não está cadastrada, use “Cadastrar” abaixo do campo.");
        }
        String medico = texto(txtMedico);
        if (medico.isEmpty()) {
            erros.mostrar(txtMedico, "Informe o médico: digite o nome e escolha um cadastro da lista.");
        } else if (resolverMedico(medico) == null && !medicoJaGravado(medico)) {
            erros.mostrar(txtMedico, "Escolha um médico da lista de sugestões. Se ele ainda não está cadastrado, use “Cadastrar” abaixo do campo.");
        }
        // O erro aparece logo abaixo do campo; o atalho de cadastro fica depois dele.
        botaoPorUltimo(btnCadastrarPaciente);
        botaoPorUltimo(btnCadastrarMedico);
        if (itensFormulario.isEmpty()) {
            erros.mostrar(tableItensReceituario, "Adicione ao menos um medicamento ao receituário.");
        }

        if (erros.temErros()) {
            Toast.mostrar(rootStack, Toast.Tipo.ERRO, "Corrija os campos destacados para salvar.");
            erros.campos().get(0).requestFocus();
            return;
        }

        Receita salva;
        String mensagem;
        if (receitaEmEdicao == null) {
            salva = new Receita();
            preencherReceita(salva);
            repositorio.adicionar(salva);
            mensagem = "Receita " + salva.getCodigo() + " salva com sucesso.";
        } else {
            salva = receitaEmEdicao;
            preencherReceita(salva);
            repositorio.atualizar(salva);
            mensagem = "Receita " + salva.getCodigo() + " atualizada.";
        }

        ContextoReceita.deixarAviso(ContextoReceita.TipoAviso.SUCESSO, mensagem, salva);
        alteracoes.zerar();
        irParaLista();
    }

    private void preencherReceita(Receita receita) {
        receita.setData(dpData.getValue());
        Cliente paciente = resolverPaciente(texto(txtPaciente));
        receita.setNomePaciente(paciente != null ? paciente.getNome() : texto(txtPaciente));
        // Sem cadastro correspondente só chega aqui uma receita antiga cujo paciente não foi trocado.
        receita.setPaciente(paciente != null ? paciente : receitaEmEdicao == null ? null : receitaEmEdicao.getPaciente());
        receita.setStatus(cbStatus.getValue() == null ? StatusReceita.EM_MANIPULACAO : cbStatus.getValue());
        // Com paciente cadastrado, RG e responsável saem do cadastro, assim como o CRM sai do cadastro do médico.
        receita.setRgPaciente(paciente != null ? valorOuNulo(nz(paciente.getRg())) : valorOuNulo(txtRgPaciente.getText()));
        receita.setNomeResponsavel(paciente != null ? valorOuNulo(nz(paciente.getNomeResponsavel())) : valorOuNulo(txtResponsavel.getText()));
        Medico medico = resolverMedico(texto(txtMedico));
        receita.setNomeMedico(medico != null ? medico.getNome() : texto(txtMedico));
        receita.setCrm(medico != null ? medico.getCrm() : valorOuNulo(txtCrm.getText()));
        receita.getItens().setAll(itensFormulario);
    }

    private void botaoPorUltimo(Button botao) {
        if (botao.getParent() instanceof javafx.scene.layout.VBox caixa) {
            caixa.getChildren().remove(botao);
            caixa.getChildren().add(botao);
        }
    }

    // O cadastro vale se o nome ainda é o do escolhido; senão, tenta um cliente com exatamente esse nome.
    private Cliente resolverPaciente(String nome) {
        if (pacienteSelecionado != null && nome.equalsIgnoreCase(pacienteSelecionado.getNome())) {
            return pacienteSelecionado;
        }
        List<Cliente> iguais = clienteRepositorio.getItens().stream()
                .filter(cl -> cl.getNome() != null && cl.getNome().equalsIgnoreCase(nome)).toList();
        return iguais.size() == 1 ? iguais.get(0) : null;
    }

    private BigDecimal parseValor(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        // Aceita formato brasileiro (vírgula como separador decimal).
        return new BigDecimal(texto.trim().replace(",", "."));
    }

    @FXML
    private void handleCancelar() {
        // Com alterações pendentes, o Guarda pergunta antes de descartar.
        Guarda.sair(this::irParaLista);
    }

    private void irParaLista() {
        Guarda.limpar();
        MenuLateralController.navegar(rootStack, "/fxml/ReceitaView.fxml", "Farmácia Homeopática — Gerenciamento de Receitas");
    }
}
