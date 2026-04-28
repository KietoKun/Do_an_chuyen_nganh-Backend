# WebSocket Handoff For FE

## 1. Mục tiêu

Tài liệu này mô tả phần realtime đã được implement ở backend cho luồng order.

FE vẫn dùng HTTP API để tạo và cập nhật dữ liệu.
WebSocket chỉ dùng để nhận event realtime sau khi backend xử lý xong và save DB thành công.

## 2. WebSocket Endpoint

- Endpoint: `/ws`
- Protocol: `STOMP over WebSocket`
- Backend broker prefixes:
  - public topic: `/topic`
  - user queue: `/user`

Ví dụ URL local:

```text
ws://localhost:8080/ws
```

Nếu FE dùng SockJS/stompjs thì vẫn connect vào `/ws`.

## 3. Auth

Backend đang verify JWT ở lúc STOMP `CONNECT`.

FE phải gửi token trong header:

```text
Authorization: Bearer <jwt_token>
```

FE có thể gửi header khi gọi `client.connect()` / `stompClient.activate()`.

Nếu token sai hoặc thiếu, backend sẽ từ chối kết nối.

## 4. Topic / Queue Cần Subscribe

### 4.1 Customer

Customer subscribe:

```text
/user/queue/orders
```

Dùng để nhận event của chính đơn hàng của customer đó.

### 4.2 Kitchen / Staff / Manager / Admin

Dashboard nội bộ subscribe:

```text
/topic/orders/kitchen
```

Dùng để nhận tất cả event liên quan đến đơn hàng trong hệ thống.

### 4.3 Theo chi nhanh

Nếu FE có dashboard riêng theo branch:

```text
/topic/orders/branches/{branchId}
```

Vi du:

```text
/topic/orders/branches/2
```

### 4.4 Theo 1 đơn cụ thể

Neu FE co man hinh chi tiet 1 don va muon nghe rieng:

```text
/topic/orders/{orderId}
```

Vi du:

```text
/topic/orders/123
```

## 5. Event Name

Trương `event` trong payload hiện có các giá trị:

- `ORDER_CREATED`
- `ORDER_PAYMENT_UPDATED`
- `ORDER_CONFIRMED`
- `ORDER_STATUS_CHANGED`
- `ORDER_CANCELLED`

## 6. Khi Nao Backend Bắn Event

### `ORDER_CREATED`

Bắn sau khi customer tạo đơn thành công qua:

```text
POST /api/orders
```

Status ban đầu thường là:

```text
PENDING
```

### `ORDER_PAYMENT_UPDATED`

Bắn sau khi callback VNPAY thành công và backend đổi status sang:

```text
PAID
```

### `ORDER_CONFIRMED`

Bắn sau khi nhân viên duyệt đơn qua:

```text
PUT /api/orders/{id}/confirm
```

Status mới:

```text
CONFIRMED
```

### `ORDER_STATUS_CHANGED`

Bắn sau khi backend cập nhật trạng thái qua:

```text
PUT /api/orders/{id}/status
```

Cac status co the gap:

- `COOKING`
- `DELIVERING`
- `COMPLETED`

### `ORDER_CANCELLED`

Bắn sau khi backend hủy đơn thành công qua:

```text
PUT /api/orders/{id}/cancel
```

Status moi:

```text
CANCELLED
```

## 7. Payload Thực Tế

Backend đang gửi payload theo DTO `OrderRealtimeEvent`.

Payload mẫu:

```json
{
  "event": "ORDER_STATUS_CHANGED",
  "orderId": 123,
  "status": "COOKING",
  "previousStatus": "CONFIRMED",
  "orderTime": "2026-04-23T10:30:00",
  "acceptedAt": "2026-04-23T10:35:12",
  "totalPrice": 259000.0,
  "discountAmount": 20000.0,
  "finalTotalPrice": 239000.0,
  "deliveryMethod": "DELIVERY",
  "deliveryAddress": "123 Nguyen Trai, Q1",
  "note": "It cay",
  "customerId": 5,
  "customerName": "Nguyen Van A",
  "customerUsername": "customer01",
  "branchId": 2,
  "branchName": "Chi nhanh Quan 1",
  "handledById": 8,
  "handledByName": "Tran Van B",
  "handledByUsername": "chef01",
  "message": "Trang thai don hang da duoc cap nhat",
  "items": [
    {
      "orderDetailId": 1001,
      "dishVariantId": 11,
      "dishName": "Pizza Hai San",
      "size": "L",
      "quantity": 2,
      "unitPrice": 129500.0,
      "subTotal": 259000.0,
      "toppings": [
        "Pho mai",
        "Xuc xich"
      ]
    }
  ]
}
```

## 8. Ý Nghĩa Các Field Quan Trọng

- `event`: Loại sự kiện để FE phân nhánh.
- `orderId`: ID đơn hàng.
- `status`: Trạng thái mới nhất sau khi backend đã update.
- `previousStatus`: Trạng thái trước đó.
- `message`: Text phụ trợ, lời nhắn cho cửa hàng
- `customerId`, `customerName`, `customerUsername`: thong tin customer.
- `branchId`, `branchName`: chi nhanh xu ly don.
- `handledBy...`: nhan vien dang xu ly hoac da duyet don.
- `items[]`: snapshot rut gon de FE render nhanh ma khong can goi lai API ngay.

## 9. Cach FE Nen Xu Ly

### Customer app

Subscribe:

```text
/user/queue/orders
```

Goi y xu ly:

- Khi nhan event co `orderId` trung voi don dang xem, update timeline trang thai.
- Khi nhan `ORDER_PAYMENT_UPDATED`, hien thi da thanh toan.
- Khi nhan `ORDER_CANCELLED`, khoa cac action tiep theo neu can.

### Kitchen dashboard

Subscribe:

```text
/topic/orders/kitchen
```

Goi y xu ly:

- `ORDER_CREATED`: them don moi vao danh sach.
- `ORDER_CONFIRMED`: update nguoi nhan don / trang thai.
- `ORDER_STATUS_CHANGED`: doi cot / badge theo status.
- `ORDER_CANCELLED`: remove hoac danh dau don da huy.

### Branch dashboard

Subscribe:

```text
/topic/orders/branches/{branchId}
```

Chi render cac don cua branch hien tai.

## 10. Khuyen Nghi FE

- Khong dung `message` de lam logic.
- Nen dung `event`, `orderId`, `status`, `previousStatus`.
- Khi vao man hinh lan dau, van nen fetch HTTP de lay snapshot ban dau.
- WebSocket dung de cap nhat incremental sau do.
- Neu mat ket noi, FE nen reconnect va fetch lai danh sach de dong bo.

## 11. Vi Du STOMP Client

```js
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "ws://localhost:8080/ws",
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
  reconnectDelay: 5000,
});

client.onConnect = () => {
  client.subscribe("/user/queue/orders", (frame) => {
    const event = JSON.parse(frame.body);
    console.log("customer event", event);
  });

  client.subscribe("/topic/orders/kitchen", (frame) => {
    const event = JSON.parse(frame.body);
    console.log("kitchen event", event);
  });
};

client.activate();
```

## 12. Tom Tat Nhanh

- Connect vao `/ws`
- Gui JWT trong header `Authorization: Bearer <token>`
- Customer subscribe `/user/queue/orders`
- Staff/kitchen subscribe `/topic/orders/kitchen`
- Theo branch subscribe `/topic/orders/branches/{branchId}`
- Theo chi tiet don subscribe `/topic/orders/{orderId}`
- Xu ly theo `event` va `status`

