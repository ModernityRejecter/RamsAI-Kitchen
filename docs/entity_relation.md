# Kitchen System Entity Relationship Diagram

This diagram shows all key entities in the system and their relationships.

```mermaid
---
config:
  layout: elk
---
erDiagram
    USER ||--o{ AICHAT_SESSION : initiates
    USER ||--o{ AUDIT_LOG : performs
    USER ||--o{ REFRESH_TOKEN : has
    USER ||--o{ USER_TOKEN : receives
    USER ||--o{ REVIEW : writes
    USER ||--o{ ORDER : places
    AICHAT_SESSION ||--|{ AI_MESSAGE : contains
    AI_MESSAGE }o--|| AICHAT_SESSION : "belongs to"
    CATEGORY ||--o{ PRODUCT : contains
    PRODUCT ||--o{ PRODUCT_INGREDIENT : uses
    INGREDIENT ||--o{ PRODUCT_INGREDIENT : "used in"
    INGREDIENT ||--o{ INVENTORY_LOG : tracked
    PRODUCT ||--o{ REVIEW : "reviewed in"
    PRODUCT ||--o{ ORDER_ITEM : "ordered as"
    ORDER ||--|{ ORDER_ITEM : contains
    RESTAURANT_TABLE ||--o{ ORDER : "has orders"
    ORDER }o--|| RESTAURANT_TABLE : "placed at"

    USER {
        long id PK
        string username UK
        string password_hash
        string email UK
        string user_role
        boolean is_active
        boolean is_email_verified
        datetime last_login_at
        datetime created_at
        string profile_picture_url
    }

    AICHAT_SESSION {
        long id PK
        long user_id FK
        datetime started_at
        text summary
    }

    AI_MESSAGE {
        long id PK
        long session_id FK
        string sender_type
        text content
        datetime timestamp
    }

    AUDIT_LOG {
        long id PK
        long user_id FK
        string username
        string action
        string ip_address
        string user_agent
        string status
        string details
        datetime timestamp
    }

    CATEGORY {
        long id PK
        string name UK
        string description
    }

    PRODUCT {
        long id PK
        string name
        string description
        decimal base_price
        boolean is_active
        boolean is_special_offer
        boolean is_daily_recipe
        decimal discount_price
        double average_rating
        string rejection_feedback
        string approval_status
        long category_id FK
    }

    INGREDIENT {
        long id PK
        string name UK
        string unit
        double current_stock
        double minimum_stock_threshold
    }

    PRODUCT_INGREDIENT {
        long id PK
        long product_id FK
        long ingredient_id FK
        double quantity_required
    }

    INVENTORY_LOG {
        long id PK
        long ingredient_id FK
        double change_amount
        string reason
        datetime timestamp
    }

    ORDER {
        long id PK
        long table_id FK
        long customer_id
        string status
        decimal total_price
        datetime created_at
        datetime updated_at
    }

    ORDER_ITEM {
        long id PK
        long order_id FK
        long product_id FK
        integer quantity
        decimal unit_price
        string special_notes
        string item_status
    }

    REVIEW {
        long id PK
        long product_id FK
        long user_id FK
        integer rating
        string comment
        datetime created_at
    }

    RESTAURANT_TABLE {
        long id PK
        integer table_number
        string status
        integer x_pos
        integer y_pos
        long occupied_by_user_id
        datetime occupied_at
    }

    RESTAURANT_WALL {
        long id PK
        integer x_pos
        integer y_pos
    }

    REFRESH_TOKEN {
        long id PK
        string token UK
        long user_id FK
        instant expiry_date
        boolean revoked
    }

    USER_TOKEN {
        long id PK
        string token UK
        long user_id FK
        string token_type
        datetime expiry_date
        datetime created_at
    }


