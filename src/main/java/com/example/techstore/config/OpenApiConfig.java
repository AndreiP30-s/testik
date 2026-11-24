package com.example.techstore.config;

import com.example.techstore.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "basicAuth"),
        info =
                @Info(
                        title = "TechStore REST API",
                        version = "1.0.0",
                        description =
                                "Войдите кнопкой Authorize, указав email и пароль. **Добавить товар:** откройте «1. Товары», выберите «Добавить"
                                    + " смартфон» или другую категорию, нажмите Try it out,"
                                    + " заполните название и цену, затем Execute. JSON вводить не"
                                    + " нужно. Проверьте значения характеристик перед сохранением."
                                    + " **Посмотреть товары:** GET /api/products → Try it out →"
                                    + " Execute. **Изменить цену:** PATCH"
                                    + " /api/products/{article}/details. Расширенные JSON-запросы"
                                    + " находятся в последней группе."))
public class OpenApiConfig {
    private static final String ERROR_SCHEMA = "#/components/schemas/ErrorResponse";
    private static final String CATALOG_CONFLICT =
            "Каталог заблокирован действующей скидкой или нарушено ограничение целостности данных";
    private static final String DISCOUNT_CONFIG_ERROR = "Сервис скидок не настроен";
    private static final String DATABASE_UNAVAILABLE = "База данных недоступна";

    @Bean
    public OpenApiCustomizer errorDocumentation() {
        return api -> {
            boolean openApi31 = api.getOpenapi().startsWith("3.1");
            ModelConverters.getInstance(openApi31).readAll(ErrorResponse.class)
                    .forEach(api.getComponents()::addSchemas);

            api.getPaths().forEach((path, item) -> {
                if (!path.startsWith("/api/")) return;
                item.readOperationsMap().forEach((method, operation) -> {
                    ApiResponse unauthorized = errorResponse(
                            "Укажите действительные email и пароль через HTTP Basic");
                    Header challenge = new Header()
                            .schema(new StringSchema().example("Basic realm=\"TechStore\""));
                    operation.getResponses().putIfAbsent("401",
                            unauthorized.addHeaderObject("WWW-Authenticate", challenge));
                    if (path.equals("/api/users") || path.startsWith("/api/users/")
                            || method != PathItem.HttpMethod.GET) {
                        error(operation, "403", "У пользователя нет прав на эту операцию");
                    }
                });
            });

            PathItem products = api.getPaths().get("/api/products");
            PathItem product = api.getPaths().get("/api/products/{article}");
            PathItem details = api.getPaths().get("/api/products/{article}/details");

            error(products.getGet(), "400", "Неверный фильтр, диапазон цен или параметры пагинации");
            error(products.getGet(), "503", DATABASE_UNAVAILABLE);
            error(product.getGet(), "400", "Артикул должен быть целым числом");
            error(product.getGet(), "404", "Товар с таким артикулом не найден");
            error(product.getGet(), "503", DATABASE_UNAVAILABLE);

            catalogWrite(products.getPost(), "Некорректный JSON или данные товара",
                    "Бренд, категория или характеристика не найдены");
            catalogWrite(product.getPut(), "Неверный артикул, JSON или данные товара",
                    "Товар, бренд, категория или характеристика не найдены");
            catalogWrite(details.getPatch(), "Неверный артикул или данные формы",
                    "Товар или указанный бренд не найдены");

            Operation delete = product.getDelete();
            error(delete, "400", "Артикул должен быть целым числом");
            error(delete, "404", "Товар с таким артикулом не найден");
            error(delete, "409", CATALOG_CONFLICT);
            error(delete, "500", DISCOUNT_CONFIG_ERROR);
            error(delete, "503", DATABASE_UNAVAILABLE);
        };
    }

    private static void catalogWrite(Operation operation, String badRequest, String notFound) {
        error(operation, "400", badRequest);
        error(operation, "404", notFound);
        error(operation, "409", CATALOG_CONFLICT);
        error(operation, "415", "Тип содержимого запроса не поддерживается");
        error(operation, "500", DISCOUNT_CONFIG_ERROR);
        error(operation, "503", DATABASE_UNAVAILABLE);
    }

    private static void error(Operation operation, String status, String description) {
        operation.getResponses().putIfAbsent(status, errorResponse(description));
    }

    private static ApiResponse errorResponse(String description) {
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA))));
    }

    @Bean
    public OpenApiCustomizer easyOperationOrder() {
        return api -> {
            Paths original = api.getPaths();
            Paths ordered = new Paths();
            List<String> first =
                    List.of(
                            "/api/products/smartphones",
                            "/api/products/tablets",
                            "/api/products/laptops",
                            "/api/products/headphones",
                            "/api/products",
                            "/api/products/{article}",
                            "/api/products/{article}/details",
                            "/api/users/form",
                            "/api/users",
                            "/api/users/{email}",
                            "/api/users/{email}/form");
            for (String path : first)
                if (original.containsKey(path)) ordered.addPathItem(path, original.get(path));
            original.forEach(
                    (path, item) -> {
                        if (!ordered.containsKey(path)) ordered.addPathItem(path, item);
                    });
            advanced(ordered.get("/api/products").getPost());
            advanced(ordered.get("/api/products/{article}").getPut());
            advanced(ordered.get("/api/users").getPost());
            advanced(ordered.get("/api/users/{email}").getPut());
            api.setPaths(ordered);
        };
    }

    private static void advanced(Operation operation) {
        if (operation != null) operation.setTags(List.of("5. Расширенные JSON-запросы"));
    }
}
