```mermaid
config: layout: elk

flowchart TB subgraph Guest[Guest (No Account)] G1["View Menu"] G2["Place Custom Order"] G3["Occupy Table"] end

subgraph Customer[Customer (With Account)]
  C1["Leave Review"]
end

subgraph Waiter[Waiter]
  W1["View Kitchen Order Status"]
  W2["Deliver Finished Order to Table"]
end

subgraph Chef[Chef]
  CH1["Propose Product (Needs Approval)"]
  CH2["Propose Ingredient Addition"]
  CH3["Get Recipe Suggestions from AI Assistant"]
end

subgraph Manager[Manager]
  M1["Approve/Reject Products (with Feedback)"]
  M2["Add Product (Auto-approved)"]
  M3["Manage Ingredients (Edit/Delete/Stock)"]
  M4["Manage Floor Plan (Add/Remove Tables/Walls)"]
  M5["Request Reports (Sales/Reviews)"]
  M6["Manage User Roles and View Users"]
end

subgraph System[System Components]
  S1["AI Assistant"]
  S2["Reports Generator"]
  S3["Sales Data & Reviews"]
end

%% relationships (actors -> use cases)
G1 -.->|"inherits from"| C1
Guest --> G1 & G2 & G3
Customer --> G1 & G2 & G3 & C1
Waiter --> W1 & W2
Chef --> CH1 & CH2 & CH3
Manager --> M1 & M2 & M3 & M4 & M5 & M6

%% system interactions
CH3 --> S1
M5 --> S2 --> S3
S1 --> CH1
S1 --> CH2

%% style by actor group
classDef guest fill:#ecfeff,stroke:#22d3ee,color:#000;
classDef customer fill:#fdf4ff,stroke:#e879f9,color:#000;
classDef waiter fill:#fff7ed,stroke:#fb923c,color:#000;
classDef chef fill:#f0fdf4,stroke:#4ade80,color:#000;
classDef manager fill:#fef2f2,stroke:#f87171,color:#000;
classDef system fill:#eef2ff,stroke:#818cf8,color:#000;

class Guest,G1,G2,G3 guest;
class Customer,C1 customer;
class Waiter,W1,W2 waiter;
class Chef,CH1,CH2,CH3 chef;
class Manager,M1,M2,M3,M4,M5,M6 manager;
class System,S1,S2,S3 system;

