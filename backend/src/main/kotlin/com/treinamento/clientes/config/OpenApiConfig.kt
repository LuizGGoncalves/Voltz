package com.treinamento.clientes.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Voltz — API de Gestão de Clientes")
                .description(
                    """
                    API REST para gestão de clientes e unidades consumidoras (UC).

                    ## Funcionalidades
                    - **Clientes**: CRUD completo com exclusão lógica, paginação, ordenação e filtro por status
                    - **Unidades Consumidoras**: CRUD independente vinculado a clientes
                    - **Cadastros Pendentes**: fila de retry quando ViaCEP está indisponível
                    - **Análises MG**: eventos gerados para UCs em Minas Gerais
                    - **Autenticação**: JWT (access token + refresh token em cookie httpOnly)

                    ## Autenticação
                    1. Faça login via `POST /api/v1/auth/login` com `{ "username": "admin", "password": "admin123" }`
                    2. Copie o `accessToken` da resposta
                    3. Clique em **Authorize** (cadeado) e cole o token

                    ## Regras de negócio
                    - Documento (CPF/CNPJ) é validado com dígitos verificadores e deve ser único entre ativos
                    - UCs em **SP, RS ou PR** são bloqueadas (422)
                    - UCs em **MG** geram evento de análise automático
                    - Se o ViaCEP estiver fora do ar, o cadastro entra na fila e é processado automaticamente
                    """.trimIndent()
                )
                .version("1.0.0")
                .contact(
                    Contact()
                        .name("Luiz Gonçalves")
                        .url("https://github.com/LuizGGoncalves/Voltz")
                )
        )
        .addServersItem(Server().url("/").description("Local"))
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Access token JWT obtido via POST /api/v1/auth/login")
            )
        )
}
