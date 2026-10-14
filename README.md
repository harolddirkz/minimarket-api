# Minimarket API

Backend REST para la operación segura de un minimercado familiar. El Release 4 incorpora ventas rápidas, cálculo de importes en backend, idempotencia persistente, historial paginado y resumen de ventas, además del catálogo y la seguridad JWT de releases anteriores.

## Stack

- Java 25
- Spring Boot 4.1.1
- Maven 3.6.3 o superior
- Spring MVC y Jakarta Validation
- Spring Data JPA y PostgreSQL
- Spring Security, OAuth2 Resource Server y BCrypt
- Flyway
- springdoc-openapi 3.1.1 y Swagger UI
- JUnit 5, Mockito, AssertJ y MockMvc

## Arquitectura

El proyecto aplica arquitectura hexagonal:

- `domain`: modelos, excepciones y puertos sin dependencias de Spring, JPA o HTTP.
- `application`: servicios que implementan casos de uso y validaciones de negocio.
- `infrastructure/adapter/in/rest`: controladores, DTOs y mappers REST.
- `infrastructure/adapter/out/persistence`: entidades JPA, repositorios, mappers y adaptadores de persistencia.
- `infrastructure/exception`: traducción global de excepciones al contrato HTTP.
- `infrastructure/config`: configuración técnica y ensamblado de dependencias.
- `infrastructure/security`: JWT, BCrypt y respuestas producidas por la cadena de seguridad.

Las entidades JPA no se exponen en la API y los DTOs REST no atraviesan los puertos de entrada.

## Funcionalidad disponible

### Categorías

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/categories` | Registra una categoría |
| `GET` | `/api/v1/categories` | Lista categorías activas |

### Productos

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/products` | Registra un producto |
| `GET` | `/api/v1/products` | Lista productos activos con paginación |
| `GET` | `/api/v1/products/{id}` | Consulta un producto activo |
| `GET` | `/api/v1/products/search?name={name}` | Busca parcialmente por nombre |
| `GET` | `/api/v1/products/barcode/{barcode}` | Busca exactamente por código de barras |
| `PUT` | `/api/v1/products/{id}` | Actualiza un producto |
| `PATCH` | `/api/v1/products/{id}/price` | Actualiza únicamente el precio |
| `DELETE` | `/api/v1/products/{id}` | Desactiva lógicamente el producto |

### Sistema

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/system/info` | Comprueba la disponibilidad técnica sin consultar PostgreSQL |

### Autenticación y sesiones

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Público | Autentica y entrega access/refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Refresh token | Rota el refresh token y renueva la sesión |
| `POST` | `/api/v1/auth/logout` | Autenticado | Revoca el refresh token de la sesión propia |
| `GET` | `/api/v1/auth/me` | Autenticado | Devuelve el usuario autenticado sin datos sensibles |

### Administración de usuarios

Todos estos endpoints requieren `ADMIN`:

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/users` | Crea un usuario |
| `GET` | `/api/v1/users` | Lista usuarios con paginación |
| `GET` | `/api/v1/users/{id}` | Consulta un usuario |
| `PATCH` | `/api/v1/users/{id}/status` | Activa o desactiva |
| `PATCH` | `/api/v1/users/{id}/role` | Cambia su rol |
| `POST` | `/api/v1/users/{id}/reset-password` | Establece una contraseña nueva |

Los listados usan `page=0` y `size=20` por defecto. El tamaño máximo es 100.

### Ventas rápidas

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/v1/sales/preview` | ADMIN, VENDEDOR | Calcula productos, subtotales y total sin guardar |
| `POST` | `/api/v1/sales` | ADMIN, VENDEDOR | Confirma una venta; requiere `Idempotency-Key` |
| `GET` | `/api/v1/sales/{id}` | ADMIN o propietario | Consulta el detalle y los precios históricos |
| `GET` | `/api/v1/sales` | ADMIN, VENDEDOR | Historial paginado; VENDEDOR sólo ve sus ventas |
| `GET` | `/api/v1/sales/summary` | ADMIN | Cuenta y suma ventas completadas |

El historial acepta `from`, `to`, `userId`, `status`, `page` y `size`. `from` y `to` usan `YYYY-MM-DD`, son inclusivos y se interpretan en `America/Lima`. El listado no carga detalles; éstos se obtienen mediante `/api/v1/sales/{id}`.

## Flujo y reglas de ventas

El frontend puede mantener un carrito temporal local, pero no envía precios, subtotales, total, rol ni usuario responsable. En preview y confirmación, el backend obtiene todos los productos en una consulta por lote y usa los precios vigentes de PostgreSQL.

- `UND` y `PACK` admiten cantidades enteras positivas.
- `KG`, `GR`, `LT` y `ML` admiten hasta tres decimales positivos.
- Precio, subtotal y total usan `BigDecimal`, dos decimales y `RoundingMode.HALF_UP`.
- La moneda actual es PEN.
- No se aceptan ventas vacías, productos repetidos, inexistentes o inactivos.
- Confirmar vuelve a calcular todo; una preview no reserva ni garantiza precios.
- La venta guarda una copia del nombre y precio unitario, por lo que cambios posteriores del catálogo no modifican el historial.
- Este release no modifica stock y no implementa pagos, descuentos, cancelaciones ni facturación electrónica.

Ejemplo de preview o confirmación:

```json
{
  "data": {
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 5, "quantity": 0.500 }
    ]
  }
}
```

Para confirmar, enviar además un header estable por intento lógico:

```text
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

