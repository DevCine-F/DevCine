package com.devcine.backend.config;

import com.devcine.backend.entity.FnbComboSlot;
import com.devcine.backend.entity.FnbItem;
import com.devcine.backend.entity.FnbOptionGroup;
import com.devcine.backend.entity.FnbOptionItem;
import com.devcine.backend.entity.SystemSetting;
import com.devcine.backend.repository.FnbComboSlotRepository;
import com.devcine.backend.repository.FnbItemRepository;
import com.devcine.backend.repository.FnbOptionGroupRepository;
import com.devcine.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(100) // Ensure it runs after other basic seeders if needed
@RequiredArgsConstructor
public class FnbDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final FnbOptionGroupRepository fnbOptionGroupRepository;
    private final FnbItemRepository fnbItemRepository;
    private final FnbComboSlotRepository fnbComboSlotRepository;
    private final SystemSettingRepository systemSettingRepository;

    /** Cờ seed một-lần. TUYỆT ĐỐI không TRUNCATE F&B mỗi lần boot (mất menu + lịch sử đơn F&B). */
    private static final String SEED_FLAG = "FNB_POOLS_SEEDED_V1";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Chạy MỘT LẦN: bỏ qua nếu đã seed HOẶC đã có dữ liệu F&B (bảo toàn menu Admin chỉnh tay + lịch sử).
        boolean seeded = systemSettingRepository.findById(SEED_FLAG).isPresent();
        if (seeded || fnbItemRepository.count() > 0) {
            if (!seeded) {
                systemSettingRepository.save(SystemSetting.builder().settingKey(SEED_FLAG).settingValue("true").build());
            }
            System.out.println("[FnbDataSeeder] Bỏ qua re-seed F&B (đã có dữ liệu / đã seed trước đó).");
            return;
        }

        System.out.println("====== BẮT ĐẦU SEED F&B THEO MÔ HÌNH POOLS & SLOTS (lần đầu) ======");

        // 1. Wipe everything related to old F&B model (chỉ khi DB rỗng — dọn tàn dư mô hình cũ)
        jdbcTemplate.execute("TRUNCATE TABLE fnb_item_slots, fnb_option_items, fnb_option_groups, fnb_items, booking_fnb_options, concession_sale_item_options CASCADE;");

        System.out.println("Đã wipe sạch tàn dư F&B cũ (clean slate).");

        // 2. Create Option Pools & Items
        FnbOptionGroup popcornPool = FnbOptionGroup.builder().name("Tùy Chọn Bắp").build();
        FnbOptionItem sweetPopcorn = FnbOptionItem.builder().group(popcornPool).name("Bắp Ngọt L").surchargePrice(BigDecimal.ZERO).build();
        FnbOptionItem cheesePopcorn = FnbOptionItem.builder().group(popcornPool).name("Bắp Phô Mai L").surchargePrice(new BigDecimal("10000")).build();
        FnbOptionItem caramelPopcorn = FnbOptionItem.builder().group(popcornPool).name("Bắp Caramel L").surchargePrice(new BigDecimal("10000")).build();
        FnbOptionItem mixPopcorn = FnbOptionItem.builder().group(popcornPool).name("Bắp 2 Vị Phô Mai & Caramel").surchargePrice(new BigDecimal("20000")).build();
        
        popcornPool.getItems().add(sweetPopcorn);
        popcornPool.getItems().add(cheesePopcorn);
        popcornPool.getItems().add(caramelPopcorn);
        popcornPool.getItems().add(mixPopcorn);
        
        popcornPool = fnbOptionGroupRepository.save(popcornPool);

        FnbOptionGroup drinkPool = FnbOptionGroup.builder().name("Tùy Chọn Nước").build();
        FnbOptionItem pepsi = FnbOptionItem.builder().group(drinkPool).name("Pepsi 32oz").surchargePrice(BigDecimal.ZERO).build();
        FnbOptionItem sevenUp = FnbOptionItem.builder().group(drinkPool).name("7Up 32oz").surchargePrice(BigDecimal.ZERO).build();
        FnbOptionItem nestea = FnbOptionItem.builder().group(drinkPool).name("Trà Vải Nestea L").surchargePrice(new BigDecimal("10000")).build();
        FnbOptionItem sodaAde = FnbOptionItem.builder().group(drinkPool).name("Soda ADE Xoài").surchargePrice(new BigDecimal("10000")).build();
        
        drinkPool.getItems().add(pepsi);
        drinkPool.getItems().add(sevenUp);
        drinkPool.getItems().add(nestea);
        drinkPool.getItems().add(sodaAde);
        
        drinkPool = fnbOptionGroupRepository.save(drinkPool);

        System.out.println("Đã tạo 2 Pool (Tùy Chọn Bắp & Tùy Chọn Nước).");

        // Prepare some items mapping logic to fetch saved items
        FnbOptionItem defaultPopcorn = popcornPool.getItems().stream().filter(i -> i.getName().equals("Bắp Ngọt L")).findFirst().orElse(null);
        FnbOptionItem defaultDrink = drinkPool.getItems().stream().filter(i -> i.getName().equals("Pepsi 32oz")).findFirst().orElse(null);

        // 3. Create Combos & Assign Slots
        FnbItem comboSolo = FnbItem.builder().name("Combo Solo").type("COMBO").price(new BigDecimal("89000"))
                .description("1 bắp ngọt lớn + 1 nước ngọt lớn").isActive(true).build();
        comboSolo = fnbItemRepository.save(comboSolo);
        fnbComboSlotRepository.save(FnbComboSlot.builder().fnbItem(comboSolo).optionGroup(popcornPool).slotLabel("Ô chọn Bắp")
                .defaultOptionItem(defaultPopcorn).displayOrder(1).minChoices(1).maxChoices(1).isRequired(true).build());
        fnbComboSlotRepository.save(FnbComboSlot.builder().fnbItem(comboSolo).optionGroup(drinkPool).slotLabel("Ô chọn Nước")
                .defaultOptionItem(defaultDrink).displayOrder(2).minChoices(1).maxChoices(1).isRequired(true).build());

        FnbItem comboCouple = FnbItem.builder().name("Combo Couple").type("COMBO").price(new BigDecimal("129000"))
                .description("1 bắp lớn + 2 nước lớn").isActive(true).build();
        comboCouple = fnbItemRepository.save(comboCouple);
        fnbComboSlotRepository.save(FnbComboSlot.builder().fnbItem(comboCouple).optionGroup(popcornPool).slotLabel("Ô chọn Bắp")
                .defaultOptionItem(defaultPopcorn).displayOrder(1).minChoices(1).maxChoices(1).isRequired(true).build());
        fnbComboSlotRepository.save(FnbComboSlot.builder().fnbItem(comboCouple).optionGroup(drinkPool).slotLabel("Ô chọn Nước 1")
                .defaultOptionItem(defaultDrink).displayOrder(2).minChoices(1).maxChoices(1).isRequired(true).build());
        fnbComboSlotRepository.save(FnbComboSlot.builder().fnbItem(comboCouple).optionGroup(drinkPool).slotLabel("Ô chọn Nước 2")
                .defaultOptionItem(defaultDrink).displayOrder(3).minChoices(1).maxChoices(1).isRequired(true).build());

        System.out.println("Đã tạo Combos và gán Slots thành công!");

        // Đặt cờ để các lần khởi động sau KHÔNG re-seed/wipe nữa.
        systemSettingRepository.save(SystemSetting.builder().settingKey(SEED_FLAG).settingValue("true").build());
        System.out.println("================================================================");
    }
}
