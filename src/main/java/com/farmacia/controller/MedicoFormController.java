package com.farmacia.controller;

import com.farmacia.model.Medico;
import com.farmacia.repository.MedicoRepository;
import com.farmacia.ui.Alteracoes;
import com.farmacia.ui.AtalhosLista;
import com.farmacia.ui.Botoes;
import com.farmacia.ui.Caixas;
import com.farmacia.ui.ErroInline;
import com.farmacia.ui.Guarda;
import com.farmacia.ui.Icones;
import com.farmacia.ui.Mascaras;
import com.farmacia.ui.Toast;
import com.farmacia.util.ContextoMedico;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Controller do cadastro de médico (tema v2), nos modos NOVO e EDITAR.
public class MedicoFormController {

    private static final List<String> UFS = List.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    // Aceita "CRM 45210-SP", "45210/sp", "crm45210 sp"...: número (3 a 7 dígitos) e sigla do estado.
    private static final Pattern PADRAO_CRM = Pattern.compile("^(?:CRM)?\\s*(\\d{3,7})\\s*[-/ ]?\\s*([A-Z]{2})$");

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
    private HBox avisoReceitas;
    @FXML
    private StackPane avisoReceitasIcone;
    @FXML
    private Label lblAvisoReceitas;

    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtCrm;
    @FXML
    private TextField txtTelefone;

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
    private Button btnCancelar;
    @FXML
    private Button btnSalvarMedico;

    private final MedicoRepository repositorio = MedicoRepository.getInstance();
    private final ErroInline.Grupo erros = new ErroInline.Grupo();
    private Alteracoes alteracoes;

    private Medico medicoEmEdicao;

    @FXML
    private void initialize() {
        menuLateralController.marcarAtivo(MenuLateralController.MEDICOS);

        btnCancelar.setGraphic(Botoes.composto(null, "Cancelar", "Esc"));
        com.farmacia.ui.Seletores.comTextoDeExemplo(cbEstado);
        btnSalvarMedico.setGraphic(Botoes.composto("ok", "Salvar médico", "Ctrl S"));
        avisoReceitasIcone.getChildren().add(Icones.criar("info", 18));
        lnkLista.setOnMouseClicked(e -> handleCancelar());

        Mascaras.telefone(txtTelefone);
        Mascaras.cep(txtCep);
        // Ao sair do campo, o CRM válido é arrumado no padrão "CRM 45210-SP" (mostra como será salvo).
        txtCrm.focusedProperty().addListener((obs, antes, focado) -> {
            if (!focado) {
                arrumarCrm();
            }
        });

        aplicarPedido(ContextoMedico.consumirPedido());

        alteracoes = new Alteracoes(txtNome, txtCrm, txtTelefone, txtCep, txtRua, txtNumero, txtComplemento,
                txtBairro, txtCidade, cbEstado);
        alteracoes.zerar();
        Guarda.ativar(rootStack, alteracoes::isSujo);
        AtalhosLista.instalarFormulario(rootStack, this::handleSalvarMedico, this::handleCancelar);
        txtNome.requestFocus();
    }

    // ===================== Modos =====================

    private void aplicarPedido(ContextoMedico.Pedido pedido) {
        if (pedido.modo() == ContextoMedico.Modo.EDITAR) {
            prepararEdicao(pedido.medico());
        } else {
            prepararNovo();
        }
        // Aberto pela receita: o nome digitado lá já vem preenchido e o fim do cadastro devolve ao receituário.
        String sugerido = ContextoMedico.consumirNomeSugerido();
        if (sugerido != null) {
            txtNome.setText(sugerido);
        }
        if (ContextoMedico.voltaParaReceita()) {
            lnkLista.setText("Nova receita");
            lblSubtituloPagina.setText("Cadastre o médico. Ao salvar, você volta para a receita que estava preenchendo.");
        }
    }

    private void prepararNovo() {
        medicoEmEdicao = null;
        definirCabecalho("Novo médico", "Novo médico", "Preencha os dados do médico. Só Nome e CRM são obrigatórios.");
        preencher(repositorio.proximoCodigo(), new Medico());
        mostrarAvisoReceitas(0);
    }

