package com.farmacia.controller;

import com.farmacia.model.Cliente;
import com.farmacia.model.Receita;
import com.farmacia.model.StatusReceita;
import com.farmacia.repository.ClienteRepository;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.Alteracoes;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.ErroInline;
import com.farmacia.ui.Guarda;
import com.farmacia.ui.Mascaras;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoCliente;
import com.farmacia.util.ContextoReceita;
import com.farmacia.util.Datas;
import com.farmacia.util.ListaSugestoes;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Controller do cadastro de cliente (tema v2), nos modos NOVO, EDITAR e NOVO_DEPENDENTE.
public class ClienteFormController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
    private TextField txtCodigo;
    @FXML
    private TextField txtNome;
    @FXML
    private Label lblTelefoneObrigatorio;
    @FXML
    private Label lblTelefoneHerdado;
    @FXML
    private TextField txtTelefone;
    @FXML
    private DatePicker dpDataNascimento;
    @FXML
    private TextField txtIdade;
    @FXML
    private TextField txtCpf;
    @FXML
    private TextField txtRg;

    @FXML
    private TextField txtResponsavel;
    @FXML
    private ListView<Cliente> listSugestoesResponsavel;
    @FXML
    private Label lblParentescoObrigatorio;
    @FXML
    private ComboBox<String> cbParentesco;
    @FXML
    private TextField txtTelefoneResponsavel;

    @FXML
    private TextField txtCep;
    @FXML
    private TextField txtRua;
    @FXML
    private TextField txtNumero;
    @FXML
    private TextField txtComplemento;
    @FXML
    private TextField txtBairro;
    @FXML
    private TextField txtCidade;
    @FXML
    private ComboBox<String> cbEstado;

    @FXML
    private VBox cardDependentes;
    @FXML
    private Button btnAdicionarDependente;
    @FXML
    private TableView<Cliente> tableDependentes;
    @FXML
    private TableColumn<Cliente, Cliente> colDepNome;
    @FXML
    private TableColumn<Cliente, Cliente> colDepParentesco;
    @FXML
    private TableColumn<Cliente, Cliente> colDepIdade;
    @FXML
    private TableColumn<Cliente, Cliente> colDepAcoes;

    @FXML
    private VBox cardReceitas;
    @FXML
    private Button btnNovaReceita;
    @FXML
    private TableView<Receita> tableReceitas;
    @FXML
    private TableColumn<Receita, Receita> colRecCodigo;
    @FXML
    private TableColumn<Receita, Receita> colRecData;
    @FXML
    private TableColumn<Receita, Receita> colRecPaciente;
    @FXML
    private TableColumn<Receita, Receita> colRecMedicamentos;
    @FXML
    private TableColumn<Receita, Receita> colRecAcoes;

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvarCliente;

    private final ClienteRepository repositorio = ClienteRepository.getInstance();
    private final ReceitaRepository receitas = ReceitaRepository.getInstance();
    private final ErroInline.Grupo erros = new ErroInline.Grupo();
    private Alteracoes alteracoes;

    private Cliente clienteEmEdicao;
    private Cliente responsavelEscolhido;
    private boolean atualizandoProgramaticamente = false;

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.CLIENTES);

        btnCancelar.setGraphic(Botoes.composto(null, "Cancelar", "Esc"));
        btnSalvarCliente.setGraphic(Botoes.composto("ok", "Salvar cliente", "Ctrl S"));
        btnAdicionarDependente.setGraphic(Botoes.composto("mais", "Adicionar dependente", null));
        btnNovaReceita.setGraphic(Botoes.composto("mais", "Nova receita", null));
        lnkLista.setOnMouseClicked(e -> handleCancelar());

        com.farmacia.ui.Seletores.comTextoDeExemplo(cbParentesco);
        com.farmacia.ui.Seletores.comTextoDeExemplo(cbEstado);
        Mascaras.cpf(txtCpf);
        Mascaras.cep(txtCep);
        Mascaras.telefone(txtTelefone);
        dpDataNascimento.valueProperty().addListener((obs, antiga, nova) -> atualizarIdade(nova));
        txtTelefone.textProperty().addListener((obs, a, n) -> atualizarTelefoneHerdado());
        configurarAutocompleteResponsavel();
        configurarTabelas();

        aplicarPedido(ContextoCliente.consumirPedido());

        alteracoes = new Alteracoes(txtNome, txtTelefone, dpDataNascimento, txtCpf, txtRg, txtResponsavel, cbParentesco,
                txtCep, txtRua, txtNumero, txtComplemento, txtBairro, txtCidade, cbEstado);
        alteracoes.zerar();
        Guarda.ativar(rootStack, alteracoes::isSujo);
        AtalhosLista.instalarFormulario(rootStack, this::handleSalvarCliente, this::handleCancelar);
        // O foco só pega depois que a tela entra na cena.
        javafx.application.Platform.runLater(txtNome::requestFocus);
    }

    private void atualizarIdade(LocalDate nascimento) {
        Integer idade = Cliente.calcularIdade(nascimento);
        txtIdade.setText(idade == null ? "" : idade + (idade == 1 ? " ano" : " anos"));
    }

    // Com responsável: o telefone próprio deixa de ser obrigatório e o parentesco passa a ser.
    private void atualizarEstadoDoVinculo() {
        boolean vinculado = responsavelEscolhido != null;
        lblTelefoneObrigatorio.setVisible(!vinculado);
        // Sem o asterisco, a etiqueta "usando o do responsável" encosta no rótulo, sem um vão no lugar dele.
        lblTelefoneObrigatorio.setManaged(!vinculado);
        lblParentescoObrigatorio.setVisible(vinculado);
        txtTelefoneResponsavel.setText(vinculado && responsavelEscolhido.getTelefone() != null ? responsavelEscolhido.getTelefone() : "");
        atualizarTelefoneHerdado();
    }

    // Etiqueta que avisa que o número usado para contato é o do responsável (evita editar achando que é próprio).
    private void atualizarTelefoneHerdado() {
        boolean herdado = responsavelEscolhido != null && (txtTelefone.getText() == null || txtTelefone.getText().isBlank());
        lblTelefoneHerdado.setText("usando o do responsável");
        lblTelefoneHerdado.setVisible(herdado);
        lblTelefoneHerdado.setManaged(herdado);
    }

    // ===================== Modos =====================

    private void aplicarPedido(ContextoCliente.Pedido pedido) {
        switch (pedido.modo()) {
            case EDITAR -> prepararEdicao(pedido.cliente());
            case NOVO_DEPENDENTE -> prepararNovoDependente(pedido.cliente());
            default -> prepararNovo();
        }
        // Aberto pela receita: o nome digitado lá já vem preenchido e o fim do cadastro devolve ao receituário.
        String sugerido = ContextoCliente.consumirNomeSugerido();
        if (sugerido != null) {
            txtNome.setText(sugerido);
        }
        if (ContextoCliente.voltaParaReceita()) {
            lnkLista.setText("Nova receita");
            lblSubtituloPagina.setText("Cadastre o paciente. Ao salvar, você volta para a receita que estava preenchendo.");
        }
    }

    private void prepararNovo() {
        clienteEmEdicao = null;
        definirCabecalho("Novo cliente", "Novo cliente", "Preencha os dados do cliente. Só Nome e Telefone são obrigatórios; "
                + "para dependentes, Nome e Parentesco (vale o telefone do responsável).");
        preencher(repositorio.proximoCodigo(), new Cliente(), null);
        mostrarCartoesDeRelacionamento(false, false);
    }

    private void prepararNovoDependente(Cliente responsavel) {
        clienteEmEdicao = null;
        definirCabecalho("Novo dependente", "Novo dependente",
                "Dependente de " + responsavel.getNome() + ". Informe nome e parentesco; sem telefone próprio, vale o do responsável.");
        preencher(repositorio.proximoCodigo(), new Cliente(), responsavel);
        mostrarCartoesDeRelacionamento(false, false);
    }

    private void prepararEdicao(Cliente cliente) {
        clienteEmEdicao = cliente;
        definirCabecalho("Editar " + cliente.getNome(), "Editar cliente " + cliente.getCodigo(),
                cliente.isDependente() ? "Dependente de " + cliente.getNomeResponsavel() + " (" + cliente.getParentesco() + ")"
                        : "Altere os dados necessários e salve.");
        preencher(cliente.getCodigo(), cliente, cliente.getResponsavel());
        boolean titular = !cliente.isDependente();
        mostrarCartoesDeRelacionamento(titular, true);
        if (titular) {
            tableDependentes.setItems(FXCollections.observableArrayList(repositorio.dependentesDe(cliente)));
        }
        List<Cliente> familia = new ArrayList<>(repositorio.dependentesDe(cliente));
        familia.add(cliente);
        tableReceitas.setItems(FXCollections.observableArrayList(receitas.getItens().stream()
                .filter(r -> familia.contains(r.getPaciente())).toList()));
        tableReceitas.setPlaceholder(Celulas.estadoVazio("receitas", "Nenhuma receita ainda",
                "As receitas deste cliente e de seus dependentes aparecem aqui."));
        tableDependentes.setPlaceholder(Celulas.estadoVazio("clientes", "Nenhum dependente",
                "Use Adicionar dependente para cadastrar um."));
    }

    private void definirCabecalho(String titulo, String trilha, String subtitulo) {
        lblTituloPagina.setText(titulo);
        lblTrilhaAtual.setText(trilha);
        lblSubtituloPagina.setText(subtitulo);
    }

    private void mostrarCartoesDeRelacionamento(boolean dependentes, boolean receitasDoCliente) {
        cardDependentes.setVisible(dependentes);
        cardDependentes.setManaged(dependentes);
        cardReceitas.setVisible(receitasDoCliente);
        cardReceitas.setManaged(receitasDoCliente);
    }

    private void preencher(String codigo, Cliente base, Cliente responsavel) {
        atualizandoProgramaticamente = true;
        try {
            txtCodigo.setText(codigo);
            txtNome.setText(nz(base.getNome()));
            txtTelefone.setText(nz(base.getTelefone()));
            Datas.definir(dpDataNascimento, base.getDataNascimento());
            txtCpf.setText(nz(base.getCpf()));
            txtRg.setText(nz(base.getRg()));
            txtCep.setText(nz(base.getCep()));
            txtRua.setText(nz(base.getRua()));
            txtNumero.setText(nz(base.getNumero()));
            txtComplemento.setText(nz(base.getComplemento()));
            txtBairro.setText(nz(base.getBairro()));
            txtCidade.setText(nz(base.getCidade()));
            cbEstado.setValue(base.getEstado());
            definirParentesco(base.getParentesco());
            txtResponsavel.setText(responsavel == null ? "" : responsavel.getNome());
            responsavelEscolhido = responsavel;
            atualizarEstadoDoVinculo();
        } finally {
            atualizandoProgramaticamente = false;
        }
        ListaSugestoes.ocultar(listSugestoesResponsavel);
    }

    // Parentesco é uma lista fechada. Um valor salvo que não esteja nela (cadastro antigo) entra antes de "Outro",
    // para continuar aparecendo e podendo ser escolhido de novo.
    private void definirParentesco(String parentesco) {
        if (parentesco != null && !cbParentesco.getItems().contains(parentesco)) {
            int posOutro = cbParentesco.getItems().indexOf("Outro");
            cbParentesco.getItems().add(posOutro < 0 ? cbParentesco.getItems().size() : posOutro, parentesco);
        }
        cbParentesco.setValue(parentesco);
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }

    // ===================== Autocomplete do responsável =====================

    private void configurarAutocompleteResponsavel() {
        txtResponsavel.textProperty().addListener((obs, antigo, novoTexto) -> {
            if (atualizandoProgramaticamente) {
                return;
            }
            // Digitar de novo desfaz a escolha anterior: o vínculo vale para um item da lista ou para um nome
            // que coincida exatamente com um único titular, e os asteriscos acompanham isso enquanto se digita.
            responsavelEscolhido = null;
            responsavelEscolhido = resolverResponsavel();
            atualizarEstadoDoVinculo();
            String termo = novoTexto == null ? "" : novoTexto.trim().toLowerCase(Locale.ROOT);
            if (termo.isEmpty()) {
                ListaSugestoes.ocultar(listSugestoesResponsavel);
                return;
            }
            List<Cliente> resultados = candidatosAResponsavel().stream()
                    .filter(c -> c.getNome() != null && c.getNome().toLowerCase(Locale.ROOT).contains(termo)).toList();
            if (resultados.isEmpty()) {
                ListaSugestoes.ocultar(listSugestoesResponsavel);
            } else {
                listSugestoesResponsavel.setItems(FXCollections.observableArrayList(resultados));
                ListaSugestoes.exibir(listSugestoesResponsavel, txtResponsavel);
            }
        });
        listSugestoesResponsavel.setCellFactory(ListaSugestoes.fabricaCelulas(
                c -> c.getNome() + "  —  " + (c.getTelefone() == null ? "sem telefone" : c.getTelefone())));
        listSugestoesResponsavel.setOnMouseClicked(e ->
                selecionarResponsavel(listSugestoesResponsavel.getSelectionModel().getSelectedItem()));
        listSugestoesResponsavel.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                selecionarResponsavel(listSugestoesResponsavel.getSelectionModel().getSelectedItem());
            }
        });
        txtResponsavel.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                ListaSugestoes.ocultar(listSugestoesResponsavel);
            }
        });
        ListaSugestoes.ocultarAoPerderFoco(txtResponsavel, listSugestoesResponsavel);
    }

    // Só titulares podem ser responsáveis, e ninguém é responsável por si mesmo.
    private List<Cliente> candidatosAResponsavel() {
        return repositorio.getItens().stream().filter(c -> !c.isDependente() && c != clienteEmEdicao).toList();
    }

    private void selecionarResponsavel(Cliente responsavel) {
        if (responsavel == null) {
            return;
        }
        atualizandoProgramaticamente = true;
        try {
            txtResponsavel.setText(responsavel.getNome());
        } finally {
            atualizandoProgramaticamente = false;
        }
        responsavelEscolhido = responsavel;
        atualizarEstadoDoVinculo();
        erros.limpar(txtResponsavel);
        ListaSugestoes.ocultar(listSugestoesResponsavel);
        txtResponsavel.positionCaret(txtResponsavel.getText().length());
    }

    // ===================== Tabelas de apoio (dependentes e receitas) =====================

    private void configurarTabelas() {
        Celulas.coluna(colDepNome, c -> Celulas.texto(c.getNome()), null);
        Celulas.coluna(colDepParentesco, c -> Celulas.texto(c.getParentesco()), null);
        Celulas.coluna(colDepIdade, c -> {
            Integer idade = Cliente.calcularIdade(c.getDataNascimento());
            return Celulas.texto(idade == null ? null : idade + (idade == 1 ? " ano" : " anos"));
        }, null);
        Celulas.coluna(colDepAcoes, c -> {
            Button abrir = Botoes.icone("editar", "Abrir cadastro de " + c.getNome(), false);
            abrir.setOnAction(e -> abrirCadastro(c));
            return Celulas.acoes(abrir);
        }, null);
        colDepAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        Celulas.coluna(colRecCodigo, r -> Celulas.numero(r.getCodigo()), null);
        Celulas.coluna(colRecData, r -> Celulas.texto(r.getData() == null ? null : FORMATO_DATA.format(r.getData())), null);
        Celulas.coluna(colRecPaciente, r -> Celulas.texto(r.getNomePaciente()), null);
        Celulas.coluna(colRecMedicamentos, r -> {
            FlowPane chips = new FlowPane(6, 6);
            r.getItens().forEach(i -> {
                Label chip = new Label(i.getNomeRemedio() + (i.getDinamizacao() == null ? "" : " " + i.getDinamizacao()));
                chip.getStyleClass().add("v2-chip");
                chips.getChildren().add(chip);
            });
            chips.setPrefWrapLength(240);
            return chips;
        }, null);
        Celulas.coluna(colRecAcoes, r -> {
            Button abrir = Botoes.icone("editar", "Abrir receita " + r.getCodigo(), false);
            abrir.setOnAction(e -> {
                ContextoReceita.iniciarEdicao(r);
                MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — Editar Receita");
            });
            return Celulas.acoes(abrir);
        }, null);
        colRecAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void abrirCadastro(Cliente cliente) {
        ContextoCliente.iniciarEdicao(cliente);
        MenuLateralController.navegar(rootStack, "/fxml/ClienteFormView.fxml", "Farmácia Homeopática — Editar Cliente");
    }

    @FXML
    private void handleAdicionarDependente() {
        if (clienteEmEdicao == null) {
            return;
        }
        ContextoCliente.iniciarNovoDependente(clienteEmEdicao);
        MenuLateralController.navegar(rootStack, "/fxml/ClienteFormView.fxml", "Farmácia Homeopática — Novo Dependente");
    }

    @FXML
    private void handleNovaReceita() {
        if (clienteEmEdicao == null) {
            return;
        }
        ContextoReceita.iniciarNovaPara(clienteEmEdicao);
        MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — Nova Receita");
    }

    // ===================== Salvar / cancelar =====================

    @FXML
    private void handleSalvarCliente() {
        erros.limpar();
        boolean dataValida = Datas.confirmarTextoDigitado(dpDataNascimento);
        Cliente responsavel = resolverResponsavel();
        // Responsável reconhecido pelo nome digitado (sem escolher na lista): os asteriscos passam a mostrar o vínculo
        // (Telefone opcional, Parentesco obrigatório), de acordo com a validação abaixo.
        if (responsavel != null && responsavelEscolhido == null) {
            responsavelEscolhido = responsavel;
            atualizarEstadoDoVinculo();
        }
        String textoResponsavel = txtResponsavel.getText() == null ? "" : txtResponsavel.getText().trim();
        String parentesco = valorOuNulo(cbParentesco.getValue());

        if (txtNome.getText() == null || txtNome.getText().isBlank()) {
            erros.mostrar(txtNome, "Informe o nome do cliente.");
        }
        if (!textoResponsavel.isEmpty() && responsavel == null) {
            erros.mostrar(txtResponsavel, "Escolha um responsável da lista de sugestões.");
        } else if (responsavel != null && clienteEmEdicao != null && !repositorio.dependentesDe(clienteEmEdicao).isEmpty()) {
            erros.mostrar(txtResponsavel, "Este cliente possui dependentes e não pode ser dependente de outra pessoa.");
        }
        if (responsavel == null) {
            String tel = txtTelefone.getText();
            if (tel == null || tel.isBlank()) {
                erros.mostrar(txtTelefone, "Informe um telefone para contato.");
            } else if (!Mascaras.telefoneValido(tel)) {
                erros.mostrar(txtTelefone, "Telefone incompleto: informe o DDD e o número.");
            }
        } else {
            if (parentesco == null) {
                erros.mostrar(cbParentesco, "Selecione o parentesco com o responsável.");
            }
            String tel = txtTelefone.getText();
            if (tel != null && !tel.isBlank() && !Mascaras.telefoneValido(tel)) {
                erros.mostrar(txtTelefone, "Telefone incompleto: informe o DDD e o número ou deixe em branco.");
            }
        }
        if (!dataValida) {
            erros.mostrar(dpDataNascimento, "Data inválida: use o formato dd/mm/aaaa.");
        } else if (dpDataNascimento.getValue() != null && dpDataNascimento.getValue().isAfter(LocalDate.now())) {
            erros.mostrar(dpDataNascimento, "A data de nascimento não pode estar no futuro.");
        }
        String cpf = txtCpf.getText();
        if (cpf != null && !cpf.isBlank()) {
            if (!Mascaras.cpfValido(cpf)) {
                erros.mostrar(txtCpf, "CPF inválido: verifique os dígitos.");
            } else {
                Cliente duplicado = repositorio.getItens().stream()
                        .filter(c -> c != clienteEmEdicao && Mascaras.somenteDigitos(c.getCpf()).equals(Mascaras.somenteDigitos(cpf)))
                        .findFirst().orElse(null);
                if (duplicado != null) {
                    erros.mostrar(txtCpf, "Este CPF já está cadastrado para " + duplicado.getNome() + ".");
                }
            }
        }

        if (erros.temErros()) {
            Toast.mostrar(rootStack, Toast.Tipo.ERRO, "Corrija os campos destacados para salvar.");
            Control primeiro = erros.campos().get(0);
            primeiro.requestFocus();
            return;
        }

        // Homônimo: não bloqueia, mas pergunta (pode ser outra pessoa com o mesmo nome).
        String nome = txtNome.getText().trim();
        Cliente homonimo = repositorio.getItens().stream()
                .filter(c -> c != clienteEmEdicao && c.getNome() != null && c.getNome().equalsIgnoreCase(nome))
                .findFirst().orElse(null);
        final String parentescoFinal = parentesco;
        if (homonimo != null) {
            Caixas.confirmar(rootStack, "alerta", false, "Já existe um cliente com este nome",
                    "Existe um cadastro de " + homonimo.getNome() + " (" + homonimo.getCodigo() + "). Se for a mesma pessoa, cancele e edite o cadastro existente.",
                    null, "Salvar mesmo assim", () -> gravar(responsavel, parentescoFinal));
            return;
        }
        gravar(responsavel, parentesco);
    }

    private void gravar(Cliente responsavel, String parentesco) {
        Cliente salvo;
        String mensagem;
        if (clienteEmEdicao == null) {
            salvo = new Cliente();
            preencherCliente(salvo, responsavel, parentesco);
            repositorio.adicionar(salvo);
            mensagem = (responsavel == null ? "Cliente " : "Dependente ") + salvo.getNome() + " cadastrado com sucesso.";
        } else {
            salvo = clienteEmEdicao;
            preencherCliente(salvo, responsavel, parentesco);
            repositorio.atualizar(salvo);
            mensagem = "Cadastro de " + salvo.getNome() + " atualizado.";
        }
        ContextoCliente.deixarAviso(ContextoCliente.TipoAviso.SUCESSO, mensagem, salvo);
        if (ContextoCliente.voltaParaReceita()) {
            com.farmacia.util.ContextoReceita.registrarCadastroRecemCriado(salvo);
        }
        alteracoes.zerar();
        voltarParaLista();
    }

    // O responsável é o escolhido na lista; se o texto coincide exatamente com um titular, também vale.
    private Cliente resolverResponsavel() {
        if (responsavelEscolhido != null) {
            return responsavelEscolhido;
        }
        String texto = txtResponsavel.getText() == null ? "" : txtResponsavel.getText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        List<Cliente> iguais = candidatosAResponsavel().stream()
                .filter(c -> c.getNome() != null && c.getNome().equalsIgnoreCase(texto)).toList();
        return iguais.size() == 1 ? iguais.get(0) : null;
    }

    private void preencherCliente(Cliente cliente, Cliente responsavel, String parentesco) {
        cliente.setNome(txtNome.getText().trim());
        cliente.setTelefone(valorOuNulo(txtTelefone.getText()));
        cliente.setRua(valorOuNulo(txtRua.getText()));
        cliente.setNumero(valorOuNulo(txtNumero.getText()));
        cliente.setBairro(valorOuNulo(txtBairro.getText()));
        cliente.setCep(valorOuNulo(txtCep.getText()));
        cliente.setComplemento(valorOuNulo(txtComplemento.getText()));
        cliente.setEstado(cbEstado.getValue());
        cliente.setCidade(valorOuNulo(txtCidade.getText()));
        cliente.setDataNascimento(dpDataNascimento.getValue());
        cliente.setIdade(Cliente.calcularIdade(dpDataNascimento.getValue()));
        cliente.setCpf(valorOuNulo(txtCpf.getText()));
        cliente.setRg(valorOuNulo(txtRg.getText()));
        cliente.setResponsavel(responsavel);
        cliente.setParentesco(responsavel == null ? null : parentesco);
    }

    private String valorOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    @FXML
    private void handleCancelar() {
        // Com alterações pendentes, o Guarda pergunta antes de descartar.
        Guarda.sair(this::voltarParaLista);
    }

    private void voltarParaLista() {
        Guarda.limpar();
        // Veio da receita: salvar ou cancelar devolve ao receituário em preenchimento, não à lista de clientes.
        if (ContextoCliente.encerrarRetornoParaReceita()) {
            MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — Nova Receita");
            return;
        }
        MenuLateralController.navegar(rootStack, "/fxml/ClienteView.fxml", "Farmácia Homeopática — Gerenciamento de Clientes");
    }
}
