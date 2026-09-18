# Autogestión del cliente — backend

Implementa el flujo tokenizado y el cambio de ingreso por DNI, panel del cliente y reservas pendientes para clientes nuevos.

## Integración de autenticación profesional pendiente

El proyecto no tiene un proveedor de autenticación para la API profesional existente.
Los nuevos endpoints de gestión del enlace requieren un principal autenticado cuyo
nombre sea el ID del profesional y el rol servlet `PROFESIONAL`. Sin ese principal
responden 401; con otro ID o sin el rol responden 403. Las cabeceras `X-Usuario`
o los IDs enviados por el cliente no autentican al profesional.

Antes de exponer el backend públicamente, debe integrarse la autenticación y
autorización de **todos** los endpoints `/api/profesionales/**`, o mantenerlos en
una red privada detrás de un gateway autenticado. El aislamiento del nuevo flujo
público no protege por sí mismo las APIs administrativas anteriores. No debe
considerarse completo el aislamiento de toda la aplicación hasta resolver esto.

## Enlace y sesión

### Autenticación simulada para desarrollo local

El perfil Spring `dev` permite gestionar el enlace sin autenticación ni encabezados
especiales. Usa el ID de la ruta y comprueba que el profesional exista. El portal
público conserva sus tokens y el aislamiento por profesional. Con los perfiles
`prod` o `production`, o sin `dev`, este mecanismo no se registra.

Para los contenedores locales, ejecutar:

```sh
docker compose -f compose.yaml -f compose.dev.yaml up -d --build
```

También se puede ejecutar `docker compose up -d --build`: el archivo
`compose.override.yaml` activa automáticamente el mismo modo de prueba local.
Un comando con `-f compose.yaml` exclusivamente omite el override y exige
autenticación profesional. Al recrear los servicios, conservar el override
para evitar que el panel vuelva a responder 401.

Este override habilita el perfil `dev`. El frontend permite acceso por localhost y por la IP de la red local; `FRONTEND_HOST=127.0.0.1` permite restringirlo a localhost. El backend sigue expuesto directamente solamente en localhost y el frontend accede a él mediante su proxy `/api`. Requiere configurar
`AUTOGESTION_ENLACE_CLAVE` en `.env` (32 bytes aleatorios en Base64).
El compose base mantiene deshabilitada la autenticación simulada. Para ejecutar
el backend sin Docker, usar `SPRING_PROFILES_ACTIVE=dev`.

- `POST /api/profesionales/{id}/enlace-autogestion`: genera un token aleatorio
  de 256 bits. Revoca el anterior y todas las sesiones que dependían de él.
  Devuelve `{token, vence: null}`. El enlace es permanente hasta su revocación.
- `DELETE /api/profesionales/{id}/enlace-autogestion`: revoca el enlace.
- `POST /api/autogestion/sesiones`, cuerpo `{token}`: intercambia el token
  del enlace por una sesión de 30 minutos, vinculada al profesional.

El profesional autenticado puede recuperar el enlace con
`GET /api/profesionales/{id}/enlace-autogestion`, que devuelve
`{activo, recuperable, token}`. La base almacena la huella SHA-256 y, desde V11,
el token cifrado con AES-256-GCM. Debe configurarse `AUTOGESTION_ENLACE_CLAVE`
con 32 bytes aleatorios codificados en Base64, conservados en el gestor de
secretos del despliegue y compartidos por todas las instancias. Sin una clave
válida, generar o recuperar enlaces devuelve 503; no se usa una clave pública
predeterminada. No reemplazar esa clave sin migrar los tokens cifrados.
Los enlaces anteriores a V11 siguen funcionando, pero deben regenerarse para
poder recuperarlos desde el panel.

El frontend comparte una URL con el token en
el fragmento, por ejemplo `/reservar#TOKEN`, y enviarlo en el cuerpo al abrir
la sesión. El fragmento evita incluir la credencial en la URL de las solicitudes.
La ruta pública es `/reservar`; la gestión profesional está en
`/profesional/autogestion`, con acceso desde la barra lateral y una tarjeta en Mi día.

Las solicitudes siguientes usan `Authorization: Bearer TOKEN_DE_SESION`.
`GET /api/autogestion/sesion` devuelve los datos públicos del profesional
(nombre, apellido, especialidad, teléfono y zona horaria), la identidad ya
validada del cliente y el resultado de una confirmación previa. Permite
reanudar el flujo sin reenviar DNI ni duplicar una reserva.
El token del enlace no sirve como sesión. La sesión no permite cambiar de
profesional ni de identidad de cliente; para otra identidad debe abrirse otra.

## Identificación y datos

1. `POST /api/autogestion/cliente/identificacion`, cuerpo `{dni}`.
   Busca exclusivamente por profesional de la sesión y DNI. Si existe, devuelve
   sus datos y permite abrir el panel. Si no existe devuelve estado `NUEVO`.
