# ExamenMicroservicios — productos-service

Microservicio de productos registrado en Eureka, con persistencia en MongoDB, construido con **Spring Boot 3.3.5** y **Spring Cloud 2024.0.0**.

| Detalle | Valor |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2024.0.0 |
| Puerto | 8081 |
| Base de datos | MongoDB (`productosdb`) |
| Eureka | `http://eureka-server:8761/eureka` |

---

## Estructura del proyecto

```
src/
└── main/
    ├── java/com/examen/productosservice/
    │   ├── ProductosServiceApplication.java
    │   ├── controller/ProductoController.java
    │   ├── model/Producto.java
    │   └── repository/ProductoRepository.java
    └── resources/
        └── application.yml
Dockerfile
pom.xml
```

---

## Requisitos previos

- Java 21+
- Maven 3.9+
- MongoDB en ejecución (local o Docker)
- Eureka Server en ejecución
- Docker (opcional)

---

## Ejecución local

### 1. Compilar

```bash
mvn clean package
```

### 2. Ejecutar con Maven

```bash
mvn spring-boot:run
```

### 3. Ejecutar el JAR

```bash
java -jar target/productos-service-0.0.1-SNAPSHOT.jar
```

---

## Docker

### Construir la imagen

```bash
docker build -t productos-service:latest .
```

### Ejecutar el contenedor

```bash
docker run -d \
  --name productos-service \
  -p 8081:8081 \
  -e SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/productosdb \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka \
  productos-service:latest
```

---

## API REST

### Ping / Health

```bash
# Directo
curl http://localhost:8081/productos/ping

# Vía API Gateway
curl http://localhost:8080/productos/ping
```

Respuesta esperada:
```json
{ "service": "productos-service", "ok": true }
```

---

### Crear producto

```bash
# Directo
curl -X POST http://localhost:8081/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Laptop", "precio": 999.99}'

# Vía API Gateway
curl -X POST http://localhost:8080/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Laptop", "precio": 999.99}'
```

---

### Listar productos

```bash
# Directo
curl http://localhost:8081/productos

# Vía API Gateway
curl http://localhost:8080/productos
```

---

### Obtener producto por ID

```bash
# Directo
curl http://localhost:8081/productos/{id}

# Vía API Gateway
curl http://localhost:8080/productos/{id}
```

---

### Eliminar producto

```bash
# Directo
curl -X DELETE http://localhost:8081/productos/{id}

# Vía API Gateway
curl -X DELETE http://localhost:8080/productos/{id}
```

---

## Actuator

| Endpoint | URL |
|---|---|
| Health | http://localhost:8081/actuator/health |
| Info | http://localhost:8081/actuator/info |
