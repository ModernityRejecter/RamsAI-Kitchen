# Ramsai Kitchen

Digital restaurant management system built with Spring Boot and PostgreSQL.

## Quick Start Tutorial

### Prerequisites
- Docker and Docker Compose installed on your machine.

### First-Time Running the Project
1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd RamsAI-Kitchen
   ```

2. **Start the application**:
   Use Docker Compose to build and start both the database and the application:
   ```bash
   docker compose up --build
   ```
   The application will be available at [http://localhost:8080](http://localhost:8080).

### Development with Docker Compose Watch
For a faster development loop, this project supports `docker compose watch`. This allows you to see your changes in real-time without manually rebuilding the containers.

1. **Run in watch mode**:
   ```bash
   docker compose watch
   ```

2. **What happens during watch?**:
   - **Static Files**: Any changes in `src/main/resources/static` will be automatically synced to the running container.
   - **Java Code & Dependencies**: Any changes in `src/` or `pom.xml` will trigger an automatic rebuild and restart of the application container, ensuring your backend changes are reflected immediately.

---

## Database Management

This project includes **pgAdmin** to help you manage the PostgreSQL database through a web interface.

### Accessing pgAdmin
1. **Open your browser** and navigate to: [http://localhost:8888](http://localhost:8888)
2. **Login** using the corresponding credentials.
---

## User Stories & Acceptance Criteria

### 1. Menu Access
**As a client**, I want to be able to view the menu online so that I can quickly search for the products I want to order.

### 2. Web Ordering
**As a client**, I want to be able to order directly from a web interface so that I don't have to wait for a waiter during busy times.

### 3. Dietary Requirements
**As a person with allergies**, I want to be able to add notes to my order (e.g., "no peanuts") so that the food is prepared according to my needs.

### 4. Special Offers
**As a client**, I want to see products marked with "Discount" or "Special Offer" directly in the online menu so that I can choose the best value-for-money dishes.

### 5. Kitchen Management
**As a chef**, I want to receive orders instantaneously on a screen so that I can optimize the cooking workflow.
* **Acceptance Criteria:**
    * Orders must be sorted by the order in which they were placed.
    * Customer customization notes must be clearly visible.
    * Orders can be marked as "In Progress" and "Ready" so that service staff or the customer are notified immediately.

### 6. Live Status
**As an impatient person**, I want to receive real-time notifications about my order status (Received / Cooking / Ready to serve) so that I know exactly how much longer I have to wait.

### 7. Feedback System
**As a client**, I want to be able to leave a review and a rating for the dishes I consumed so that I can provide feedback to the restaurant regarding service quality.

### 8. Table Mapping
**As a waiter**, I want to be able to view the status of all tables on a digital map of the restaurant so that I know which tables are free, occupied, or waiting for the bill.
* **Acceptance Criteria:**
    * Tables must be colored differently based on status (e.g., Green - Free, Red - Occupied, Yellow - Bill Requested).
    * The system must allow assigning a customer to a specific table simply by clicking on it.
    * Display the time elapsed since the last order for each occupied table.

### 9. AI Kitchen Assistant
**As an uninspired chef**, I want an AI assistant to help me in the kitchen so that I can save time on daily tasks.
* **Acceptance Criteria:**
    * There must be a real-time communication interface with the assistant.
    * It should be able to help create new recipes based on the most popular products.
    * There must be a conversation history.

### 10. Sales Reporting
**As a manager**, I want access to a report of products purchased by customers.
* **Acceptance Criteria:**
    * The report should contain products separated by category, sorted by their popularity in the restaurant.
    * The report should include the average review rating for each product.
    * The report must be generated in less than 2 minutes and presented as a visual dashboard (charts/tables), not just raw text.

### 11. Menu Approval
**As a manager**, I want to be able to approve or reject products proposed by the chef, with these being added directly to the online menu upon approval.

### 12. Inventory Automation
**As a manager**, I want the system to automatically deduct ingredients from the stock for every finalized order so that I have an accurate inventory record.

### 13. Real-time Menu Control
**As a manager**, I want to be able to activate or deactivate products in the digital menu in real-time so that customers cannot order dishes whose ingredients have run out.

.