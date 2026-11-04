package com.farmacia.controller;

import com.farmacia.model.Cliente;
import com.farmacia.model.Receita;
import com.farmacia.repository.ClienteRepository;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.EstadoLista;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoCliente;
import com.farmacia.util.ContextoReceita;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// Controller da lista de Clientes (tema v2): titulares com os dependentes logo abaixo. Cadastro em ClienteFormView.
public class ClienteController {

    private static final String CHAVE_ESTADO = "clientes";

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
    private TextField txtFiltroClientes;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnNovoCliente;
    @FXML
    private ToggleGroup grupoTipo;
    @FXML
    private ToggleButton tgTodos;
    @FXML
    private ToggleButton tgTitulares;
    @FXML
    private ToggleButton tgDependentes;
    @FXML
    private TableView<Cliente> tableClientes;
    @FXML
    private TableColumn<Cliente, Cliente> colCodigo;
    @FXML
    private TableColumn<Cliente, Cliente> colNome;
    @FXML
    private TableColumn<Cliente, Cliente> colTelefone;
    @FXML
    private TableColumn<Cliente, Cliente> colCpf;
    @FXML
    private TableColumn<Cliente, Cliente> colCidade;
    @FXML
    private TableColumn<Cliente, Cliente> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final ClienteRepository repositorio = ClienteRepository.getInstance();
    private final ReceitaRepository receitas = ReceitaRepository.getInstance();
    private FilteredList<Cliente> clientesFiltrados;
    private Label contTodos;
    private Label contTitulares;
    private Label contDependentes;

    // Família: o titular e, logo abaixo, os dependentes dele (titulares em ordem alfabética).
    private static final Comparator<Cliente> ORDEM_FAMILIA = ordemFamilia(Comparator.naturalOrder());
    // Nome decrescente: as famílias se invertem, mas o titular continua logo acima dos seus dependentes.
    private static final Comparator<Cliente> ORDEM_FAMILIA_DECRESCENTE = ordemFamilia(Comparator.reverseOrder());

    private static Comparator<Cliente> ordemFamilia(Comparator<String> nomes) {
        return Comparator.comparing((Cliente c) -> chaveFamilia(c), nomes)
                .thenComparing(Cliente::isDependente)
                .thenComparing((Cliente c) -> nomeMinusculo(c), nomes);
    }

    private static String chaveFamilia(Cliente c) {
        return nomeMinusculo(c.isDependente() ? c.getResponsavel() : c);
    }

    private static String nomeMinusculo(Cliente c) {
        return c.getNome() == null ? "" : c.getNome().toLowerCase(Locale.ROOT);
    }

