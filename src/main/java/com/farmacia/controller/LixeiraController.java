package com.farmacia.controller;

import com.farmacia.model.Cliente;
import com.farmacia.model.Funcionario;
import com.farmacia.model.ItemReceita;
import com.farmacia.model.Medico;
import com.farmacia.model.Produto;
import com.farmacia.model.Receita;
import com.farmacia.repository.ClienteRepository;
import com.farmacia.repository.FuncionarioRepository;
import com.farmacia.repository.MedicoRepository;
import com.farmacia.repository.ProdutoRepository;
import com.farmacia.repository.ReceitaRepository;
import com.farmacia.repository.RepositorioEmMemoria;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.Celulas;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Toast;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Controller da Lixeira: reúne os itens excluídos de todas as telas. Restaurar é reversível;
// apagar de vez e esvaziar pedem confirmação.
public class LixeiraController {

    private enum Tipo {
        RECEITA("Receita", "receitas"), CLIENTE("Cliente", "clientes"), MEDICO("Médico", "medicos"),
        PRODUTO("Produto", "produtos"), FUNCIONARIO("Funcionário", "funcionarios");

        private final String rotulo;
        private final String icone;

        Tipo(String rotulo, String icone) {
            this.rotulo = rotulo;
            this.icone = icone;
        }
    }

    // Um item da lixeira, já descrito para a tabela e com as ações de restaurar e apagar.
    private record ItemLixeira(Tipo tipo, Object registro, String codigo, String descricao, String detalhe,
                               long ordem, Runnable restaurar, Runnable apagar) {
    }

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane rootPane;
    @FXML
    private MenuLateralController menuLateralController;
    @FXML
    private Button btnAjuda;
    @FXML
    private TextField txtFiltroLixeira;
    @FXML
    private StackPane iconeBusca;
    @FXML
    private Label kbdBusca;
    @FXML
    private Button btnLimparBusca;
    @FXML
    private Button btnEsvaziar;
    @FXML
    private ToggleGroup grupoTipo;
    @FXML
    private ToggleButton tgTodos;
    @FXML
    private ToggleButton tgClientes;
    @FXML
    private ToggleButton tgReceitas;
    @FXML
    private ToggleButton tgMedicos;
    @FXML
    private ToggleButton tgProdutos;
    @FXML
    private ToggleButton tgFuncionarios;
    @FXML
    private TableView<ItemLixeira> tableLixeira;
    @FXML
    private TableColumn<ItemLixeira, ItemLixeira> colTipo;
    @FXML
    private TableColumn<ItemLixeira, ItemLixeira> colCodigo;
    @FXML
    private TableColumn<ItemLixeira, ItemLixeira> colDescricao;
    @FXML
    private TableColumn<ItemLixeira, ItemLixeira> colDetalhe;
    @FXML
    private TableColumn<ItemLixeira, ItemLixeira> colAcoes;
    @FXML
    private Label lblRodapeContagem;

    private final ReceitaRepository receitas = ReceitaRepository.getInstance();
    private final ClienteRepository clientes = ClienteRepository.getInstance();
    private final MedicoRepository medicos = MedicoRepository.getInstance();
    private final ProdutoRepository produtos = ProdutoRepository.getInstance();
    private final FuncionarioRepository funcionarios = FuncionarioRepository.getInstance();

