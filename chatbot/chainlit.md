# E-Commerce Analytics Assistant

Welcome! I'm your AI-powered analytics assistant for the e-commerce platform.

## What I can do

Ask me anything about your data in plain English:

- **Revenue & Sales** — "What was my total revenue last month?"
- **Products** — "Show me the top 10 products by quantity sold"
- **Orders** — "How many orders are in each status?"
- **Customers** — "Which cities have the most customers?" *(Admin only)*
- **Shipments** — "What's the average customer rating by shipping mode?"
- **Reviews** — "Which products have the most negative sentiment reviews?"

## Getting started

Paste your JWT access token (from the `/api/auth/login` response) when prompted, then start asking questions.

Your data access is scoped to your role:
- **Admin** — full platform view
- **Corporate** — your stores and their data
- **Individual** — your own orders, reviews, and profile