    // Cidade que vale para o cliente: a própria ou, no dependente sem cidade própria, a do responsável (como o telefone).
    private static String cidadeContato(Cliente c) {
        return c.getCidade() == null && c.isDependente() ? c.getResponsavel().getCidade() : c.getCidade();
    }

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.CLIENTES);

        btnAjuda.setGraphic(Botoes.composto("ajuda", "Ajuda", "F1"));
        btnNovoCliente.setGraphic(Botoes.composto("mais", "Novo cliente", "Ctrl N"));
        dicaIcone.getChildren().add(Icones.criar("info", 18));
        dicaBox.setVisible(!dicaDispensada);
        dicaBox.setManaged(!dicaDispensada);
        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");

        contTodos = contador(tgTodos);
        contTitulares = contador(tgTitulares);
        contDependentes = contador(tgDependentes);
        grupoTipo.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
                return;
            }
            atualizarTudo();
        });
        txtFiltroClientes.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            atualizarTudo();
        });

        clientesFiltrados = new FilteredList<>(repositorio.getItens(), c -> true);
        configurarTabela();
        repositorio.getItens().addListener((ListChangeListener<Cliente>) c -> atualizarTudo());
        AtalhosLista.instalar(rootStack, txtFiltroClientes, tableClientes, this::handleNovoCliente,
                this::editarCliente, this::pedirExclusao, this::handleAjuda);

        // Voltando de um cadastro, a lista continua com a busca, a aba, a ordem e a linha de antes.
        EstadoLista.Estado anterior = EstadoLista.retomar(CHAVE_ESTADO);
        if (anterior != null) {
            EstadoLista.aplicar(anterior, txtFiltroClientes, grupoTipo, tableClientes);
        }
        atualizarTudo();
        exibirAvisoPendente();
    }

    private void guardarEstado() {
        EstadoLista.guardar(CHAVE_ESTADO, EstadoLista.capturar(txtFiltroClientes, grupoTipo, tableClientes));
    }

    private Label contador(ToggleButton botao) {
        Label l = new Label("0");
        l.getStyleClass().add("v2-cnt");
        botao.setGraphic(l);
        botao.setContentDisplay(ContentDisplay.RIGHT);
        return l;
    }

    // ===================== Tabela =====================

    private void configurarTabela() {
        Celulas.coluna(colCodigo, c -> Celulas.numero(c.getCodigo()), Comparator.comparing(Cliente::getCodigo));
        // Como coluna principal, Nome ordena por família (ver ordemAtual); como critério secundário, pelo nome do próprio cliente.
        Celulas.coluna(colNome, this::celulaNome, Comparator.comparing((Cliente c) -> nomeMinusculo(c)));
        Celulas.coluna(colTelefone, c -> c.getTelefone() == null && c.isDependente()
                ? Celulas.duasLinhas(c.getTelefoneContato(), "do responsável")
                : Celulas.texto(c.getTelefone()), null);
        Celulas.coluna(colCpf, c -> Celulas.texto(mascararCpf(c.getCpf())), null);
        // Dependente sem cidade própria mostra a do responsável, do mesmo jeito que o telefone.
        Celulas.coluna(colCidade, c -> c.getCidade() == null && c.isDependente()
                ? Celulas.duasLinhas(cidadeContato(c), "do responsável")
                : Celulas.texto(c.getCidade()), Comparator.comparing(
                (Cliente c) -> cidadeContato(c) == null ? "" : cidadeContato(c).toLowerCase(Locale.ROOT)));
        Celulas.coluna(colAcoes, this::celulaAcoes, null);
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        SortedList<Cliente> ordenadas = new SortedList<>(clientesFiltrados);
        // A ordem segue o cabeçalho: por Nome, as famílias ficam juntas; nas outras colunas, vale a ordenação da coluna.
        ordenadas.comparatorProperty().bind(Bindings.createObjectBinding(this::ordemAtual, tableClientes.comparatorProperty()));
        tableClientes.setItems(ordenadas);
        // A ordenação é feita pelo comparador acima; o TableView não deve reordenar por conta própria.
        tableClientes.setSortPolicy(tabela -> true);
        // Ordem inicial: por Nome, com a seta no cabeçalho (como "Data da prescrição" em Receitas).
        ordenarPorNome();
        // Sem ordenação (terceiro clique no cabeçalho), a lista volta à ordem padrão por Nome e a seta não some.
        tableClientes.getSortOrder().addListener((InvalidationListener) obs -> {
            if (tableClientes.getSortOrder().isEmpty()) {
                Platform.runLater(this::ordenarPorNome);
            }
        });

        tableClientes.setRowFactory(tv -> {
            TableRow<Cliente> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editarCliente(linha.getItem());
                }
            });
            return linha;
        });
    }

    // Comparador da lista conforme o cabeçalho: Nome (crescente ou decrescente) mantém cada titular com os dependentes
    // logo abaixo; as demais colunas usam a própria ordenação.
    private Comparator<Cliente> ordemAtual() {
        List<TableColumn<Cliente, ?>> ordem = tableClientes.getSortOrder();
        if (ordem.isEmpty()) {
            return ORDEM_FAMILIA;
        }
        if (ordem.get(0) == colNome) {
            return colNome.getSortType() == TableColumn.SortType.DESCENDING ? ORDEM_FAMILIA_DECRESCENTE : ORDEM_FAMILIA;
        }
        return tableClientes.getComparator();
    }

    private void ordenarPorNome() {
        if (tableClientes.getSortOrder().isEmpty()) {
            colNome.setSortType(TableColumn.SortType.ASCENDING);
            tableClientes.getSortOrder().add(colNome);
        }
    }

    private Node celulaNome(Cliente c) {
        Label nome = new Label(c.getNome());
        nome.getStyleClass().add("v2-link-neutro");
        nome.setTooltip(new Tooltip(c.getNome()));
        HBox linha1 = new HBox(8, nome);
        linha1.setAlignment(Pos.CENTER_LEFT);
        VBox caixa = new VBox(2, linha1);

        if (c.isDependente()) {
            linha1.getChildren().add(Celulas.selo("Dependente", "v2-selo-dependente"));
            Label sub = new Label((c.getParentesco() == null ? "Dependente" : c.getParentesco()) + " · " + c.getNomeResponsavel());
            sub.getStyleClass().add("v2-sub");
            sub.setTooltip(new Tooltip(c.getVinculo()));
            caixa.getChildren().add(sub);
            caixa.setPadding(new Insets(0, 0, 0, 22));
        } else {
            long deps = repositorio.dependentesDe(c).size();
            if (deps > 0) {
                Label sub = new Label(deps + (deps == 1 ? " dependente" : " dependentes"));
                sub.getStyleClass().add("v2-sub");
                caixa.getChildren().add(sub);
            }
        }
        String faltas = camposFaltando(c);
        if (!faltas.isEmpty()) {
            Node alerta = Icones.criar("alerta", 16);
            alerta.getStyleClass().add("v2-icone-alerta");
            Label marca = new Label();
            marca.setGraphic(alerta);
            Tooltip dica = new Tooltip("Cadastro incompleto: " + faltas);
            marca.setTooltip(dica);
            marca.setAccessibleText("Cadastro incompleto: " + faltas);
            linha1.getChildren().add(marca);
        }
        return caixa;
    }

    // Itens que fazem falta para um cadastro completo (aparecem no aviso do ícone de alerta). A regra é a mesma para
    // titulares e dependentes: CPF, data de nascimento e cidade; no dependente sem cidade própria, vale a do responsável.
    private String camposFaltando(Cliente c) {
        List<String> faltas = new ArrayList<>();
        if (c.getCpf() == null) {
            faltas.add("CPF não informado");
        }
        if (c.getDataNascimento() == null) {
            faltas.add("data de nascimento não informada");
        }
        if (cidadeContato(c) == null) {
            faltas.add("cidade não informada");
        }
        return String.join("; ", faltas);
    }

    private Node celulaAcoes(Cliente c) {
        Button editar = Botoes.icone("editar", "Editar cadastro de " + c.getNome(), false);
        editar.setOnAction(e -> editarCliente(c));
        Button receita = Botoes.icone("receitas", "Nova receita para " + c.getNome(), false);
        receita.setOnAction(e -> novaReceitaPara(c));
        Button excluir = Botoes.icone("lixeira", "Mover para a lixeira (Delete)", true);
        excluir.setOnAction(e -> pedirExclusao(c));
        return Celulas.acoes(editar, receita, excluir);
    }

    // CPF na listagem: só os extremos aparecem (123.***.***-00). O número completo fica no cadastro.
    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 14) {
            return cpf;
        }
        return cpf.substring(0, 4) + "***.***" + cpf.substring(11);
    }

    // ===================== Filtros e contagens =====================

    private Predicate<Cliente> filtroBusca() {
        String termo = txtFiltroClientes.getText() == null ? "" : txtFiltroClientes.getText().trim().toLowerCase(Locale.ROOT);
        return c -> termo.isEmpty()
                || contem(c.getNome(), termo) || contem(c.getCpf(), termo)
                || contem(c.getTelefoneContato(), termo) || contem(c.getNomeResponsavel(), termo)
                || contem(c.getCodigo(), termo);
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private void atualizarTudo() {
        if (clientesFiltrados == null) {
            return;
        }
        Predicate<Cliente> busca = filtroBusca();
        javafx.scene.control.Toggle selecionado = grupoTipo.getSelectedToggle();
        clientesFiltrados.setPredicate(c -> busca.test(c)
                && (selecionado == tgTodos || (selecionado == tgTitulares && !c.isDependente())
                || (selecionado == tgDependentes && c.isDependente())));

        List<Cliente> pelaBusca = repositorio.getItens().stream().filter(busca).toList();
        long titulares = pelaBusca.stream().filter(c -> !c.isDependente()).count();
        contTodos.setText(String.valueOf(pelaBusca.size()));
        contTitulares.setText(String.valueOf(titulares));
        contDependentes.setText(String.valueOf(pelaBusca.size() - titulares));

        int total = repositorio.getItens().size();
        long totalTit = repositorio.getItens().stream().filter(c -> !c.isDependente()).count();
        lblResumoLista.setText(total + (total == 1 ? " cliente" : " clientes") + " · " + totalTit
                + (totalTit == 1 ? " titular" : " titulares") + " · " + (total - totalTit)
                + ((total - totalTit) == 1 ? " dependente" : " dependentes"));
        int exibidos = clientesFiltrados.size();
        lblRodapeContagem.setText(total == 0 ? "Nenhum cliente cadastrado"
                : "Mostrando " + exibidos + " de " + total + (total == 1 ? " cliente" : " clientes"));
        tableClientes.setPlaceholder(estadoVazio(total == 0));
    }

    private Node estadoVazio(boolean semNenhum) {
        if (semNenhum) {
            Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar primeiro cliente", null);
            novo.setOnAction(e -> handleNovoCliente());
            return Celulas.estadoVazio("clientes", "Nenhum cliente cadastrado ainda",
                    "Comece cadastrando um titular. Dependentes são adicionados a partir dele.", novo);
        }
        String termo = txtFiltroClientes.getText() == null ? "" : txtFiltroClientes.getText().trim();
        Button limpar = Botoes.botao("v2-btn-ghost", null, "Limpar busca e filtro", null);
        limpar.setOnAction(e -> {
            txtFiltroClientes.clear();
            tgTodos.setSelected(true);
        });
        Button novo = Botoes.botao("v2-btn-primary", null, "Cadastrar novo cliente", null);
        novo.setOnAction(e -> handleNovoCliente());
        return Celulas.estadoVazio("buscar",
                termo.isEmpty() ? "Nenhum cliente neste filtro" : "Nenhum cliente para “" + termo + "”",
                "Verifique a grafia, busque pelo código, CPF ou telefone, ou mude o filtro entre titulares e dependentes.",
                limpar, novo);
    }

    // ===================== Ações =====================

    @FXML
    private void handleNovoCliente() {
        ContextoCliente.iniciarNovo();
        abrirFormulario("Novo Cliente");
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroClientes.clear();
        txtFiltroClientes.requestFocus();
    }

    @FXML
    private void handleDispensarDica() {
        dicaDispensada = true;
        dicaBox.setVisible(false);
        dicaBox.setManaged(false);
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Clientes",
                List.of("Dependentes são clientes ligados a um responsável e aparecem logo abaixo dele.",
                        "O telefone e a cidade de um dependente sem dados próprios são os do responsável.",
                        "O ícone de alerta ao lado do nome indica cadastro incompleto (falta CPF, data de nascimento ou cidade); passe o mouse para ver o que falta.",
                        "O ícone de receita cria uma nova receita já com este cliente como paciente.",
                        "Clientes excluídos vão para a Lixeira e podem ser restaurados."),
                List.of(new Caixas.Atalho("Ctrl N", "Novo cliente"),
                        new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Abrir o cadastro selecionado"),
                        new Caixas.Atalho("Delete", "Mover o cliente selecionado para a lixeira"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    private void editarCliente(Cliente cliente) {
        if (cliente == null) {
            return;
        }
        ContextoCliente.iniciarEdicao(cliente);
        abrirFormulario("Editar Cliente");
    }

    private void novaReceitaPara(Cliente cliente) {
        ContextoReceita.iniciarNovaPara(cliente);
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — Nova Receita");
    }

    private void abrirFormulario(String titulo) {
        guardarEstado();
        MenuLateralController.navegar(rootStack, "/fxml/ClienteFormView.fxml", "Farmácia Homeopática — " + titulo);
    }

    // ===================== Exclusão reversível =====================

    private void pedirExclusao(Cliente cliente) {
        if (cliente == null) {
            return;
        }
        List<Cliente> dependentes = repositorio.dependentesDe(cliente);
        long nReceitas = receitas.getItens().stream().filter(r -> r.getPaciente() == cliente
                || dependentes.contains(r.getPaciente())).count();
        String impacto = impacto(dependentes.size(), nReceitas);

        if (dependentes.isEmpty()) {
            Caixas.confirmar(rootStack, "lixeira", true,
                    "Mover " + cliente.getNome() + " para a lixeira?",
                    "O cadastro sai da lista, mas fica guardado na Lixeira. Você pode desfazer logo em seguida ou restaurá-lo depois."
                            + (nReceitas > 0 ? " As receitas dele continuam no histórico." : ""),
                    impacto.isEmpty() ? null : cliente.getNome() + " · " + impacto,
                    "Mover para a lixeira", () -> moverParaLixeira(cliente, dependentes, true));
        } else {
            Caixas.decidir(rootStack, "lixeira",
                    "O que fazer com os dependentes de " + cliente.getNome() + "?",
                    cliente.getNome() + " possui " + impacto + ". Escolha o que acontece com os dependentes; nada é apagado de vez.",
                    "Mover também os dependentes: " + dependentes.stream().map(Cliente::getNome).collect(java.util.stream.Collectors.joining(", ")),
                    "Mover também os dependentes", () -> moverParaLixeira(cliente, dependentes, true),
                    "Manter os dependentes como clientes", () -> moverParaLixeira(cliente, dependentes, false));
        }
    }

    private String impacto(int nDependentes, long nReceitas) {
        List<String> partes = new ArrayList<>();
        if (nDependentes > 0) {
            partes.add(nDependentes + (nDependentes == 1 ? " dependente" : " dependentes"));
        }
        if (nReceitas > 0) {
            partes.add(nReceitas + (nReceitas == 1 ? " receita" : " receitas"));
        }
        return String.join(" e ", partes);
    }

    // Move o cliente (e, se pedido, os dependentes) para a lixeira. Manter os dependentes os solta como clientes.
    private void moverParaLixeira(Cliente cliente, List<Cliente> dependentes, boolean levarDependentes) {
        record Antes(Cliente dependente, String parentesco, String telefone) {
        }
        List<Antes> soltos = new ArrayList<>();
        if (levarDependentes) {
            dependentes.forEach(repositorio::moverParaLixeira);
        } else {
            for (Cliente d : dependentes) {
                soltos.add(new Antes(d, d.getParentesco(), d.getTelefone()));
                if (d.getTelefone() == null) {
                    d.setTelefone(cliente.getTelefone());
                }
                d.setResponsavel(null);
                d.setParentesco(null);
                repositorio.atualizar(d);
            }
        }
        repositorio.moverParaLixeira(cliente);

        int n = levarDependentes ? dependentes.size() : 0;
        String texto = cliente.getNome() + " movido para a lixeira" + (n > 0 ? " com " + n + (n == 1 ? " dependente" : " dependentes") : "") + ".";
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, texto, "Desfazer", () -> {
            repositorio.restaurar(cliente);
            if (levarDependentes) {
                dependentes.forEach(repositorio::restaurar);
            }
            for (Antes a : soltos) {
                a.dependente().setResponsavel(cliente);
                a.dependente().setParentesco(a.parentesco());
                a.dependente().setTelefone(a.telefone());
                repositorio.atualizar(a.dependente());
            }
            tableClientes.getSelectionModel().select(cliente);
            Toast.mostrar(rootStack, Toast.Tipo.INFO, cliente.getNome() + " restaurado.");
        });
    }

    private void exibirAvisoPendente() {
        ContextoCliente.Aviso aviso = ContextoCliente.consumirAviso();
        if (aviso == null) {
            return;
        }
        Toast.mostrar(rootStack, aviso.tipo() == ContextoCliente.TipoAviso.SUCESSO ? Toast.Tipo.SUCESSO : Toast.Tipo.INFO, aviso.texto());
        Cliente destaque = aviso.destaque();
        if (destaque != null) {
            if (!clientesFiltrados.contains(destaque)) {
                tgTodos.setSelected(true);
            }
            if (!clientesFiltrados.contains(destaque)) {
                txtFiltroClientes.clear();
            }
            tableClientes.getSelectionModel().select(destaque);
            tableClientes.scrollTo(destaque);
        }
    }
}
