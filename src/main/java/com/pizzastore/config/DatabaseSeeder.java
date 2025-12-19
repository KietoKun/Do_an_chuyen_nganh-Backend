package com.pizzastore.config;

import com.pizzastore.entity.*;
import com.pizzastore.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DatabaseSeeder(ProductRepository productRepository,
                          DishRepository dishRepository,
                          CategoryRepository categoryRepository,
                          ToppingRepository toppingRepository,
                          CouponRepository couponRepository) {
        this.productRepository = productRepository;
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> Bắt đầu Seeding dữ liệu (CHẾ ĐỘ TEST KHO THẤP)...");

        // QUAN TRỌNG:
        // Nếu bạn muốn reset lại dữ liệu cũ, hãy đổi ddl-auto=create-drop trong application.properties một lần
        // Hoặc xóa tay các bảng trong pgAdmin trước khi chạy lại.

        if (dishRepository.count() == 0) {
            // 1. TẠO CATEGORY
            Category catPizza = categoryRepository.save(new Category("Pizza"));
            Category catPasta = categoryRepository.save(new Category("Pasta"));
            Category catAppetizer = categoryRepository.save(new Category("Khai vị"));
            Category catDrink = categoryRepository.save(new Category("Nước uống"));
            Category catDessert = categoryRepository.save(new Category("Tráng miệng"));

            // 2. TẠO NGUYÊN LIỆU (Product) - ĐÃ GIẢM SỐ LƯỢNG ĐỂ TEST
            // Giả sử 1 Pizza cần 0.2kg bột -> 2kg làm được 10 cái
            Product botMi = productRepository.save(new Product("Bột mì", 2.0, "kg")); // Cũ: 100.0

            // Giả sử 1 Pizza cần 0.1kg phô mai -> 1.5kg làm được 15 cái
            Product phoMai = productRepository.save(new Product("Phô mai Mozzarella", 1.5, "kg")); // Cũ: 50.0

            Product sotCa = productRepository.save(new Product("Sốt cà chua", 2.0, "lit")); // Cũ: 50.0

            // Tôm đắt tiền, để ít thôi -> 1kg (Làm được khoảng 5-7 cái Pizza Hải sản)
            Product tom = productRepository.save(new Product("Tôm", 1.0, "kg")); // Cũ: 20.0

            Product muc = productRepository.save(new Product("Mực", 1.0, "kg")); // Cũ: 20.0
            Product boBam = productRepository.save(new Product("Bò băm", 2.0, "kg")); // Cũ: 20.0
            Product xucXich = productRepository.save(new Product("Xúc xích", 2.0, "kg")); // Cũ: 20.0
            Product myY = productRepository.save(new Product("Mỳ Ý khô", 3.0, "kg")); // Cũ: 50.0
            Product kem = productRepository.save(new Product("Kem tươi", 2.0, "lit")); // Cũ: 20.0

            // Nước ngọt: Để 5 lon -> Mua 6 lon là lỗi ngay
            Product coca = productRepository.save(new Product("Coca Cola", 5.0, "lon")); // Cũ: 100.0

            // 3. TẠO MÓN ĂN (DISHES)
            // --- NHÓM PIZZA ---
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

            // --- NHÓM PASTA ---
            createSingleItem(catPasta, "Mỳ Ý Bò Bằm", "Mỳ Ý sốt cà chua bò bằm truyền thống",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951324/ntztks4xphxornlal1wm.jpg",
                    89000.0, myY, 0.15, boBam, 0.1);
            createSingleItem(catPasta, "Mỳ Ý Carbonara", "Sốt kem nấm béo ngậy",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951380/crnn1iwvqh7atgmyas5e.jpg",
                    99000.0, myY, 0.15, kem, 0.05);
            createSingleItem(catPasta, "Mỳ Ý Hải Sản", "Tôm mực tươi ngon",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951421/m8ulzmos9jwponezcomw.jpg",
                    109000.0, myY, 0.15, tom, 0.1);
            createSingleItem(catPasta, "Nui Đút Lò", "Nui bỏ lò phô mai",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951510/exojkj0xf8yjh3gmiv5p.jpg",
                    119000.0, myY, 0.15, phoMai, 0.1);

            // --- NHÓM KHAI VỊ ---
            createSingleItem(catAppetizer, "Khoai Tây Chiên", "Giòn rụm",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951555/fis171drplnss2og980o.jpg",
                    39000.0, null, 0, null, 0);
            createSingleItem(catAppetizer, "Mực Chiên Giòn", "Mực vòng chiên giòn",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951622/stn6gmj4qtpid76zqeaz.jpg",
                    69000.0, muc, 0.2, null, 0);
            createSingleItem(catAppetizer, "Salad Cá Ngừ", "Rau xanh và cá ngừ",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951721/hxprdxxnjhmxs9r5tv8m.jpg",
                    59000.0, null, 0, null, 0);
            createSingleItem(catAppetizer, "Bánh Mì Bơ Tỏi", "Thơm lừng bơ tỏi",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951787/suettqizugxi3txtgwe8.jpg",
                    29000.0, botMi, 0.1, null, 0);

            // --- NHÓM NƯỚC UỐNG ---
            createSingleItem(catDrink, "Coca Cola", "Lon 330ml",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951842/ohqmwlmhfgtcagl5hlfr.jpg",
                    20000.0, coca, 1.0, null, 0);
            createSingleItem(catDrink, "Pepsi", "Lon 330ml",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951885/gjst0jcgyuttwzhwkrzc.jpg",
                    20000.0, null, 0, null, 0);
            createSingleItem(catDrink, "7Up", "Lon 330ml",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951917/u1f3n5w8yeanggnp7j6f.jpg",
                    20000.0, null, 0, null, 0);
            createSingleItem(catDrink, "Nước Suối", "Chai 500ml",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951966/ogvxewrywuwxax6xyqx2.jpg",
                    10000.0, null, 0, null, 0);

            // --- NHÓM TRÁNG MIỆNG ---
            createSingleItem(catDessert, "Bánh Mousse Chanh Leo", "Chua ngọt thanh mát",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952046/nsw2wm5teh7ttsksefqx.jpg",
                    49000.0, botMi, 0.05, kem, 0.02);
            createSingleItem(catDessert, "Kem Vani", "Mát lạnh",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952082/z2mdsarpmu7277c7ntfs.jpg",
                    29000.0, kem, 0.1, null, 0);
            createSingleItem(catDessert, "Bánh Tiramisu", "Hương vị Ý",
                    "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952131/ugvr5l98nphqupywzyz6.jpg",
                    59000.0, botMi, 0.05, null, 0);
        }

        // 4. TOPPING (Giữ nguyên)
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

            if (xucXich != null) {
                Topping themXucXich = new Topping();
                themXucXich.setName("Thêm Xúc Xích");
                themXucXich.setPrice(20000.0);
                themXucXich.setProduct(xucXich);
                themXucXich.setQuantityNeeded(0.05);
                toppingRepository.save(themXucXich);
            }

            if (botMi != null) {
                Topping deDay = new Topping();
                deDay.setName("Đế Bánh Dày");
                deDay.setPrice(10000.0);
                deDay.setProduct(botMi);
                deDay.setQuantityNeeded(0.1);
                toppingRepository.save(deDay);
            }
        }

        // 5. COUPON (Giữ nguyên)
        if (couponRepository.count() == 0) {
            Coupon c1 = new Coupon();
            c1.setCode("WELCOME10");
            c1.setDiscountPercent(10.0);
            c1.setDiscountAmount(0.0);
            c1.setActive(true);
            c1.setExpirationDate(LocalDate.now().plusMonths(3));
            couponRepository.save(c1);

            Coupon c2 = new Coupon();
            c2.setCode("HELLOSUMMER");
            c2.setDiscountPercent(0.0);
            c2.setDiscountAmount(50000.0);
            c2.setActive(true);
            c2.setExpirationDate(LocalDate.now().plusDays(30));
            couponRepository.save(c2);

            Coupon c3 = new Coupon();
            c3.setCode("EXPIRED2023");
            c3.setDiscountPercent(50.0);
            c3.setActive(false);
            c3.setExpirationDate(LocalDate.of(2023, 12, 31));
            couponRepository.save(c3);
        }
    }

    // --- HELPER METHODS (Giữ nguyên) ---
    private void createPizza(Category cat, String name, String desc, String imageUrl,
                             Product bot, Product phomai, Product sot, Product topping) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(desc);
        dish.setImageUrl(imageUrl);
        dish.setCategory(cat);
        dish.setAvailable(true);

        // Lưu ý: Món XL dùng 0.4kg bột. Nếu kho có 2kg -> làm được 5 cái size XL là hết.
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