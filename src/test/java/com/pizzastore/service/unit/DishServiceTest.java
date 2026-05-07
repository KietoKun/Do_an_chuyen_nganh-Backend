package com.pizzastore.service.unit;

import com.pizzastore.dto.DishRequest;
import com.pizzastore.entity.Category;
import com.pizzastore.entity.Dish;
import com.pizzastore.repository.*;
import com.pizzastore.service.DishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishServiceTest {

    @Mock
    private DishRepository dishRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    // Khai báo các mock khác dù không dùng trong hàm này để Spring không báo lỗi Null
    @Mock private BranchRepository branchRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private DishVariantRepository dishVariantRepository;

    @InjectMocks
    private DishService dishService;

    @Test
    void addDish_ShouldSaveDishSuccessfully_WhenCategoryExists() {
        // 1. ARRANGE: Chuẩn bị dữ liệu đầu vào
        DishRequest request = new DishRequest();
        request.setName("Pizza Hải Sản");
        request.setDescription("Ngon tuyệt vời");
        request.setCategoryId(1L);
        // Bỏ qua tạo Variant/Recipe cho code test gọn gàng,
        // bạn có thể thêm request.setVariants(...) nếu muốn test sâu hơn

        Category mockCategory = new Category();
        mockCategory.setId(1L);
        mockCategory.setName("Pizza");

        Dish savedDishMock = new Dish();
        savedDishMock.setId(100L);
        savedDishMock.setName("Pizza Hải Sản");

        // Dặn dò Mock: Khi tìm category id 1 thì trả về mockCategory
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        // Khi lưu dish thì trả về savedDishMock
        when(dishRepository.save(any(Dish.class))).thenReturn(savedDishMock);

        // 2. ACT: Thực thi hàm cần test
        Dish result = dishService.addDish(request);

        // 3. ASSERT: Kiểm tra kết quả
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Pizza Hải Sản", result.getName());

        // Kiểm tra xem dữ liệu nhét vào hàm save() có chuẩn không
        ArgumentCaptor<Dish> dishCaptor = ArgumentCaptor.forClass(Dish.class);
        verify(dishRepository).save(dishCaptor.capture());
        Dish capturedDish = dishCaptor.getValue();

        assertTrue(capturedDish.isAvailable());
        assertEquals(mockCategory, capturedDish.getCategory());
    }

    @Test
    void addDish_ShouldThrowException_WhenCategoryNotFound() {
        // 1. ARRANGE
        DishRequest request = new DishRequest();
        request.setCategoryId(99L); // ID không tồn tại

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT: Kỳ vọng sẽ ném ra lỗi RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dishService.addDish(request);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy Category ID: 99"));

        // Đảm bảo hàm save của dishRepository KHÔNG BAO GIỜ ĐƯỢC GỌI
        verify(dishRepository, never()).save(any(Dish.class));
    }
    @Test
    void getDishById_ShouldReturnDishDetail_WhenDishExists() {
        // 1. ARRANGE
        Long dishId = 1L;
        Dish mockDish = new Dish();
        mockDish.setId(dishId);
        mockDish.setName("Pizza Bò");
        mockDish.setDescription("Rất ngon");

        Category mockCategory = new Category();
        mockCategory.setId(2L);
        mockCategory.setName("Pizza");
        mockDish.setCategory(mockCategory);

        when(dishRepository.findById(dishId)).thenReturn(Optional.of(mockDish));

        // 2. ACT
        var response = dishService.getDishById(dishId);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(dishId, response.getId());
        assertEquals("Pizza Bò", response.getName());
        assertEquals("Pizza", response.getCategory().getName());
        assertEquals("Rất ngon", response.getDescription());
    }

    @Test
    void getDishById_ShouldThrowException_WhenDishNotFound() {
        // 1. ARRANGE
        Long unknownDishId = 999L;
        when(dishRepository.findById(unknownDishId)).thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dishService.getDishById(unknownDishId);
        });
        assertEquals("Món không tồn tại", exception.getMessage());
    }

    @Test
    void getMenu_ShouldReturnOnlyAvailableDishes() {
        // 1. ARRANGE
        Dish activeDish = new Dish();
        activeDish.setId(1L);
        activeDish.setName("Pizza Phô Mai");
        activeDish.setAvailable(true); // Đang mở bán

        Category cat = new Category();
        cat.setName("Pizza");
        activeDish.setCategory(cat);

        // dishRepository.findByIsAvailableTrue() được gọi trong hàm getMenu()
        when(dishRepository.findByIsAvailableTrue()).thenReturn(List.of(activeDish));

        // Bỏ qua logic tính số lượng chi nhánh (trả về list rỗng)
        org.mockito.Mockito.lenient().when(branchRepository.findAll()).thenReturn(List.of());

        // 2. ACT
        var menu = dishService.getMenu();

        // 3. ASSERT
        assertNotNull(menu);
        assertEquals(1, menu.size()); // Chỉ lấy món đang mở bán
        assertEquals("Pizza Phô Mai", menu.get(0).getName());
        assertEquals("Pizza", menu.get(0).getCategory());
    }
}
