package com.treinamento.clientes.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete

@Entity
@Table(name = "unidade_consumidora")
@SQLDelete(sql = "UPDATE unidade_consumidora SET ativo = false WHERE id = ?")
class UnidadeConsumidora(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var nome: String = "",

    @Column(name = "numero_instalacao", nullable = false, length = 50)
    var numeroInstalacao: String = "",

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "cep", column = Column(name = "endereco_cep", nullable = false, length = 8)),
        AttributeOverride(name = "logradouro", column = Column(name = "endereco_logradouro", nullable = false)),
        AttributeOverride(name = "numero", column = Column(name = "endereco_numero", nullable = false, length = 20)),
        AttributeOverride(name = "complemento", column = Column(name = "endereco_complemento", length = 100)),
        AttributeOverride(name = "bairro", column = Column(name = "endereco_bairro", nullable = false, length = 100)),
        AttributeOverride(name = "cidade", column = Column(name = "endereco_cidade", nullable = false, length = 100)),
        AttributeOverride(name = "uf", column = Column(name = "endereco_uf", nullable = false, length = 2))
    )
    var endereco: Endereco = Endereco(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    var cliente: Cliente? = null,

    @Column(nullable = false)
    var ativo: Boolean = true
)
