package com.pizzastore.config;

import com.pizzastore.entity.*;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final double LOAD_TEST_INVENTORY_TARGET = 5000.0;

    private final ProductRepository productRepository;
    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.load-test.enabled:false}")
    private boolean loadTestSeedEnabled;

    public DatabaseSeeder(ProductRepository productRepository,
                          DishRepository dishRepository,
                          CategoryRepository categoryRepository,
                          ToppingRepository toppingRepository,
                          CouponRepository couponRepository,
                          BranchRepository branchRepository,
                          InventoryRepository inventoryRepository,
                          InventoryBatchRepository inventoryBatchRepository,
                          AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println(">>> Starting seed data (multi-branch mode)...");

        if (branchRepository.count() == 0) {
            Branch b1 = new Branch("PizzaStore Quận 10", "268 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM", 10.7731, 106.6596);
            Branch b2 = new Branch("PizzaStore Quận 1", "15 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM", 10.7769, 106.7009);
            Branch b3 = new Branch("PizzaStore Thủ Đức", "Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức", 10.8515, 106.7585);
            branchRepository.saveAll(List.of(b1, b2, b3));
        }

        if (dishRepository.count() == 0) {
            Category catPizza = categoryRepository.save(new Category("Pizza"));
            Category catPasta = categoryRepository.save(new Category("Pasta"));
            Category catAppetizer = categoryRepository.save(new Category("Khai vị"));
            Category catDrink = categoryRepository.save(new Category("Nước uống"));
            Category catDessert = categoryRepository.save(new Category("Tráng miệng"));

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

            List<Branch> allBranches = branchRepository.findAll();
            for (Branch branch : allBranches) {
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

            createSingleItem(catPasta, "Mỳ Ý Bò Băm", "Mỳ Ý sốt cà chua bò băm truyền thống",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951324/ntztks4xphxornlal1wm.jpg",
                    89000.0, myY, 0.15, boBam, 0.1);
        }

        if (toppingRepository.count() == 0) {
            Product phoMai = productRepository.findByName("Phô mai Mozzarella");
            if (phoMai != null) {
                Topping themPhoMai = new Topping();
                themPhoMai.setName("Thêm Phô Mai Mozzarella");
                themPhoMai.setPrice(15000.0);
                themPhoMai.setProduct(phoMai);
                themPhoMai.setQuantityNeeded(0.05);
                toppingRepository.save(themPhoMai);
            }
        }

        if (couponRepository.count() == 0) {
            Coupon c1 = new Coupon();
            c1.setCode("WELCOME10");
            c1.setDiscountPercent(10.0);
            c1.setActive(true);
            c1.setExpirationDate(LocalDate.now().plusMonths(3));
            couponRepository.save(c1);
        }

        if (loadTestSeedEnabled) {
            seedLoadTestCustomers();
            seedInventoryForLoadTesting();
        }
    }

    private void seedLoadTestCustomers() {
        String password = "123456";
        for (int i = 1; i <= 100; i++) {
            String suffix = String.format("%06d", i);
            String phoneNumber = "0819" + suffix;
            String email = "loadtest" + suffix + "@example.com";

            Account account = accountRepository.findByUsername(phoneNumber).orElseGet(Account::new);
            account.setUsername(phoneNumber);
            account.setPassword(passwordEncoder.encode(password));
            account.setRole(RoleName.CUSTOMER);
            account.setFirstLogin(false);

            Customer customer = customerRepository.findByAccount_Username(phoneNumber).orElseGet(Customer::new);
            customer.setFullName("Load Test Customer " + suffix);
            customer.setPhoneNumber(phoneNumber);
            customer.setAddress("Load Test Address " + suffix);
            customer.setEmail(email);
            customer.setAccount(account);

            customerRepository.save(customer);
        }
    }

    private void seedInventoryForLoadTesting() {
        List<Branch> branches = branchRepository.findAll();
        if (branches.isEmpty()) {
            return;
        }

        Product botMi = ensureProduct("Bột mì", "kg");
        Product phoMai = ensureProduct("Phô mai Mozzarella", "kg");
        Product sotCa = ensureProduct("Sốt cà chua", "lit");
        Product tom = ensureProduct("Tôm", "kg");
        Product muc = ensureProduct("Mực", "kg");
        Product boBam = ensureProduct("Bò băm", "kg");
        Product xucXich = ensureProduct("Xúc xích", "kg");
        Product myY = ensureProduct("Mỳ Ý khô", "kg");
        Product kem = ensureProduct("Kem tươi", "lit");
        Product coca = ensureProduct("Coca Cola", "lon");

        for (Branch branch : branches) {
            topUpInventory(branch, botMi, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, phoMai, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, sotCa, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, tom, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, muc, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, boBam, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, xucXich, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, myY, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, kem, LOAD_TEST_INVENTORY_TARGET);
            topUpInventory(branch, coca, LOAD_TEST_INVENTORY_TARGET * 4);
        }
    }

    private Product ensureProduct(String name, String unit) {
        Product product = productRepository.findByName(name);
        if (product != null) {
            return product;
        }
        return productRepository.save(new Product(name, unit));
    }

    private void topUpInventory(Branch branch, Product product, double targetQuantity) {
        Inventory inventory = inventoryRepository.findByBranchAndProduct(branch, product)
                .orElseGet(() -> new Inventory(branch, product, 0.0));

        if (inventory.getQuantityAvailable() < targetQuantity) {
            inventory.setQuantityAvailable(targetQuantity);
            inventoryRepository.save(inventory);
        }

        double usableBatchQuantity = inventoryBatchRepository
                .findUsableBatchesForDeduction(branch, product, LocalDate.now())
                .stream()
                .mapToDouble(InventoryBatch::getQuantityRemaining)
                .sum();

        double desiredQuantity = inventory.getQuantityAvailable();
        if (usableBatchQuantity < desiredQuantity) {
            inventoryBatchRepository.save(new InventoryBatch(
                    branch,
                    product,
                    desiredQuantity - usableBatchQuantity,
                    LocalDateTime.now(),
                    LocalDate.now().plusMonths(3)
            ));
        }
    }

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
