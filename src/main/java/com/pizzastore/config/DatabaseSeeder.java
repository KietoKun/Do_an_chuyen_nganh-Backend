package com.pizzastore.config;

import com.pizzastore.entity.*;
import com.pizzastore.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository; // ĐÃ THÊM
    private final InventoryBatchRepository inventoryBatchRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DatabaseSeeder(ProductRepository productRepository,
                          DishRepository dishRepository,
                          CategoryRepository categoryRepository,
                          ToppingRepository toppingRepository,
                          CouponRepository couponRepository,
                          BranchRepository branchRepository,
                          InventoryRepository inventoryRepository,
                          InventoryBatchRepository inventoryBatchRepository) { // ĐÃ THÊM
        this.productRepository = productRepository;
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository; // ĐÃ THÊM
        this.inventoryBatchRepository = inventoryBatchRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> Bắt đầu Seeding dữ liệu (CHẾ ĐỘ MULTI-BRANCH)...");

        // 1. TẠO CHI NHÁNH
        if (branchRepository.count() == 0) {
            System.out.println(">>> Đang tạo dữ liệu 3 Chi nhánh...");
            Branch b1 = new Branch("PizzaStore Quận 10", "268 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM", 10.7731, 106.6596);
            Branch b2 = new Branch("PizzaStore Quận 1", "15 Lê Thánh Tôn, Phường Bến Nghé, Quận 1, TP.HCM", 10.7769, 106.7009);
            Branch b3 = new Branch("PizzaStore Thủ Đức", "Võ Văn Ngân, Phường Linh Chiểu, TP. Thủ Đức", 10.8515, 106.7585);
            branchRepository.saveAll(List.of(b1, b2, b3));
        }

        if (dishRepository.count() == 0) {
            // 2. TẠO CATEGORY
            Category catPizza = categoryRepository.save(new Category("Pizza"));
            Category catPasta = categoryRepository.save(new Category("Pasta"));
            Category catAppetizer = categoryRepository.save(new Category("Khai vị"));
            Category catDrink = categoryRepository.save(new Category("Nước uống"));
            Category catDessert = categoryRepository.save(new Category("Tráng miệng"));

            // 3. TẠO NGUYÊN LIỆU (Chỉ có tên và đơn vị)
            Product botMi = productRepository.save(new Product("Bột mì", "kg"));
            Product phoMai = productRepository.save(new Product("Phô mai Mozzarella", "kg"));
            Product sotCa = productRepository.save(new Product("Sốt cà chua", "lit"));
            Product tom = productRepository.save(new Product("Tôm", "kg"));
            Product muc = productRepository.save(new Product("Mực", "kg"));
            Product boBam = productRepository.save(new Product("Bò băm", "kg"));
            Product xucXich = productRepository.save(new Product("Xúc xích", "kg"));
            Product myY = productRepository.save(new Product("Mỳ Ý khô", "kg"));
            Product kem = productRepository.save(new Product("Kem tươi", "lit"));
            Product coca = productRepository.save(new Product("Coca Cola", "lon"));

            // 4. CHIA KHO (Nhập số lượng cho từng chi nhánh)
            List<Branch> allBranches = branchRepository.findAll();
            for (Branch branch : allBranches) {
                // Nhập kho cho từng chi nhánh, giả sử chi nhánh nào cũng có mức kho giống nhau để test
                inventoryRepository.save(new Inventory(branch, botMi, 10.0));
                inventoryRepository.save(new Inventory(branch, phoMai, 5.0));
                inventoryRepository.save(new Inventory(branch, sotCa, 5.0));
                inventoryRepository.save(new Inventory(branch, tom, 2.0));
                inventoryRepository.save(new Inventory(branch, muc, 2.0));
                inventoryRepository.save(new Inventory(branch, boBam, 3.0));
                inventoryRepository.save(new Inventory(branch, xucXich, 3.0));
                inventoryRepository.save(new Inventory(branch, myY, 5.0));
                inventoryRepository.save(new Inventory(branch, kem, 3.0));
                inventoryRepository.save(new Inventory(branch, coca, 20.0));
            }

            // 5. TẠO MÓN ĂN (Giữ nguyên logic của bạn)
            createPizza(catPizza, "Pizza Hải Sản", "Tôm, mực, sốt Thousand Island",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951100/wi2qx6sktce7y4woxhmo.jpg",
                    botMi, phoMai, sotCa, tom);

            createPizza(catPizza, "Pizza Bò Băm", "Bò băm, ngô, sốt BBQ",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951170/gxfsemm7pidm5lmfl1ls.jpg",
                    botMi, phoMai, sotCa, boBam);

            createPizza(catPizza, "Pizza Pepperoni", "Xúc xích cay, phô mai",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951170/gxfsemm7pidm5lmfl1ls.jpg",
                    botMi, phoMai, sotCa, xucXich);

            createPizza(catPizza, "Pizza Phô Mai", "3 loại phô mai thượng hạng",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951262/a0vdk79maz9m2k77jc4i.jpg",
                    botMi, phoMai, sotCa, null);

            createSingleItem(catPasta, "Mỳ Ý Bò Bằm", "Mỳ Ý sốt cà chua bò bằm truyền thống",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951324/ntztks4xphxornlal1wm.jpg",
                    89000.0, myY, 0.15, boBam, 0.1);

            // ... (Bạn có thể thêm lại các món khác ở đây nếu muốn, mình rút gọn cho dễ nhìn)
        }

        // 6. TOPPING (Giữ nguyên)
        if (toppingRepository.count() == 0) {
            Product phoMai = productRepository.findByName("Phô mai Mozzarella");
            Product xucXich = productRepository.findByName("Xúc xích");
            Product botMi = productRepository.findByName("Bột mì");

            if (phoMai != null) {
                Topping themPhoMai = new Topping();
                themPhoMai.setName("Thêm Phô Mai Mozzarella");
                themPhoMai.setPrice(15000.0);
                themPhoMai.setProduct(phoMai);
                themPhoMai.setQuantityNeeded(0.05);
                toppingRepository.save(themPhoMai);
            }
            // ... (Các Topping khác)
        }

        // 7. COUPON (Giữ nguyên)
        if (couponRepository.count() == 0) {
            Coupon c1 = new Coupon();
            c1.setCode("WELCOME10");
            c1.setDiscountPercent(10.0);
            c1.setActive(true);
            c1.setExpirationDate(LocalDate.now().plusMonths(3));
            couponRepository.save(c1);
            // ... (Các Coupon khác)
        }

        seedInventoryBatchesIfMissing();
    }

    private void seedInventoryBatchesIfMissing() {
        if (inventoryBatchRepository.count() > 0) {
            return;
        }

        LocalDateTime importedAt = LocalDateTime.now();
        LocalDate expiredAt = LocalDate.now().plusMonths(3);
        for (Inventory inventory : inventoryRepository.findAll()) {
            inventoryBatchRepository.save(new InventoryBatch(
                    inventory.getBranch(),
                    inventory.getProduct(),
                    inventory.getQuantityAvailable(),
                    importedAt,
                    expiredAt
            ));
        }
    }

    // --- HELPER METHODS ---
    private void createPizza(Category cat, String name, String desc, String imageUrl,
                             Product bot, Product phomai, Product sot, Product topping) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(desc);
        dish.setImageUrl(imageUrl);
        dish.setCategory(cat);
        dish.setAvailable(true);

        addVariant(dish, "M", 120000.0, bot, 0.2, phomai, 0.1, topping, 0.1);
        addVariant(dish, "L", 180000.0, bot, 0.3, phomai, 0.15, topping, 0.15);
        addVariant(dish, "XL", 250000.0, bot, 0.4, phomai, 0.2, topping, 0.2);

        dishRepository.save(dish);
    }

    private void createSingleItem(Category cat, String name, String desc, String imageUrl,
                                  Double price, Product p1, double q1, Product p2, double q2) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(desc);
        dish.setImageUrl(imageUrl);
        dish.setCategory(cat);
        dish.setAvailable(true);

        addVariant(dish, "Standard", price, p1, q1, p2, q2, null, 0);
        dishRepository.save(dish);
    }

    private void addVariant(Dish dish, String size, Double price, Product p1, double q1, Product p2, double q2, Product p3, double q3) {
        DishVariant v = new DishVariant();
        v.setSize(size);
        v.setPrice(price);
        v.setDish(dish);

        if (p1 != null) v.addRecipe(new Recipe(v, p1, q1));
        if (p2 != null) v.addRecipe(new Recipe(v, p2, q2));
        if (p3 != null) v.addRecipe(new Recipe(v, p3, q3));

        dish.getVariants().add(v);
    }
}
