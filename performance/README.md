# k6 performance tests

Run from the project root:

```powershell
k6 run -e BASE_URL=http://localhost:8080 -e CUSTOMER_USERNAME=your_customer -e CUSTOMER_PASSWORD=your_password performance/order-load.test.js
k6 run -e BASE_URL=http://localhost:8080 performance/menu-load.test.js
k6 run -e BASE_URL=http://localhost:8080 -e ADMIN_USERNAME=0900000000 -e ADMIN_PASSWORD=123456 performance/statistics-load.test.js
```

Use the 100-user order test when you want to simulate many customers placing orders at the same time after the database has been reset and the seeder has created the load-test accounts.

```powershell
.\performance\run-order-100-users.ps1
```

Notes:

- `order-load.test.js` needs a valid customer account.
- The 100-user order test uses `performance/customers-100.json`.
- `statistics-load.test.js` uses the default admin credentials unless you override them.
- `ORDER_VARIANT_ID` can override the auto-picked menu variant for order testing.
