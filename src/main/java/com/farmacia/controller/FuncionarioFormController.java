package com.farmacia.controller;

import com.farmacia.model.Funcionario;
import com.farmacia.repository.FuncionarioRepository;
import com.farmacia.ui.Alteracoes;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.ErroInline;
import com.farmacia.ui.Guarda;
import com.farmacia.ui.Mascaras;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoFuncionario;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Controller do cadastro de funcionário (tema v2), nos modos NOVO e EDITAR.
public class FuncionarioFormController {

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
    private TextField txtCpf;
    @FXML
    private TextField txtTelefone;
    @FXML
    private TextField txtLogin;
    @FXML
    private ComboBox<String> cbCargo;

    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnSalvarFuncionario;

    private final FuncionarioRepository repositorio = FuncionarioRepository.getInstance();
    private final ErroInline.Grupo erros = new ErroInline.Grupo();
    private Alteracoes alteracoes;

    private Funcionario funcionarioEmEdicao;

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.FUNCIONARIOS);

        btnCancelar.setGraphic(Botoes.composto(null, "Cancelar", "Esc"));
        btnCancelar.setAccessibleText("Cancelar");
        btnSalvarFuncionario.setGraphic(Botoes.composto("ok", "Salvar funcionário", "Ctrl S"));
        btnSalvarFuncionario.setAccessibleText("Salvar funcionário");
        lnkLista.setOnMouseClicked(e -> handleCancelar());

        Mascaras.cpf(txtCpf);
        Mascaras.telefone(txtTelefone);
        configurarCargo();

        aplicarPedido(ContextoFuncionario.consumirPedido());

        alteracoes = new Alteracoes(txtNome, txtCpf, txtTelefone, txtLogin, cbCargo);
        alteracoes.zerar();
        Guarda.ativar(rootStack, alteracoes::isSujo);
        AtalhosLista.instalarFormulario(rootStack, this::handleSalvarFuncionario, this::cancelarPorTeclado);
        focarQuandoPronto(txtNome);
    }

    // O foco só "pega" depois que o CSS da tela foi aplicado (a navegação troca a raiz logo após carregar o FXML);
    // por isso espera o campo ganhar sua aparência (skin) antes de pedir o foco.
    private void focarQuandoPronto(TextField campo) {
        if (campo.getSkin() != null) {
            campo.requestFocus();
            return;
        }
        campo.skinProperty().addListener(new ChangeListener<Skin<?>>() {
            @Override
            public void changed(ObservableValue<? extends Skin<?>> obs, Skin<?> antigo, Skin<?> novo) {
                if (novo != null) {
                    campo.skinProperty().removeListener(this);
                    Platform.runLater(campo::requestFocus);
                }
            }
        });
    }

    // ===================== Cargo / Função (combo editável) =====================

    // Cargos já usados por outros funcionários entram na lista de sugestões (além dos padrões do FXML).
    // O valor da combo acompanha o texto digitado; assim, reescolher um item da lista sempre tem efeito
    // e o controle de alterações percebe quem só digitou.
    private void configurarCargo() {
        for (Funcionario f : repositorio.getItens()) {
            String cargo = f.getCargo();
            if (cargo != null && !cargo.isBlank()
                    && cbCargo.getItems().stream().noneMatch(c -> c.equalsIgnoreCase(cargo.trim()))) {
                cbCargo.getItems().add(cargo.trim());
            }
        }
        cbCargo.getEditor().textProperty().addListener((obs, antigo, novo) -> {
            String texto = (novo == null || novo.isEmpty()) ? null : novo;
            if (!Objects.equals(texto, cbCargo.getValue())) {
                cbCargo.setValue(texto);
            }
        });
    }

    // ===================== Modos =====================

    private void aplicarPedido(ContextoFuncionario.Pedido pedido) {
        if (pedido.modo() == ContextoFuncionario.Modo.EDITAR && pedido.funcionario() != null) {
            prepararEdicao(pedido.funcionario());
        } else {
            prepararNovo();
        }
    }

    private void prepararNovo() {
        funcionarioEmEdicao = null;
        definirCabecalho("Novo funcionário", "Novo funcionário",
                "Preencha os dados do funcionário. Só Nome e Login são obrigatórios.");
        preencher(repositorio.proximoCodigo(), new Funcionario());
    }

    private void prepararEdicao(Funcionario funcionario) {
        funcionarioEmEdicao = funcionario;
        definirCabecalho("Editar " + funcionario.getNome(), "Editar funcionário " + funcionario.getCodigo(),
                "Altere os dados necessários e salve.");
        preencher(funcionario.getCodigo(), funcionario);
    }

    private void definirCabecalho(String titulo, String trilha, String subtitulo) {
        lblTituloPagina.setText(titulo);
        lblTrilhaAtual.setText(trilha);
        lblSubtituloPagina.setText(subtitulo);
    }

    private void preencher(String codigo, Funcionario base) {
        txtCodigo.setText(codigo);
        txtNome.setText(nz(base.getNome()));
        txtCpf.setText(nz(base.getCpf()));
        txtTelefone.setText(nz(base.getTelefone()));
        txtLogin.setText(nz(base.getLogin()));
        cbCargo.setValue(base.getCargo());
        cbCargo.getEditor().setText(nz(base.getCargo()));
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }

    // ===================== Salvar / cancelar =====================

    @FXML
    private void handleSalvarFuncionario() {
        erros.limpar();

        if (txtNome.getText() == null || txtNome.getText().isBlank()) {
            erros.mostrar(txtNome, "Informe o nome do funcionário.");
        }

        String cpf = valorOuNulo(txtCpf.getText());
        if (cpf != null) {
            if (!Mascaras.cpfValido(cpf)) {
                erros.mostrar(txtCpf, "CPF inválido: verifique os dígitos.");
            } else {
                // Quem está na lixeira também conta: restaurá-lo depois não pode gerar dois cadastros com o mesmo CPF.
                Optional<Funcionario> ativo = repositorio.buscarPorCpf(cpf, funcionarioEmEdicao);
                Optional<Funcionario> naLixeira = repositorio.buscarPorCpfNaLixeira(cpf);
                if (ativo.isPresent()) {
                    erros.mostrar(txtCpf, "Este CPF já está cadastrado para " + ativo.get().getNome() + ".");
                } else if (naLixeira.isPresent()) {
                    erros.mostrar(txtCpf, "Este CPF pertence a " + naLixeira.get().getNome()
                            + ", que está na lixeira. Restaure o cadastro em vez de criar outro.");
                }
            }
        }

        String telefone = valorOuNulo(txtTelefone.getText());
        if (telefone != null && !Mascaras.telefoneValido(telefone)) {
            erros.mostrar(txtTelefone, "Telefone incompleto: informe o DDD e o número ou deixe em branco.");
        }

        String login = valorOuNulo(txtLogin.getText());
        if (login == null) {
            erros.mostrar(txtLogin, "Informe o login de acesso.");
        } else if (login.chars().anyMatch(Character::isWhitespace)) {
            erros.mostrar(txtLogin, "O login não pode ter espaços: use ponto ou sublinhado, como em maria.silva.");
        } else {
            // O login identifica o operador: dois funcionários não podem ter o mesmo (sem diferenciar maiúsculas).
            // Quem está na lixeira também reserva o seu, porque a Lixeira restaura sem revalidar.
            Optional<Funcionario> emUso = repositorio.buscarPorLogin(login).filter(f -> f != funcionarioEmEdicao);
            Optional<Funcionario> naLixeira = repositorio.buscarPorLoginNaLixeira(login);
            if (emUso.isPresent()) {
                erros.mostrar(txtLogin, "Este login já está em uso por " + emUso.get().getNome() + ".");
            } else if (naLixeira.isPresent()) {
                erros.mostrar(txtLogin, "Este login pertence a " + naLixeira.get().getNome()
                        + ", que está na lixeira. Restaure o cadastro ou escolha outro login.");
            }
        }

        if (erros.temErros()) {
            Toast.mostrar(rootStack, Toast.Tipo.ERRO, "Corrija os campos destacados para salvar.");
            primeiroCampoComErro().requestFocus();
            return;
        }
        gravar(login, cpf, telefone);
    }

    // O Grupo não garante a ordem dos erros; o foco vai para o primeiro campo com erro na ordem da tela.
    private Control primeiroCampoComErro() {
        for (Control campo : List.of(txtNome, txtCpf, txtTelefone, txtLogin, cbCargo)) {
            if (erros.campos().contains(campo)) {
                return campo;
            }
        }
        return erros.campos().get(0);
    }

    private void gravar(String login, String cpf, String telefone) {
        Funcionario salvo;
        String mensagem;
        if (funcionarioEmEdicao == null) {
            salvo = new Funcionario();
            preencherFuncionario(salvo, login, cpf, telefone);
            repositorio.adicionar(salvo);
            mensagem = "Funcionário " + salvo.getNome() + " cadastrado com sucesso.";
        } else {
            salvo = funcionarioEmEdicao;
            preencherFuncionario(salvo, login, cpf, telefone);
            repositorio.atualizar(salvo);
            mensagem = "Cadastro de " + salvo.getNome() + " atualizado.";
        }
        ContextoFuncionario.deixarAviso(ContextoFuncionario.TipoAviso.SUCESSO, mensagem, salvo);
        alteracoes.zerar();
        voltarParaLista();
    }

    private void preencherFuncionario(Funcionario funcionario, String login, String cpf, String telefone) {
        funcionario.setNome(txtNome.getText().trim());
        funcionario.setLogin(login);
        funcionario.setCargo(cargoDigitado());
        funcionario.setCpf(cpf);
        funcionario.setTelefone(telefone);
    }

    private String cargoDigitado() {
        String texto = valorOuNulo(cbCargo.getEditor().getText());
        return texto != null ? texto : valorOuNulo(cbCargo.getValue());
    }

    private String valorOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    // Esc com a lista de cargos aberta só fecha a lista; sem ela, cancela o cadastro (com a confirmação do Guarda).
    private void cancelarPorTeclado() {
        if (cbCargo.isShowing()) {
            cbCargo.hide();
        } else {
            handleCancelar();
        }
    }

    @FXML
    private void handleCancelar() {
        // Com alterações pendentes, o Guarda pergunta antes de descartar.
        Guarda.sair(this::voltarParaLista);
    }

    private void voltarParaLista() {
        Guarda.limpar();
        MenuLateralController.navegar(rootStack, "/fxml/FuncionarioView.fxml", "Farmácia Homeopática — Gerenciamento de Funcionários");
    }
}