La clave es única por usuario. Un reintento con el mismo contenido devuelve la venta original; reutilizarla con un contenido diferente devuelve 409. La reserva de clave, la cabecera y los detalles se escriben en una sola transacción. Dos requests concurrentes equivalentes producen una sola venta. Una operación fallida hace rollback completo y permite reintentar.

## Contratos JSON

Los endpoints `POST`, `PUT` y `PATCH` reciben un envoltorio obligatorio:

```json
{
  "data": {
    "name": "Leche Gloria",
    "barcode": "0012345678905",
    "price": 4.50,
    "purchasePrice": 3.00,
    "categoryId": 1,
    "unitOfMeasure": "UND"
  }
}
```

Las respuestas con cuerpo siguen este formato:

```json
{
  "statusCode": 200,
  "message": "Producto encontrado correctamente",
  "data": {}
}
```

Los errores utilizan el mismo envoltorio e incluyen un código estable, fecha, ruta, correlation ID cuando esté disponible y errores de campo cuando corresponda. Se utilizan 400, 404, 409, 405, 422 y 500 según el caso. HTTP 204 no devuelve cuerpo.

Los errores de Spring Security también respetan el contrato: una solicitud sin autenticación válida devuelve `401/UNAUTHORIZED`; un usuario autenticado sin el rol requerido devuelve `403/FORBIDDEN`.

## Arquitectura de autenticación

El access token es un JWT HS256 validado por Spring Security OAuth2 Resource Server. Su duración predeterminada es 15 minutos e incluye `sub`, `iss`, `iat`, `exp`, `jti`, `user_id`, `role` y `token_type=access`. La API valida firma, expiración, issuer, tipo y claims obligatorios; no acepta el algoritmo indicado arbitrariamente por el cliente.

El refresh token es un valor aleatorio de 256 bits, no un JWT. El cliente recibe el valor original una sola vez y PostgreSQL almacena exclusivamente su SHA-256. Cada renovación:

1. Bloquea el token anterior en PostgreSQL.
2. Verifica expiración, revocación y usuario activo.
3. Revoca el token utilizado.
4. Emite un nuevo par de tokens dentro de la misma familia.

La reutilización de un token rotado revoca toda la familia. Dos solicitudes concurrentes no pueden renovar exitosamente el mismo token.

El logout revoca el refresh token indicado y comprueba que pertenezca al usuario del JWT. Un access token ya emitido continúa siendo válido hasta expirar; por tanto, desactivar, cambiar rol, restablecer contraseña o cerrar sesión tiene una ventana máxima predeterminada de 15 minutos para access tokens. Esas operaciones sí revocan los refresh tokens y evitan renovar la sesión.

## Roles y permisos

| Operación | ADMIN | VENDEDOR |
|---|---:|---:|
| Login y refresh | Público | Público |
| Logout y usuario autenticado | Sí | Sí |
| Consultar productos y categorías | Sí | Sí |
| Crear o modificar productos/categorías | Sí | No |
| Administrar usuarios | Sí | No |
| Información técnica | Sí | No |
| Previsualizar y registrar ventas | Sí | Sí |
| Consultar ventas propias | Sí | Sí |
| Consultar ventas de otros usuarios | Sí | No |
| Consultar historial global | Sí | No |
| Consultar resumen global | Sí | No |

La configuración es deny-by-default: toda ruta nueva requiere autenticación salvo que se declare pública expresamente. No se confía en roles enviados en requests; las autoridades proceden exclusivamente del JWT validado.

## Persistencia

Las migraciones crean y evolucionan el catálogo:

