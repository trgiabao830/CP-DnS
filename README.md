# 🏨 Dine & Stay (DnS) - Backend API

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)

The robust backend infrastructure for the **Dine & Stay Booking Management System**, supporting both Homestay reservations and Restaurant table bookings. Built with Java Spring Boot, this API leverages a message broker for reliable payments and real-time protocols for seamless communication.

## ✨ Key Features
*   **Robust RESTful APIs:** Manages complex business logic for homestay and restaurant operations, supporting both Customer and Admin portals.
*   **Asynchronous Payment Processing:** Integrates **VNPay API** and utilizes **RabbitMQ** as a message broker to handle payment queues securely and reliably.
*   **Real-time Communication:**
    *   **WebSocket:** Powers bi-directional live chat between customers and staff.
    *   **Server-Sent Events (SSE):** Pushes real-time updates to the Admin monitoring dashboard for instant restaurant table status synchronization.
*   **Cloud Media Management:** Uses **Cloudinary** for efficient and optimized image storage.
*   **Secure Authentication & Authorization:** Role-based access control (RBAC) separating Admin and Customer privileges.

## 🛠️ Technology Stack
*   **Framework:** Java Spring Boot
*   **Database:** PostgreSQL
*   **Message Broker:** RabbitMQ
*   **Real-time Protocols:** WebSocket, SSE
*   **Third-party Integrations:** VNPay API, Cloudinary, AI Chatbot Provider

## 🚀 Getting Started

### Prerequisites
*   JDK 17 or higher
*   PostgreSQL
*   RabbitMQ server (running locally or via Docker)
*   Maven
