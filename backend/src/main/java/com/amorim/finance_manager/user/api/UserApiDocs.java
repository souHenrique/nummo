package com.amorim.finance_manager.user.api;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.user.dto.ChangePasswordRequest;
import com.amorim.finance_manager.user.dto.ConfirmCurrentPasswordRequest;
import com.amorim.finance_manager.user.dto.UpdateProfileRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.*;

public interface UserApiDocs {

    @Operation(
            summary = "Consultar perfil",
            description = "Retorna o perfil do usuário autenticado"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = UserResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = USER_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<UserResponse> getCurrentUser();

    @Operation(
            summary = "Atualizar perfil",
            description = "Atualiza parcialmente o nome ou e-mail do usuário. Alterar o e-mail exige a senha atual.",
            requestBody =
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UpdateProfileRequest.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATE_PROFILE_REQUEST
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil atualizado",
                    content = @Content(
                            schema = @Schema(
                                    implementation = UserResponse.class
                            ),
                            examples = @ExampleObject(
                                    value = UPDATED_USER_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Atualização inválida ou senha atual incorreta",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = VALIDATION_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = UNAUTHORIZED_ERROR
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    value = PROFILE_EMAIL_ALREADY_EXISTS
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })
    ResponseEntity<UserResponse> updateCurrentUser(
            UpdateProfileRequest request
    );

    @Operation(
            summary = "Alterar senha",
            description = "Exige a senha atual, invalida os tokens anteriores e solicita um novo login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nova senha inválida ou igual à atual",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    ResponseEntity<Void> changeCurrentUserPassword(
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ChangePasswordRequest.class),
                            examples = @ExampleObject(value = CHANGE_PASSWORD_REQUEST)
                    )
            )
            ChangePasswordRequest request
    );

    @Operation(
            summary = "Excluir conta",
            description = "Exige a senha atual e realiza a exclusão lógica da conta, preservando os dados financeiros e invalidando as sessões ativas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta excluída logicamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Senha atual incorreta",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    ResponseEntity<Void> deleteCurrentUser(
            @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConfirmCurrentPasswordRequest.class),
                            examples = @ExampleObject(value = CONFIRM_CURRENT_PASSWORD_REQUEST)
                    )
            )
            ConfirmCurrentPasswordRequest request
    );
}
