package com.treinamento.clientes.domain.model

import com.treinamento.clientes.domain.vo.Documento
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "cliente")
@SQLDelete(sql = "UPDATE cliente SET ativo = false WHERE id = ? AND version = ?")
@EntityListeners(AuditingEntityListener::class)
class Cliente(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var nome: String = "",

    @Column(nullable = false, length = 14)
    var documento: Documento = Documento.of("00000000000"),

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

    @Column(nullable = false)
    var ativo: Boolean = true,

    @Version
    var version: Long = 0,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "cliente", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var unidadesConsumidoras: MutableList<UnidadeConsumidora> = mutableListOf()
) {
    fun adicionarUC(uc: UnidadeConsumidora) {
        uc.cliente = this
        unidadesConsumidoras.add(uc)
    }
}