    private final ObservableList<ItemLixeira> todos = FXCollections.observableArrayList();
    private FilteredList<ItemLixeira> exibidos;
    private final java.util.Map<ToggleButton, Label> contadores = new java.util.LinkedHashMap<>();
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.LIXEIRA);

        btnAjuda.setGraphic(Botoes.composto("ajuda", "Ajuda", "F1"));
        btnEsvaziar.setGraphic(Botoes.composto("lixeira", "Esvaziar lixeira", null));
        btnEsvaziar.disableProperty().bind(Bindings.isEmpty(todos));
        iconeBusca.getChildren().add(Icones.criar("buscar", 18));
        btnLimparBusca.setGraphic(Icones.criar("fechar", 16));
        btnLimparBusca.setTooltip(new Tooltip("Limpar busca"));
        btnLimparBusca.setAccessibleText("Limpar busca");
        txtFiltroLixeira.textProperty().addListener((obs, antigo, novo) -> {
            boolean temTexto = novo != null && !novo.isEmpty();
            btnLimparBusca.setVisible(temTexto);
            kbdBusca.setVisible(!temTexto);
            aplicarFiltro();
        });
        // Abas na mesma ordem do menu lateral.
        for (ToggleButton botao : List.of(tgTodos, tgClientes, tgReceitas, tgMedicos, tgProdutos, tgFuncionarios)) {
            Label contador = new Label("0");
            contador.getStyleClass().add("v2-cnt");
            botao.setGraphic(contador);
            botao.setContentDisplay(ContentDisplay.RIGHT);
            contadores.put(botao, contador);
        }

        Celulas.coluna(colTipo, i -> Celulas.selo(i.tipo().rotulo, "v2-selo-dependente"), null);
        Celulas.coluna(colCodigo, i -> Celulas.numero(i.codigo()), null);
        Celulas.coluna(colDescricao, i -> Celulas.texto(i.descricao()), null);
        Celulas.coluna(colDetalhe, i -> Celulas.texto(i.detalhe()), null);
        Celulas.coluna(colAcoes, this::acoes, null);
        colAcoes.setStyle("-fx-alignment: CENTER-RIGHT;");

        exibidos = new FilteredList<>(todos, i -> true);
        tableLixeira.setItems(exibidos);
        grupoTipo.selectedToggleProperty().addListener((obs, antigo, novo) -> {
            if (novo == null) {
                antigo.setSelected(true);
                return;
            }
            aplicarFiltro();
        });

        ListChangeListener<Object> aoMudar = c -> reconstruir();
        receitas.getLixeira().addListener(aoMudar);
        clientes.getLixeira().addListener(aoMudar);
        medicos.getLixeira().addListener(aoMudar);
        produtos.getLixeira().addListener(aoMudar);
        funcionarios.getLixeira().addListener(aoMudar);
        // Enter restaura o item selecionado; Delete pede para apagá-lo de vez (com confirmação). Não há "novo" aqui.
        AtalhosLista.instalar(rootStack, txtFiltroLixeira, tableLixeira, () -> { },
                item -> item.restaurar().run(), this::pedirExclusaoDefinitiva, this::handleAjuda);
        reconstruir();
    }

    @FXML
    private void handleLimparBusca() {
        txtFiltroLixeira.clear();
        txtFiltroLixeira.requestFocus();
    }

    @FXML
    private void handleAjuda() {
        Caixas.ajuda(rootStack, "Ajuda: Lixeira",
                List.of("Tudo o que é excluído nas outras telas fica aqui, do mais recente para o mais antigo.",
                        "Restaurar devolve o item à sua tela. Ao restaurar um titular, os dependentes excluídos com ele voltam junto; ao restaurar um dependente, o responsável volta com ele.",
                        "Excluir definitivamente e Esvaziar lixeira não podem ser desfeitos e sempre pedem confirmação.",
                        "A busca procura no tipo, no código, na descrição e nos detalhes do item, e as abas filtram por tipo."),
                List.of(new Caixas.Atalho("Ctrl F", "Ir para a busca"),
                        new Caixas.Atalho("/", "Ir para a busca (fora de um campo de texto)"),
                        new Caixas.Atalho("Enter", "Restaurar o item selecionado"),
                        new Caixas.Atalho("Delete", "Excluir definitivamente o item selecionado"),
                        new Caixas.Atalho("Esc", "Fechar caixas e limpar a busca"),
                        new Caixas.Atalho("F1", "Abrir esta ajuda")));
    }

    // ===================== Montagem da lista =====================

    private void reconstruir() {
        List<ItemLixeira> lista = new ArrayList<>();
        for (Receita r : receitas.getLixeira()) {
            String itens = r.getItens().stream().map(ItemReceita::getNomeRemedio).collect(Collectors.joining(", "));
            lista.add(new ItemLixeira(Tipo.RECEITA, r, r.getCodigo(), r.getNomePaciente(), itens.isBlank() ? null : itens,
                    receitas.ordemDeExclusao(r), () -> restaurar(receitas, r, "Receita " + r.getCodigo()),
                    () -> apagar(receitas, r, "Receita " + r.getCodigo())));
        }
        for (Cliente c : clientes.getLixeira()) {
            String detalhe = c.isDependente() ? c.getVinculo() : c.getTelefone();
            lista.add(new ItemLixeira(Tipo.CLIENTE, c, c.getCodigo(), c.getNome(), detalhe,
                    clientes.ordemDeExclusao(c), () -> restaurarCliente(c), () -> apagarCliente(c)));
        }
        for (Medico m : medicos.getLixeira()) {
            lista.add(new ItemLixeira(Tipo.MEDICO, m, m.getCodigo(), m.getNome(), m.getCrm(),
                    medicos.ordemDeExclusao(m), () -> restaurar(medicos, m, m.getNome()), () -> apagar(medicos, m, m.getNome())));
        }
        for (Produto p : produtos.getLixeira()) {
            String detalhe = (p.getCategoria() == null ? "" : p.getCategoria())
                    + (p.getPreco() == null ? "" : (p.getCategoria() == null ? "" : " · ") + MOEDA.format(p.getPreco()));
            lista.add(new ItemLixeira(Tipo.PRODUTO, p, p.getCodigo(), p.getNome(), detalhe.isBlank() ? null : detalhe,
                    produtos.ordemDeExclusao(p), () -> restaurar(produtos, p, p.getNome()), () -> apagar(produtos, p, p.getNome())));
        }
        for (Funcionario f : funcionarios.getLixeira()) {
            lista.add(new ItemLixeira(Tipo.FUNCIONARIO, f, f.getCodigo(), f.getNome(), f.getCargo(),
                    funcionarios.ordemDeExclusao(f), () -> restaurar(funcionarios, f, f.getNome()), () -> apagar(funcionarios, f, f.getNome())));
        }
        lista.sort(Comparator.comparingLong(ItemLixeira::ordem).reversed());
        todos.setAll(lista);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        Toggle selecionado = grupoTipo.getSelectedToggle();
        Tipo tipo = selecionado == tgReceitas ? Tipo.RECEITA : selecionado == tgClientes ? Tipo.CLIENTE
                : selecionado == tgMedicos ? Tipo.MEDICO : selecionado == tgProdutos ? Tipo.PRODUTO
                : selecionado == tgFuncionarios ? Tipo.FUNCIONARIO : null;
        String termo = txtFiltroLixeira.getText() == null ? "" : txtFiltroLixeira.getText().trim().toLowerCase(Locale.ROOT);
        Predicate<ItemLixeira> pelaBusca = i -> termo.isEmpty() || contem(i.codigo(), termo)
                || contem(i.descricao(), termo) || contem(i.detalhe(), termo) || contem(i.tipo().rotulo, termo);
        exibidos.setPredicate(i -> (tipo == null || i.tipo() == tipo) && pelaBusca.test(i));

        // As contagens das abas acompanham a busca, como nas demais telas.
        List<ItemLixeira> encontrados = todos.stream().filter(pelaBusca).toList();
        contadores.get(tgTodos).setText(String.valueOf(encontrados.size()));
        contadores.get(tgClientes).setText(String.valueOf(contar(encontrados, Tipo.CLIENTE)));
        contadores.get(tgReceitas).setText(String.valueOf(contar(encontrados, Tipo.RECEITA)));
        contadores.get(tgMedicos).setText(String.valueOf(contar(encontrados, Tipo.MEDICO)));
        contadores.get(tgProdutos).setText(String.valueOf(contar(encontrados, Tipo.PRODUTO)));
        contadores.get(tgFuncionarios).setText(String.valueOf(contar(encontrados, Tipo.FUNCIONARIO)));

        // Rodapé como nas demais listas. Com a lixeira vazia fica em branco: o aviso aparece só no centro da tabela.
        int n = todos.size();
        lblRodapeContagem.setText(n == 0 ? ""
                : "Mostrando " + exibidos.size() + " de " + n + (n == 1 ? " item" : " itens")
                + " · do mais recente para o mais antigo");
        tableLixeira.setPlaceholder(estadoVazio(n == 0, txtFiltroLixeira.getText() == null ? "" : txtFiltroLixeira.getText().trim()));
    }

    private Node estadoVazio(boolean lixeiraVazia, String termo) {
        if (lixeiraVazia) {
            return Celulas.estadoVazio("lixeira", "A lixeira está vazia",
                    "Quando você excluir um item, ele aparece aqui e pode ser restaurado.");
        }
        Button limpar = Botoes.botao("v2-btn-ghost", null, "Limpar busca e filtro", null);
        limpar.setOnAction(e -> {
            txtFiltroLixeira.clear();
            tgTodos.setSelected(true);
        });
        return Celulas.estadoVazio("buscar",
                termo.isEmpty() ? "Nada deste tipo na lixeira" : "Nenhum item para “" + termo + "”",
                "Verifique a grafia, busque pelo código ou mude o filtro entre os tipos de item.", limpar);
    }

    private boolean contem(String campo, String termo) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(termo);
    }

    private long contar(List<ItemLixeira> itens, Tipo tipo) {
        return itens.stream().filter(i -> i.tipo() == tipo).count();
    }

    private Node acoes(ItemLixeira item) {
        Button restaurar = Botoes.icone("restaurar", "Restaurar " + item.tipo().rotulo.toLowerCase(Locale.ROOT), false);
        restaurar.setOnAction(e -> item.restaurar().run());
        Button apagar = Botoes.icone("lixeira", "Excluir definitivamente", true);
        apagar.setOnAction(e -> pedirExclusaoDefinitiva(item));
        return Celulas.acoes(restaurar, apagar);
    }

    // ===================== Restaurar =====================

    private <T> void restaurar(RepositorioEmMemoria<T> repositorio, T registro, String nome) {
        repositorio.restaurar(registro);
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, nome + " restaurado.", "Desfazer", () -> {
            repositorio.moverParaLixeira(registro);
            Toast.mostrar(rootStack, Toast.Tipo.INFO, nome + " voltou para a lixeira.");
        });
    }

    // Restaurar um titular traz junto os dependentes excluídos com ele; restaurar um dependente traz o responsável.
    private void restaurarCliente(Cliente cliente) {
        List<Cliente> juntos = new ArrayList<>();
        juntos.add(cliente);
        if (cliente.isDependente() && clientes.getLixeira().contains(cliente.getResponsavel())) {
            juntos.add(cliente.getResponsavel());
        }
        if (!cliente.isDependente()) {
            clientes.getLixeira().stream().filter(d -> d.getResponsavel() == cliente).forEach(juntos::add);
        }
        List<Cliente> restaurados = List.copyOf(juntos);
        restaurados.forEach(clientes::restaurar);
        String texto = restaurados.size() == 1 ? cliente.getNome() + " restaurado."
                : cliente.getNome() + " restaurado junto com " + (restaurados.size() - 1)
                + (restaurados.size() == 2 ? " cadastro ligado." : " cadastros ligados.");
        Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, texto, "Desfazer", () -> {
            restaurados.forEach(clientes::moverParaLixeira);
            Toast.mostrar(rootStack, Toast.Tipo.INFO, "Voltou para a lixeira.");
        });
    }

    // ===================== Apagar de vez =====================

    private void pedirExclusaoDefinitiva(ItemLixeira item) {
        Caixas.confirmar(rootStack, "alerta", true,
                "Excluir definitivamente?",
                "Esta ação não pode ser desfeita. " + item.tipo().rotulo + " " + item.codigo() + " será apagado do sistema."
                        + (item.tipo() == Tipo.CLIENTE && !((Cliente) item.registro()).isDependente()
                        && clientes.getLixeira().stream().anyMatch(d -> d.getResponsavel() == item.registro())
                        ? " Os dependentes que estão na lixeira com ele também serão apagados." : ""),
                item.descricao(), "Excluir definitivamente", () -> {
                    item.apagar().run();
                    Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, "Excluído definitivamente.");
                });
    }

    private <T> void apagar(RepositorioEmMemoria<T> repositorio, T registro, String nome) {
        repositorio.excluirDefinitivamente(registro);
    }

    private void apagarCliente(Cliente cliente) {
        if (!cliente.isDependente()) {
            List.copyOf(clientes.getLixeira()).stream().filter(d -> d.getResponsavel() == cliente)
                    .forEach(clientes::excluirDefinitivamente);
        }
        clientes.excluirDefinitivamente(cliente);
    }

    @FXML
    private void handleEsvaziar() {
        int n = todos.size();
        if (n == 0) {
            return;
        }
        Caixas.confirmar(rootStack, "alerta", true, "Esvaziar a lixeira?",
                "Esta ação não pode ser desfeita. " + (n == 1 ? "O item será apagado" : "Os " + n + " itens serão apagados") + " definitivamente.",
                null, "Esvaziar lixeira", () -> {
                    receitas.esvaziarLixeira();
                    clientes.esvaziarLixeira();
                    medicos.esvaziarLixeira();
                    produtos.esvaziarLixeira();
                    funcionarios.esvaziarLixeira();
                    Toast.mostrar(rootStack, Toast.Tipo.SUCESSO, "Lixeira esvaziada.");
                });
    }
}