- `categories`, con nombre único normalizado sin distinguir mayúsculas.
- `products`, con precio de venta, precio de compra opcional, clave foránea de categoría y auditoría.
- Índice único parcial para códigos de barras no nulos.
- Índices de nombre, estado y categoría.
- Restricciones para precios positivos, nombres no vacíos y unidades válidas.
- `users`, con username único normalizado, hash de contraseña, rol y estado.
- `refresh_tokens`, con hash único, familia, expiración y revocación.
- `login_attempts`, con limitación persistente por usuario y origen.
- `sales`, con número único, usuario responsable, estado, total y auditoría.
- `sale_items`, con producto, nombre y precio históricos, cantidad y subtotal.
- `sale_idempotency_keys`, con clave por usuario, hash canónico del request y venta resultante.
- `sale_number_seq`, secuencia concurrente que genera números como `V-00000001`; puede tener saltos tras rollbacks sin perder unicidad.

`V2__add_product_purchase_price.sql` incorpora `purchase_price NUMERIC(12,2)`. Cuando hay precio de compra, la API calcula en cada respuesta `grossMarginPercentage` (margen sobre venta) y `markupPercentage` (recargo sobre costo); esos porcentajes no se almacenan para evitar inconsistencias.

Un producto se desactiva mediante el campo `active`; no se elimina físicamente.

## OpenAPI y Swagger UI

`src/main/resources/swagger.yml` es el contrato principal y Swagger UI está configurado explícitamente para cargar `/swagger.yml`. Springdoc mantiene disponible su endpoint técnico generado, pero la interfaz no lo utiliza como fuente principal.

Una vez iniciada la aplicación, Swagger UI estará disponible en:

```text
http://localhost:8080/swagger-ui.html
```

En `local` y `test`, Swagger es público. En producción queda protegido. Para probar rutas protegidas desde Swagger:

1. Ejecutar `/api/v1/auth/login`.
2. Copiar `accessToken` de la respuesta.
3. Pulsar **Authorize** e ingresar el token. Swagger aplica el esquema Bearer JWT.

Login y refresh aparecen expresamente como operaciones públicas en el contrato.

Para probar una confirmación en Swagger, autorizarse con un access token, completar el body y asignar un valor nuevo al parámetro de header `Idempotency-Key`.

## Requisitos

- JDK 25
- Maven 3.6.3+
- PostgreSQL 17 para ejecutar la aplicación
- Docker Desktop únicamente si se utiliza PostgreSQL o Testcontainers mediante Docker

## Configuración local

1. Copiar `.env.example` como `.env` y reemplazar los valores de ejemplo. El repositorio ya ignora `.env`.
2. Iniciar PostgreSQL 17:

```bash
docker compose up -d postgres
docker compose ps
```