2. Para un cliente nuevo: `POST /api/autogestion/cliente`, cuerpo
   `{nombre, apellido, email, telefono}`. El DNI proviene de la identificación.
   Crea un cliente `PENDIENTE_DE_VERIFICACION` que puede solicitar turnos
   `PENDIENTE_DE_APROBACION`. Verificar al cliente y aprobar el turno son
   acciones independientes.
3. `GET /api/autogestion/turnos`: lista todos los turnos del cliente identificado
   con el profesional del enlace, con estado actual, horarios y tipo de atención.
   No acepta IDs del cliente o profesional.
4. Para actualizar contacto: `PUT /api/autogestion/cliente/contacto`, cuerpo
   `{email, telefono}`. Requiere identificación previa y no modifica identidad.

El ingreso se realiza solamente por DNI, según el nuevo flujo autorizado.
Pueden reservar `HABILITADO`, `REQUIERE_APROBACION` y
`PENDIENTE_DE_VERIFICACION`. Los demás estados reciben 403 al reservar,
aunque pueden consultar sus turnos. Una agenda que impide autogestión rechaza
las sesiones ya abiertas.

## Disponibilidad y confirmación

`GET /api/autogestion/dias?mes=AAAA-MM` devuelve los días en estado actual
`ACTIVO` del profesional de la sesión, exclusivamente para ese mes. Sin `mes`,
usa el primer mes activo desde el mes actual en la zona horaria configurada. Incluye ID, fecha, estado y
seleccionabilidad; los días pasados no se pueden elegir. No filtra por tipo ni
por intervalos: la consulta de brechas ocurre después de seleccionar un día.
`GET /api/autogestion/meses` devuelve únicamente meses en estado `ACTIVO`
del profesional, desde el mes actual en adelante. La navegación recorre solo
esa lista; si el mes actual está inactivo inicia en el primer mes activo.
Consultar un mes inactivo explícitamente devuelve 409; elegir o confirmar un
intervalo de un mes desactivado también se rechaza.

1. `GET /api/autogestion/configuracion`: duración y capacidad simultánea generales del profesional.
2. `GET /api/autogestion/fechas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`:
   devuelve fechas con intervalos disponibles. Rango máximo de 90 días,
   sin fechas anteriores al día actual.
3. `GET /api/autogestion/brechas?fecha=YYYY-MM-DD`:
   devuelve brechas efectivas con sus intervalos, considerando estados,
   excepciones, duración y capacidad. No ofrece horarios que ya comenzaron.
4. `POST /api/autogestion/intervalos`, cuerpo `{fecha, horaInicio}`:
   valida que exista exactamente ese intervalo y devuelve `{token, vence}`.
   La referencia vence en 5 minutos y queda vinculada a sesión, cliente,
   profesional, día, inicio y fin; los nuevos turnos no tienen tipo de atención. No retiene el cupo.
5. `POST /api/autogestion/turnos`, cabecera `Idempotency-Key`, cuerpo
   `{referencia: TOKEN_DEL_INTERVALO, observaciones}`: revalida identidad,
   estado del cliente, configuración general, día, intervalo efectivo, horario futuro,
   duración y capacidad antes de crear el turno.

La creación y el registro de idempotencia son una única transacción. Se bloquean
sesión, enlace y día de agenda para serializar confirmaciones concurrentes.
Una segunda reserva del último cupo recibe 409. Se usa la capacidad general del
profesional y el mismo verificador que al crear turnos sin tipo de atención:
`ASIGNADO`, `PENDIENTE_DE_APROBACION`, `CONFIRMADO`, `REPROGRAMADO` y
`AFECTADO_POR_EXCEPCION` ocupan cupo. El cronograma público nunca muestra esos
turnos ni datos de otros clientes. Las referencias de otras sesiones se rechazan.
La duración y capacidad se revalidan al confirmar. Las referencias antiguas con
tipo de atención deben seleccionarse nuevamente. Se requiere configuración general válida.

La API anterior `/api/autogestion/profesionales/{id}/**` devuelve 410.

## Límites y operación

- 60 solicitudes por minuto por IP y 120 por credencial de sesión.
- 5 intentos de identificación por minuto por sesión y por combinación
  profesional/DNI. Los intentos fallidos se contabilizan en transacciones independientes.
- Contadores persistidos en PostgreSQL, compartidos por todas las instancias.
- Los contadores antiguos se eliminan; las ventanas son de un minuto.
- Se usa la IP remota de la conexión. No se confía directamente en
  `X-Forwarded-For`. Si se usa un proxy, configurar la resolución de IP exclusivamente
  para proxies confiables; de lo contrario sus usuarios comparten el límite del proxy.
- Respuestas públicas y generación de enlaces: `Cache-Control: no-store`.
- Registro, cambios de contacto, gestión del enlace e identificación se auditan.
  La creación del turno mantiene la auditoría y las notificaciones existentes.
- Usar HTTPS. No registrar cuerpos que contienen tokens ni cabeceras Authorization.
- Las sesiones y referencias vencidas no se aceptan. La retención y limpieza de
  esos registros y confirmaciones debe ajustarse a la política de datos del despliegue.

