package com.pizzastore.config;

import com.pizzastore.entity.*;
import com.pizzastore.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public DatabaseSeeder(ProductRepository productRepository,
                          DishRepository dishRepository,
                          CategoryRepository categoryRepository,
                          ToppingRepository toppingRepository) {
        this.productRepository = productRepository;
        this.dishRepository = dishRepository;
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Kiểm tra nếu đã có dữ liệu thì thôi
        if (dishRepository.count() > 0) return;

        System.out.println(">>> Bắt đầu Seeding dữ liệu Menu mới...");

        // 1. TẠO CATEGORY
        Category catPizza = categoryRepository.save(new Category("Pizza"));
        Category catPasta = categoryRepository.save(new Category("Pasta"));
        Category catAppetizer = categoryRepository.save(new Category("Khai vị"));
        Category catDrink = categoryRepository.save(new Category("Nước uống"));
        Category catDessert = categoryRepository.save(new Category("Tráng miệng"));

        // 2. TẠO NGUYÊN LIỆU (Product)
        Product botMi = productRepository.save(new Product("Bột mì", 100.0, "kg"));
        Product phoMai = productRepository.save(new Product("Phô mai Mozzarella", 50.0, "kg"));
        Product sotCa = productRepository.save(new Product("Sốt cà chua", 50.0, "lit"));
        Product tom = productRepository.save(new Product("Tôm", 20.0, "kg"));
        Product muc = productRepository.save(new Product("Mực", 20.0, "kg"));
        Product boBam = productRepository.save(new Product("Bò băm", 20.0, "kg"));
        Product xucXich = productRepository.save(new Product("Xúc xích", 20.0, "kg"));
        Product myY = productRepository.save(new Product("Mỳ Ý khô", 50.0, "kg"));
        Product kem = productRepository.save(new Product("Kem tươi", 20.0, "lit"));
        Product coca = productRepository.save(new Product("Coca Cola", 100.0, "lon"));

        // 3. TẠO MÓN ĂN (DISHES)

        // --- NHÓM PIZZA (Có Size M, L, XL) ---
        createPizza(catPizza,
                "Pizza Hải Sản",
                "Tôm, mực, sốt Thousand Island", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951100/wi2qx6sktce7y4woxhmo.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                botMi, phoMai, sotCa, tom);

        createPizza(catPizza,
                "Pizza Bò Băm",
                "Bò băm, ngô, sốt BBQ", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951170/gxfsemm7pidm5lmfl1ls.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                botMi, phoMai, sotCa, boBam);

        createPizza(catPizza,
                "Pizza Pepperoni",
                "Xúc xích cay, phô mai", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951170/gxfsemm7pidm5lmfl1ls.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                botMi, phoMai, sotCa, xucXich);

        createPizza(catPizza,
                "Pizza Phô Mai",
                "3 loại phô mai thượng hạng", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951262/a0vdk79maz9m2k77jc4i.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                botMi, phoMai, sotCa, null);

        // --- NHÓM PASTA (1 Size: Standard) ---
        createSingleItem(catPasta,
                "Mỳ Ý Bò Bằm",
                "Mỳ Ý sốt cà chua bò bằm truyền thống", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951324/ntztks4xphxornlal1wm.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                89000.0, myY, 0.15, boBam, 0.1);

        createSingleItem(catPasta,
                "Mỳ Ý Carbonara",
                "Sốt kem nấm béo ngậy", // <--- Sửa mô tả ở đây
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951380/crnn1iwvqh7atgmyas5e.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                99000.0, myY, 0.15, kem, 0.05);

        createSingleItem(catPasta,
                "Mỳ Ý Hải Sản",
                "Tôm mực tươi ngon",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951421/m8ulzmos9jwponezcomw.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                109000.0, myY, 0.15, tom, 0.1);

        createSingleItem(catPasta,
                "Nui Đút Lò",
                "Nui bỏ lò phô mai",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951510/exojkj0xf8yjh3gmiv5p.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                119000.0, myY, 0.15, phoMai, 0.1);

        // --- NHÓM KHAI VỊ ---
        createSingleItem(catAppetizer,
                "Khoai Tây Chiên",
                "Giòn rụm",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951555/fis171drplnss2og980o.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                39000.0, null, 0, null, 0);

        createSingleItem(catAppetizer,
                "Mực Chiên Giòn",
                "Mực vòng chiên giòn",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951622/stn6gmj4qtpid76zqeaz.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                69000.0, muc, 0.2, null, 0);

        createSingleItem(catAppetizer,
                "Salad Cá Ngừ",
                "Rau xanh và cá ngừ",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951721/hxprdxxnjhmxs9r5tv8m.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                59000.0, null, 0, null, 0);

        createSingleItem(catAppetizer,
                "Bánh Mì Bơ Tỏi",
                "Thơm lừng bơ tỏi",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951787/suettqizugxi3txtgwe8.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                29000.0, botMi, 0.1, null, 0);

        // --- NHÓM NƯỚC UỐNG ---
        createSingleItem(catDrink,
                "Coca Cola",
                "Lon 330ml",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951842/ohqmwlmhfgtcagl5hlfr.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                20000.0, coca, 1.0, null, 0);

        createSingleItem(catDrink,
                "Pepsi",
                "Lon 330ml",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951885/gjst0jcgyuttwzhwkrzc.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                20000.0, null, 0, null, 0);

        createSingleItem(catDrink,
                "7Up",
                "Lon 330ml",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951917/u1f3n5w8yeanggnp7j6f.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                20000.0, null, 0, null, 0);

        createSingleItem(catDrink,
                "Nước Suối",
                "Chai 500ml",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764951966/ogvxewrywuwxax6xyqx2.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                10000.0, null, 0, null, 0);

        // --- NHÓM TRÁNG MIỆNG ---
        createSingleItem(catDessert,
                "Bánh Mousse Chanh Leo",
                "Chua ngọt thanh mát",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952046/nsw2wm5teh7ttsksefqx.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                49000.0, botMi, 0.05, kem, 0.02);

        createSingleItem(catDessert,
                "Kem Vani",
                "Mát lạnh",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952082/z2mdsarpmu7277c7ntfs.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                29000.0, kem, 0.1, null, 0);

        createSingleItem(catDessert,
                "Bánh Tiramisu",
                "Hương vị Ý",
                "https://res.cloudinary.com/dzt7upu2d/image/upload/v1764952131/ugvr5l98nphqupywzyz6.jpg", // TODO: Dán link ảnh Cloudinary vào đây
                59000.0, botMi, 0.05, null, 0);

        // 4. TẠO TOPPING (MÓN THÊM)
        // ==========================================
        if (toppingRepository.count() == 0) {
            // Topping: Thêm Phô Mai (Giá 15k, tốn 0.05kg Phô mai)
            Topping themPhoMai = new Topping();
            themPhoMai.setName("Thêm Phô Mai Mozzarella");
            themPhoMai.setPrice(15000.0);
            themPhoMai.setProduct(phoMai); // Trừ vào kho Phô mai
            themPhoMai.setQuantityNeeded(0.05);
            toppingRepository.save(themPhoMai);

            // Topping: Thêm Xúc Xích (Giá 20k, tốn 0.05kg Xúc xích)
            Topping themXucXich = new Topping();
            themXucXich.setName("Thêm Xúc Xích");
            themXucXich.setPrice(20000.0);
            themXucXich.setProduct(xucXich);
            themXucXich.setQuantityNeeded(0.05);
            toppingRepository.save(themXucXich);

            // Topping: Đế Dày (Giá 10k, tốn thêm 0.1kg Bột mì)
            Topping deDay = new Topping();
            deDay.setName("Đế Bánh Dày");
            deDay.setPrice(10000.0);
            deDay.setProduct(botMi);
            deDay.setQuantityNeeded(0.1);
            toppingRepository.save(deDay);

            System.out.println("   + Đã tạo 3 loại Topping.");
        }

        System.out.println(">>> SEEDING MENU HOÀN TẤT!");
    }

    // --- HELPER METHODS ---

    // Hàm hỗ trợ tạo Pizza nhiều size (Đã thêm tham số imageUrl)
    private void createPizza(Category cat, String name, String desc, String imageUrl,
                             Product bot, Product phomai, Product sot, Product topping) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(desc);
        dish.setImageUrl(imageUrl); // <--- Set Image URL
        dish.setCategory(cat);
        dish.setAvailable(true);

        // Size M
        addVariant(dish, "M", 120000.0, bot, 0.2, phomai, 0.1, topping, 0.1);
        // Size L
        addVariant(dish, "L", 180000.0, bot, 0.3, phomai, 0.15, topping, 0.15);
        // Size XL
        addVariant(dish, "XL", 250000.0, bot, 0.4, phomai, 0.2, topping, 0.2);

        dishRepository.save(dish);
    }

    // Hàm hỗ trợ tạo món đơn (Đã thêm tham số imageUrl và desc)
    private void createSingleItem(Category cat, String name, String desc, String imageUrl,
                                  Double price, Product p1, double q1, Product p2, double q2) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(desc); // <--- Set Description
        dish.setImageUrl(imageUrl); // <--- Set Image URL
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