3. Exportar `DB_USERNAME` y `DB_PASSWORD` desde `.env` en el entorno del IDE o la terminal. En PowerShell puede cargarse el archivo así:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}
```

4. Ejecutar la aplicación:

```bash
mvn spring-boot:run
```

Flyway aplica automáticamente las migraciones y Hibernate valida que las entidades coincidan con el esquema. No se utiliza `ddl-auto=update` ni `create-drop`.

### Variables de seguridad

| Variable | Predeterminado | Uso |
|---|---|---|
| `JWT_ISSUER` | `minimarket-api` | Emisor exigido en access tokens |
| `JWT_SECRET_BASE64` | Solo existe valor de desarrollo en `local` | Secreto HS256 Base64 de al menos 32 bytes |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `PT15M` | Vigencia del access token |
| `REFRESH_TOKEN_EXPIRATION` | `P30D` | Vigencia del refresh token |
| `LOGIN_MAX_ATTEMPTS` | `5` | Fallos permitidos por ventana |
| `LOGIN_ATTEMPT_WINDOW` | `PT15M` | Ventana de intentos |
| `LOGIN_BLOCK_DURATION` | `PT15M` | Duración del bloqueo |
| `BOOTSTRAP_ADMIN_USERNAME` | Vacío | Activa el bootstrap inicial |
| `BOOTSTRAP_ADMIN_PASSWORD` | Vacío | Contraseña inicial, nunca se registra |
| `BOOTSTRAP_ADMIN_FULL_NAME` | `Administrador inicial` | Nombre del primer ADMIN |

En producción `JWT_SECRET_BASE64` es obligatorio y no tiene valor predeterminado. Puede generarse, por ejemplo, con un gestor de secretos o con 32 bytes criptográficamente aleatorios codificados en Base64. No se deben guardar secretos reales en `.env.example` ni en el repositorio.

### Bootstrap del primer administrador

No existe endpoint de registro público. Para crear el primer ADMIN, definir las tres variables `BOOTSTRAP_ADMIN_*` antes del primer inicio. La contraseña debe tener entre 12 y 128 caracteres e incluir mayúscula, minúscula, número y símbolo.

El proceso toma un bloqueo transaccional PostgreSQL, comprueba que no exista ningún ADMIN y almacena solo el hash BCrypt. En reinicios posteriores no vuelve a crear usuarios. Después del bootstrap se recomienda retirar las credenciales del entorno y crear el resto de familiares mediante `/api/v1/users`.

Ejemplo de login:

```json
{
  "data": {
    "username": "admin",
    "password": "UnaPasswordSegura123!"
  }
}
```

Ejemplo de renovación:

```json
{
  "data": {
    "refreshToken": "valor_recibido_en_login"
  }
}
```

## Compilación y pruebas

```bash
mvn clean verify
```

La suite combina pruebas unitarias, MVC aisladas y una integración con PostgreSQL 17 mediante Testcontainers. Además del catálogo y la seguridad cubre preview, importes y redondeo, cantidades por peso, productos inválidos, precios históricos, rollback, numeración, confirmaciones concurrentes, reintentos idempotentes, conflicto por clave reutilizada, propiedad de ventas, historial paginado, filtros de fecha, resumen SQL, exclusión de ventas canceladas, Flyway y OpenAPI.

Docker Desktop debe estar iniciado para ejecutar la prueba de integración; si Docker no está disponible, Testcontainers la omite explícitamente.

## Logging y trazabilidad

- El perfil local produce logs legibles en consola.
- El perfil `prod` utiliza JSON Logstash mediante el soporte nativo de Spring Boot.
- `X-Correlation-ID` se valida, se incorpora al MDC y se devuelve en la respuesta.
- Cuando el header falta o contiene caracteres inseguros se genera un UUID.
- El MDC se limpia al finalizar cada solicitud.
- No se registran cuerpos completos, contraseñas ni tokens.

## Imagen Docker

```bash
docker build -t minimarket-api:local .
```

La imagen usa compilación multi-stage con Java 25 y ejecuta la aplicación como usuario no root. Para ejecutarla en la red creada por Compose:

```bash
docker run --rm --name minimarket-api \
  --network minimarket-api_default \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://minimarket-postgres:5432/minimarket_db \
  -e DB_USERNAME=minimarket_user \
  -e DB_PASSWORD=your_password \
  -e JWT_ISSUER=minimarket-api \
  -e JWT_SECRET_BASE64=your_base64_secret \
  minimarket-api:local
```

## Perfiles

- `local`: PostgreSQL local, Swagger público, secreto JWT exclusivo de desarrollo, SQL de diagnóstico y logs legibles.
- `prod`: secretos obligatorios mediante entorno/gestor de secretos, HTTPS obligatorio, Swagger protegido, logs JSON y pool configurable.
- `test`: usado por la integración aislada con Testcontainers.

Actuator expone únicamente `health` e `info`: health es público e info requiere ADMIN. `/api/v1/system/info` también requiere ADMIN.

## Consideraciones para React Native

- Enviar el access token mediante `Authorization: Bearer <token>`; nunca incluir tokens en URLs.
- Renovar la sesión usando siempre el refresh token más reciente y reemplazar atómicamente el anterior.
- Si refresh devuelve 401, eliminar la sesión local y solicitar credenciales nuevamente.
- Guardar tokens con mecanismos seguros del sistema operativo, como iOS Keychain o Android Keystore mediante una biblioteca compatible.
- No almacenar refresh tokens en `AsyncStorage`.
- No registrar passwords, tokens ni el header `Authorization` en logs del cliente.
- El carrito puede mantenerse temporalmente en el dispositivo, pero los importes mostrados deben refrescarse mediante `/sales/preview`.
- Generar una clave idempotente al iniciar cada confirmación y conservarla durante todos los reintentos de esa misma venta.
- No generar una clave nueva por timeout: podría duplicar la venta si el primer request sí llegó al servidor.
- Si se modifica el carrito después de una confirmación fallida, generar otra clave; el backend rechaza una clave reutilizada con contenido diferente.
- Considerar el response de `POST /sales` como importe definitivo, aunque difiera de una preview anterior por un cambio de precio.

## Limitaciones y despliegue

- No hay revocación inmediata de access tokens; su ventana máxima es `JWT_ACCESS_TOKEN_EXPIRATION`.
- El rate limiting usa PostgreSQL y funciona con varias instancias. Antes de un despliegue de gran escala conviene evaluar una capa perimetral especializada y una política de limpieza de intentos antiguos.
- HTTPS se fuerza en `prod`; si existe un proxy inverso debe conservar correctamente los headers `Forwarded` o `X-Forwarded-*`.
- CORS no se abre globalmente: React Native nativo no lo necesita y Swagger opera en el mismo origen. Si se incorpora un frontend web, deben configurarse orígenes explícitos.