La migración `V10` incorpora las tablas sin cambiar el modelo de clientes o turnos.
Las pruebas de integración usan PostgreSQL real mediante Testcontainers e incluyen
estados, aislamiento, revocación, vencimiento, revalidación, concurrencia,
idempotencia, contrato HTTP y límites de solicitudes.

Validación: `mvn "-Dtest=AutogestionIntegrationTest" test`.

## Frontend

El portal solicita solo DNI. Los clientes existentes abren un panel de bienvenida con dos botones grandes:
“Consultar mis turnos” y “Registrar un nuevo turno”. El listado de turnos se
consulta únicamente al abrir esa sección, que permite volver al inicio. Los clientes nuevos ven la opción
“Registrarse como cliente de [profesional]”, completan sus datos y continúan
al calendario, sin esperar la verificación.

La reserva reutiliza el calendario de “Mi Mes”, inicialmente en el mes actual,
y el cronograma de “Mi Día” mostrando únicamente brechas e intervalos para
seleccionar. Se pueden elegir los días activos no pasados del mes consultado y luego los intervalos disponibles. El cliente no selecciona tipos de atención: los intervalos se calculan con la duración y capacidad generales del profesional. La confirmación distingue turno asignado y solicitud
pendiente de aprobación. Desde el resultado puede volver al panel y reservar
otro turno.

La sesión, el enlace y la selección se conservan en `sessionStorage`, dentro de
la pestaña. Los datos identificatorios se recuperan del backend y no se guardan
en almacenamiento permanente. Durante una confirmación incierta, se guarda
temporalmente la clave de idempotencia y el cuerpo exacto del pedido para poder
reintentarlo incluso después de recargar. Las observaciones temporales se limpian
cuando se conoce el resultado. Un enlace diferente descarta el contexto anterior.

El panel recupera el enlace, permite copiarlo, abrir el portal y mostrar su QR.
Generar, regenerar y revocar requieren confirmación. Un error de autenticación
se muestra sin simular acceso ni crear enlaces locales. La autenticación
profesional sigue siendo un requisito de integración pendiente.

Validación frontend: `npm test -- src/test/BookingPage.test.jsx src/test/SelfServicePage.test.jsx src/test/Routing.test.jsx`,
`npm run build` y `npm run typecheck`.

La configuración `permitirMultiplesTurnosPorClienteEnDia` está desactivada por
defecto. Al registrar un turno desde el profesional o autogestión, se bloquea
un segundo turno del mismo cliente en ese día, incluso si el anterior está
pendiente de aprobación. Los turnos cancelados, dados de baja o rechazados no
bloquean una nueva reserva. Activar la opción permite varios turnos y mantiene
la validación de capacidad. La regla se aplica bajo el bloqueo transaccional del día.

El dashboard incorpora “Turnos pendientes de verificación” debajo de “Nuevo turno”
y “Mis clientes” debajo de “Turnos afectados”.
`GET /api/profesionales/{id}/turnos/pendientes-verificacion?page=0&size=20`
lista todos los turnos cuyo estado actual es `PENDIENTE_DE_APROBACION`, sin
restricción de fechas, exclusivamente del profesional indicado.
La cartera usa el endpoint paginado de clientes y sus filtros de nombre,
apellido, DNI y estado. “Verificado (habilitado)” representa `HABILITADO`;
no se añade un estado nuevo ni se modifican clientes o turnos al consultar.

En el panel de pendientes se permite seleccionar turnos entre páginas.
“Seleccionar todos los turnos” obtiene los IDs de todos los pendientes del
profesional (`GET /turnos/pendientes-verificacion/ids`). Las acciones
`POST /turnos/pendientes-verificacion/verificar` y `/baja` aceptan `turnoIds`.
La baja requiere `motivo` de hasta 255 caracteres. Ambas acciones validan
pertenencia y estado actual bajo bloqueo y se aplican en una transacción:
si uno de los seleccionados no es válido, el grupo no se modifica. Verificar
el turno lo pasa a `ASIGNADO`; no cambia el estado del cliente.

La configuración `todosLosTurnosPendientesVerificacion` (predeterminado `false`) fuerza `PENDIENTE_DE_APROBACION` en todo turno nuevo, incluso con cliente habilitado y en registros manuales. No modifica turnos existentes ni habilita reservas para clientes bloqueados. Se conserva la aprobación manual del profesional para pasar a `ASIGNADO`.

El panel de clientes permite cambiar su estado mediante `PUT /api/profesionales/{profesionalId}/clientes/{clienteId}/estado` con `{estado, observacion?}`. El servicio valida los estados del ámbito CLIENTE y la pertenencia al profesional, y audita el cambio. La sección Clientes pendientes de verificación usa la lista paginada filtrada por `PENDIENTE_DE_VERIFICACION`, con contador del total.
La sección de clientes pendientes permite selección múltiple y seleccionar todos mediante GET /api/profesionales/{id}/clientes/pendientes-verificacion/ids. POST /estado recibe clienteIds y estado (HABILITADO, REQUIERE_APROBACION, INHABILITADO, DADO_DE_BAJA); valida pertenencia y estado pendiente con bloqueo de filas y aplica el lote en una transacción.
