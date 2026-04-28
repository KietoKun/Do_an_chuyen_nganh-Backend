package com.pizzastore.controller;

import com.pizzastore.dto.TopSellingDishResponse;
import com.pizzastore.service.StatisticsService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "13. Thong ke", description = "API thong ke doanh thu va mon ban chay. Chi SUPER_ADMIN va MANAGER duoc truy cap; STAFF/CHEF khong co quyen xem so lieu.")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/top-selling-dishes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Thong ke top seller theo mon",
            description = """
                    Tra ve danh sach mon ban chay nhat, sap xep theo tong so luong ban giam dan.

                    Quyen truy cap:
                    - SUPER_ADMIN: khong truyen branchId de xem toan chuoi, hoac truyen branchId de xem mot chi nhanh.
                    - MANAGER: chi xem duoc chi nhanh cua minh. Neu truyen branchId khac chi nhanh cua minh se bi tu choi.
                    - STAFF/CHEF/CUSTOMER: khong co quyen xem endpoint nay.

                    Cach tinh:
                    - Chi tinh don co status = COMPLETED.
                    - quantitySold = tong OrderDetail.quantity theo mon.
                    - revenue = tong OrderDetail.subTotal theo mon; neu subTotal null thi dung unitPrice * quantity.
                    - fromDate/toDate loc theo Order.orderTime, dinh dang yyyy-MM-dd.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Danh sach mon ban chay",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = TopSellingDishResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "dishId": 1,
                                        "dishName": "Pizza Hai San",
                                        "quantitySold": 50,
                                        "revenue": 7500000
                                      },
                                      {
                                        "dishId": 2,
                                        "dishName": "Pizza Bo",
                                        "quantitySold": 42,
                                        "revenue": 6300000
                                      }
                                    ]
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Sai filter hoac manager xem nham chi nhanh"),
            @ApiResponse(responseCode = "403", description = "Khong co quyen. STAFF/CHEF/CUSTOMER se bi chan.")
    })
    public ResponseEntity<?> getTopSellingDishes(
            @Parameter(description = "ID chi nhanh can thong ke. SUPER_ADMIN co the bo trong de xem toan chuoi. MANAGER khong can truyen vi he thong tu lay chi nhanh cua manager.", example = "1")
            @RequestParam(required = false) Long branchId,

            @Parameter(description = "Ngay bat dau loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi khong gioi han ngay bat dau.", example = "2026-04-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "Ngay ket thuc loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi khong gioi han ngay ket thuc.", example = "2026-04-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            @Parameter(description = "So mon toi da muon lay. Mac dinh 10, toi da 100.", example = "10")
            @RequestParam(required = false) Integer limit) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<TopSellingDishResponse> result = statisticsService.getTopSellingDishes(
                    username,
                    branchId,
                    fromDate,
                    toDate,
                    limit
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Thong ke tong doanh thu",
            description = """
                    Tra ve tong doanh thu va so don hang hoan thanh trong khoang loc.

                    Quyen truy cap:
                    - SUPER_ADMIN: khong truyen branchId de xem toan chuoi, hoac truyen branchId de xem mot chi nhanh.
                    - MANAGER: chi xem duoc chi nhanh cua minh.
                    - STAFF/CHEF/CUSTOMER: khong co quyen xem endpoint nay.

                    Cach tinh:
                    - Chi tinh don co status = COMPLETED.
                    - revenue = tong finalTotalPrice neu co, nguoc lai dung totalPrice.
                    - orderCount = so don COMPLETED thoa dieu kien loc.
                    - fromDate/toDate loc theo Order.orderTime, dinh dang yyyy-MM-dd.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tong quan doanh thu",
                    content = @Content(
                            schema = @Schema(implementation = com.pizzastore.dto.RevenueSummaryResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "orderCount": 20,
                                      "revenue": 12000000
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Sai filter hoac manager xem nham chi nhanh"),
            @ApiResponse(responseCode = "403", description = "Khong co quyen. STAFF/CHEF/CUSTOMER se bi chan.")
    })
    public ResponseEntity<?> getRevenueSummary(
            @Parameter(description = "ID chi nhanh can thong ke. SUPER_ADMIN co the bo trong de xem toan chuoi. MANAGER khong can truyen vi he thong tu lay chi nhanh cua manager.", example = "1")
            @RequestParam(required = false) Long branchId,

            @Parameter(description = "Ngay bat dau loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi khong gioi han ngay bat dau.", example = "2026-04-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "Ngay ket thuc loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi khong gioi han ngay ket thuc.", example = "2026-04-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(statisticsService.getRevenueSummary(username, branchId, fromDate, toDate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/revenue/daily")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Thong ke doanh thu theo tung ngay",
            description = """
                    Tra ve doanh thu va so don hang theo tung ngay trong khoang loc.

                    Cach tinh:
                    - Chi tinh don co status = COMPLETED.
                    - revenue = tong finalTotalPrice neu co, nguoc lai dung totalPrice.
                    - Nhung ngay khong co don hang van duoc tra ve voi revenue = 0 va orderCount = 0.
                    - fromDate/toDate dinh dang yyyy-MM-dd. Neu bo trong thi mac dinh tu dau thang hien tai den hom nay.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Danh sach doanh thu theo ngay",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = com.pizzastore.dto.DailyRevenueResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "date": "2026-04-01",
                                        "revenue": 135000,
                                        "orderCount": 1
                                      },
                                      {
                                        "date": "2026-04-02",
                                        "revenue": 250000,
                                        "orderCount": 3
                                      },
                                      {
                                        "date": "2026-04-03",
                                        "revenue": 0,
                                        "orderCount": 0
                                      }
                                    ]
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Sai filter hoac manager xem nham chi nhanh"),
            @ApiResponse(responseCode = "403", description = "Khong co quyen. STAFF/CHEF/CUSTOMER se bi chan.")
    })
    public ResponseEntity<?> getDailyRevenue(
            @Parameter(description = "ID chi nhanh can thong ke. SUPER_ADMIN co the bo trong de xem toan chuoi. MANAGER khong can truyen vi he thong tu lay chi nhanh cua manager.", example = "1")
            @RequestParam(required = false) Long branchId,

            @Parameter(description = "Ngay bat dau loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi lay tu dau thang cua toDate/hom nay.", example = "2026-04-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "Ngay ket thuc loc theo orderTime, dinh dang yyyy-MM-dd. Neu bo trong thi mac dinh hom nay.", example = "2026-04-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(statisticsService.getDailyRevenue(username, branchId, fromDate, toDate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