    private void prepararEdicao(Medico medico) {
        medicoEmEdicao = medico;
        definirCabecalho("Editar " + medico.getNome(), "Editar médico " + medico.getCodigo(),
                "Altere os dados necessários e salve.");
        preencher(medico.getCodigo(), medico);
        mostrarAvisoReceitas(repositorio.contarReceitas(medico));
    }

    private void definirCabecalho(String titulo, String trilha, String subtitulo) {
        lblTituloPagina.setText(titulo);
        lblTrilhaAtual.setText(trilha);
        lblSubtituloPagina.setText(subtitulo);
    }

    // Prevenção de erro: avisa antes de mexer em nome/CRM de quem já tem receitas emitidas.
    private void mostrarAvisoReceitas(int n) {
        boolean mostrar = n > 0;
        avisoReceitas.setVisible(mostrar);
        avisoReceitas.setManaged(mostrar);
        if (mostrar) {
            lblAvisoReceitas.setText((n == 1 ? "Este médico aparece em 1 receita." : "Este médico aparece em " + n + " receitas.")
                    + " Se mudar o nome ou o CRM, as receitas já emitidas continuam com os dados anteriores.");
        }
    }

    private void preencher(String codigo, Medico base) {
        txtCodigo.setText(codigo);
        txtNome.setText(nz(base.getNome()));
        txtCrm.setText(nz(base.getCrm()));
        txtTelefone.setText(nz(base.getTelefone()));
        txtCep.setText(nz(base.getCep()));
        txtRua.setText(nz(base.getRua()));
        txtNumero.setText(nz(base.getNumero()));
        txtComplemento.setText(nz(base.getComplemento()));
        txtBairro.setText(nz(base.getBairro()));
        txtCidade.setText(nz(base.getCidade()));
        cbEstado.setValue(base.getEstado());
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }

    // ===================== CRM =====================

    // "CRM 45210-SP" quando o texto é um CRM válido; nulo quando o formato (ou a sigla do estado) não confere.
    private static String crmPadronizado(String texto) {
        if (texto == null) {
            return null;
        }
        Matcher m = PADRAO_CRM.matcher(texto.trim().toUpperCase(Locale.ROOT));
        if (!m.matches() || !UFS.contains(m.group(2))) {
            return null;
        }
        return "CRM " + m.group(1) + "-" + m.group(2);
    }

    private void arrumarCrm() {
        String padronizado = crmPadronizado(txtCrm.getText());
        if (padronizado != null && !padronizado.equals(txtCrm.getText())) {
            txtCrm.setText(padronizado);
        }
    }

    // ===================== Salvar / cancelar =====================

    @FXML
    private void handleSalvarMedico() {
        erros.limpar();
        String nome = valorOuNulo(txtNome.getText());
        String crmDigitado = valorOuNulo(txtCrm.getText());

        if (nome == null) {
            erros.mostrar(txtNome, "Informe o nome do médico.");
        }

        String crm = null;
        if (crmDigitado == null) {
            erros.mostrar(txtCrm, "Informe o CRM do médico.");
        } else {
            crm = crmPadronizado(crmDigitado);
            if (crm == null) {
                erros.mostrar(txtCrm, "CRM inválido: use o número e a sigla do estado, como CRM 45210-SP.");
            } else {
                Medico duplicado = repositorio.buscarPorCrm(crm, medicoEmEdicao);
                if (duplicado != null) {
                    erros.mostrar(txtCrm, "Este CRM já está cadastrado para " + duplicado.getNome() + " (" + duplicado.getCodigo() + ").");
                }
            }
        }

        String telefone = txtTelefone.getText();
        if (telefone != null && !telefone.isBlank() && !Mascaras.telefoneValido(telefone)) {
            erros.mostrar(txtTelefone, "Telefone incompleto: informe o DDD e o número ou deixe em branco.");
        }
        String cep = txtCep.getText();
        if (cep != null && !cep.isBlank() && Mascaras.somenteDigitos(cep).length() != 8) {
            erros.mostrar(txtCep, "CEP incompleto: informe os 8 dígitos ou deixe em branco.");
        }

        if (erros.temErros()) {
            Toast.mostrar(rootStack, Toast.Tipo.ERRO, "Corrija os campos destacados para salvar.");
            focarPrimeiroComErro();
            return;
        }

        // Homônimo: não bloqueia (o CRM é que identifica o médico), mas pergunta.
        Medico homonimo = repositorio.buscarPorNome(nome, medicoEmEdicao);
        if (homonimo != null) {
            final String crmFinal = crm;
            Caixas.confirmar(rootStack, "alerta", false, "Já existe um médico com este nome",
                    "Existe um cadastro de " + homonimo.getNome() + " (" + homonimo.getCrm() + "). "
                            + "Se for a mesma pessoa, cancele e edite o cadastro existente.",
                    null, "Salvar mesmo assim", () -> gravar(nome, crmFinal));
            return;
        }
        gravar(nome, crm);
    }

