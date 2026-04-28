# Spring Boot Hibernate Bidirectional Many-to-One Relationship Mapping

[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A comprehensive Spring Boot application demonstrating Hibernate bidirectional many-to-one relationship mapping with a complete customer order management system.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## ✨ Features

- **Bidirectional Relationship Mapping**: Demonstrates Hibernate many-to-one relationships between Customer, CustomerOrder, OrderItem, and ShippingAddress entities
- **RESTful API**: Complete CRUD operations for all entities
- **Swagger UI**: Interactive API documentation
- **H2 Database**: In-memory database with console access
- **Spring Boot Actuator**: Health checks and application monitoring
- **Comprehensive Testing**: Unit tests, integration tests, and Layer 2 smoke tests
- **Containerized Testing**: Docker-based smoke tests for production-like validation

## 🏗️ Architecture

### Entity Relationships

```
Customer (1) ────→ (Many) CustomerOrder
Customer (1) ────→ (1) ShippingAddress
CustomerOrder (1) ────→ (Many) OrderItem
```

**Key Points:**
- Many OrderItems are related to one parent CustomerOrder entity
- Bidirectional navigation between entities
- Cascade operations for data persistence
- Lazy/Eager loading configurations

### ER Diagram

![Entity Relationship Diagram](doc/many_to_one_er_diagram.png)

## 🛠️ Tech Stack

- **Java**: 25
- **Spring Boot**: 4.0.6
- **Spring Data JPA**: Hibernate implementation
- **Database**: H2 (In-memory)
- **Build Tool**: Maven 3.9.11
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Testing**: JUnit 5, Spring Boot Test
- **Monitoring**: Spring Boot Actuator
- **Logging**: Logback
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- **Java**: JDK 25 or higher
- **Maven**: 3.9.11 or higher
- **Docker**: For running smoke tests (optional)
- **Git**: For version control

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/tufangorel/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping.git
   cd spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping
   ```

2. **Build the application**
   ```bash
   ./mvnw clean compile
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

The application will start on `http://localhost:8080/customer-info`

## 📖 Usage

### Application Profiles

- **dev**: Development profile with detailed logging
- **test**: Testing profile with test-specific configurations

### Accessing the Application

- **Swagger UI**: http://localhost:8080/customer-info/swagger-ui/
- **H2 Console**: http://localhost:8080/customer-info/h2/
- **Health Check**: http://localhost:8080/customer-info/actuator/health

### H2 Database Configuration

- **URL**: `jdbc:h2:mem:cust`
- **Username**: `sa`
- **Password**: `123456`
- **Driver**: `org.h2.Driver`

## 📚 API Documentation

### Customer Management

#### Create Customer
```bash
POST /customer-info/customer/save
Content-Type: application/json

{
    "name": "John Doe",
    "age": 30,
    "shippingAddress": {
        "streetName": "123 Main St",
        "city": "Istanbul",
        "country": "TR"
    }
}
```

#### List Customers
```bash
GET /customer-info/customer/list
```

#### Update Customer
```bash
PUT /customer-info/customer/update/{id}
```

#### Delete Customer
```bash
DELETE /customer-info/customer/delete/{id}
```

### Order Management

#### Create Customer Order with Items
```bash
POST /customer-info/customerorder/save
Content-Type: application/json

{
  "customer": {
    "id": 1,
    "name": "John Doe",
    "age": 30,
    "shippingAddress": {
      "id": 1,
      "streetName": "123 Main St",
      "city": "Istanbul",
      "country": "TR"
    }
  },
  "orderDate": "2026-04-28T16:00:00",
  "title": "Spring Order",
  "orderItems": [
    {
      "quantity": 2
    },
    {
      "quantity": 1
    }
  ]
}
```

#### List Customer Orders
```bash
GET /customer-info/customerorder/list
```

#### Update Customer Order
```bash
PUT /customer-info/customerorder/update/{id}
```

#### Delete Customer Order
```bash
DELETE /customer-info/customerorder/delete/{id}
```

### Order Item Management

#### List Order Items
```bash
GET /customer-info/orderitem/list
```

#### Create Order Item
```bash
POST /customer-info/orderitem/save
Content-Type: application/json

{
    "quantity": 5
}
```

#### Update Order Item
```bash
PUT /customer-info/orderitem/update/{id}
```

#### Delete Order Item
```bash
DELETE /customer-info/orderitem/delete/{id}
```

## 🧪 Testing

### Unit Tests & Integration Tests

Run all tests:
```bash
./mvnw test
```

Run with coverage:
```bash
./mvnw test jacoco:report
```

### Layer 2 Smoke Tests

The project includes comprehensive Layer 2 smoke tests that validate the application in a containerized environment.

#### Prerequisites for Smoke Tests
- Docker installed and running
- Docker Compose available

#### Running Smoke Tests

**Unix/Linux/Mac:**
```bash
bash .github/integration-tests/run-layer2-tests.sh
```

**Windows:**
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .github/integration-tests/run-layer2-tests.ps1
```

#### What Smoke Tests Validate
- ✅ Application startup and health endpoint
- ✅ Customer creation and persistence
- ✅ Customer order creation with multiple items
- ✅ Bidirectional relationship integrity
- ✅ Database operations in containerized environment

### Test Results
```
✅ Layer 2 smoke tests PASSED
========================================
```

## 📁 Project Structure

```
spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping/
├── .github/
│   └── integration-tests/          # Layer 2 smoke tests
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/company/customerinfo/
│   │   │       ├── config/         # Configuration classes
│   │   │       ├── controller/     # REST controllers
│   │   │       ├── model/          # JPA entities
│   │   │       ├── repository/     # Data repositories
│   │   │       ├── service/        # Business logic
│   │   │       └── CustomerInfoApplication.java
│   │   └── resources/              # Application properties
│   └── test/                       # Unit and integration tests
├── doc/                            # Documentation and diagrams
├── pom.xml                         # Maven configuration
├── mvnw & mvnw.cmd                 # Maven wrapper
└── README.md                       # This file
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Boot best practices
- Write comprehensive unit tests
- Update documentation for API changes
- Ensure all tests pass before submitting PR
- Use meaningful commit messages

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋 Support

If you have any questions or issues:

1. Check the [Issues](https://github.com/tufangorel/spring-boot-hibernate-bidirectional-many-to-one-relationship-mapping/issues) page
2. Review the API documentation in Swagger UI
3. Check the Layer 2 smoke test results for common issues

---

**Happy Coding! 🚀**