    // Foco no primeiro campo com erro, na ordem em que aparecem na tela.
    private void focarPrimeiroComErro() {
        List<Control> comErro = erros.campos();
        for (Control campo : List.of(txtNome, txtCrm, txtTelefone, txtCep)) {
            if (comErro.contains(campo)) {
                campo.requestFocus();
                return;
            }
        }
    }

    private void gravar(String nome, String crm) {
        Medico salvo;
        String mensagem;
        if (medicoEmEdicao == null) {
            salvo = new Medico();
            preencherMedico(salvo, nome, crm);
            repositorio.adicionar(salvo);
            mensagem = "Médico " + salvo.getNome() + " cadastrado com sucesso.";
        } else {
            salvo = medicoEmEdicao;
            int emitidas = repositorio.contarReceitas(salvo);
            boolean mudouIdentificacao = !nome.equalsIgnoreCase(nz(salvo.getNome()).trim())
                    || !crm.equalsIgnoreCase(nz(salvo.getCrm()).trim());
            preencherMedico(salvo, nome, crm);
            repositorio.atualizar(salvo);
            mensagem = "Cadastro de " + salvo.getNome() + " atualizado."
                    + (mudouIdentificacao && emitidas > 0
                    ? " As " + emitidas + (emitidas == 1 ? " receita já emitida mantém" : " receitas já emitidas mantêm") + " os dados anteriores."
                    : "");
        }
        ContextoMedico.deixarAviso(ContextoMedico.TipoAviso.SUCESSO, mensagem, salvo);
        if (ContextoMedico.voltaParaReceita()) {
            com.farmacia.util.ContextoReceita.registrarCadastroRecemCriado(salvo);
        }
        alteracoes.zerar();
        voltarParaLista();
    }

    private void preencherMedico(Medico medico, String nome, String crm) {
        medico.setNome(nome);
        medico.setCrm(crm);
        medico.setTelefone(valorOuNulo(txtTelefone.getText()));
        medico.setRua(valorOuNulo(txtRua.getText()));
        medico.setNumero(valorOuNulo(txtNumero.getText()));
        medico.setBairro(valorOuNulo(txtBairro.getText()));
        medico.setCep(valorOuNulo(txtCep.getText()));
        medico.setComplemento(valorOuNulo(txtComplemento.getText()));
        medico.setEstado(cbEstado.getValue());
        medico.setCidade(valorOuNulo(txtCidade.getText()));
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
        // Veio da receita: salvar ou cancelar devolve ao receituário em preenchimento, não à lista de médicos.
        if (ContextoMedico.encerrarRetornoParaReceita()) {
            MenuLateralController.navegar(rootStack, "/fxml/ReceitaFormView.fxml", "Farmácia Homeopática — Nova Receita");
            return;
        }
        MenuLateralController.navegar(rootStack, "/fxml/MedicoView.fxml", "Farmácia Homeopática — Gerenciamento de Médicos");
    }
}
