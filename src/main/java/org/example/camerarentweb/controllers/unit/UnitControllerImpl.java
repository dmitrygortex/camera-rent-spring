//package org.example.camerarentweb.controllers.unit;
//
//import org.example.camerarentcontracts.controllers.unit.UnitController;
//import org.example.camerarentcontracts.viewmodel.collective.BaseViewModel;
//import org.example.camerarentcontracts.viewmodel.pages.unit.EquipmentTypePageViewModel;
//import org.example.camerarentcontracts.viewmodel.pages.unit.rent.RentUnitForm;
//import org.example.camerarentweb.controllers.BaseControllerImpl;
//import org.example.camerarentweb.entities.EquipmentUnit;
//import org.example.camerarentweb.services.EquipmentUnitService;
//import org.example.camerarentweb.services.OrderService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//
//
//@Controller
//public class UnitControllerImpl extends BaseControllerImpl implements UnitController {
//
//     @Autowired
//     private EquipmentUnitService equipmentUnitService;
//
//     @Autowired
//     private OrderService orderService;
//
//    @Override
//    public String unitPage(String type, Model model) {
//        BaseViewModel viewModel = createBaseViewModel("Unit Page");
//        // доделать
//        model.addAttribute("viewModel", viewModel);
//        return "unit/unitPage";
//    }
//    @Override
//    public BaseViewModel createBaseViewModel(String title) {
//        return new BaseViewModel(title);
//    }
//
//    @Override
//    public String unitPage(String type, EquipmentTypePageViewModel model) {
//        return "";
//    }
//
//    @Override
//    public String rent(String type, RentUnitForm form, BindingResult bindingResult, Model model) {
//        return "";
//    }
//}


package org.example.camerarentweb.controllers.unit;

import org.example.camerarentcontracts.controllers.unit.UnitController;
import org.example.camerarentcontracts.viewmodel.pages.catalog.EquipmentViewModel;
import org.example.camerarentcontracts.viewmodel.pages.unit.EquipmentTypePageViewModel;
import org.example.camerarentcontracts.viewmodel.pages.unit.ReviewViewModel;
import org.example.camerarentcontracts.viewmodel.pages.unit.UserViewModel;
import org.example.camerarentweb.controllers.BaseControllerImpl;
import org.example.camerarentweb.dto.EquipmentTypeCardDto;
import org.example.camerarentweb.dto.EquipmentTypeRatedCardDto;
import org.example.camerarentweb.dto.ReviewDto;
import org.example.camerarentweb.entities.EquipmentStatus;
import org.example.camerarentweb.entities.EquipmentUnit;
import org.example.camerarentweb.entities.User;
import org.example.camerarentweb.entities.UserRole;
import org.example.camerarentweb.services.EquipmentTypeDomainService;
import org.example.camerarentweb.services.EquipmentUnitService;
import org.example.camerarentweb.services.OrderService;
import org.example.camerarentweb.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/units")
public class UnitControllerImpl extends BaseControllerImpl {

    private final EquipmentTypeDomainService equipmentTypeDomainService;
    private final ReviewService reviewService;
    private final EquipmentUnitService equipmentUnitService;
    private final OrderService orderService;

    @Autowired
    public UnitControllerImpl(EquipmentTypeDomainService equipmentTypeDomainService,
                              ReviewService reviewService,
                              EquipmentUnitService equipmentUnitService,
                              OrderService orderService) {
        this.equipmentTypeDomainService = equipmentTypeDomainService;
        this.reviewService = reviewService;
        this.equipmentUnitService = equipmentUnitService;
        this.orderService = orderService;
    }

    @GetMapping("/{name}")
    public String viewEquipmentCard(@PathVariable String name, Model model) {
        EquipmentTypeCardDto equipment = equipmentTypeDomainService.findByName(name);
        if (equipment == null) {
            return "error/404";
        }

        List<ReviewDto> listOfReviewModels = reviewService.findByEquipmentTypeId(equipment.getId());
        System.out.println("ID in List<ReviewDto> listOfReviewModels = reviewService.findByEquipmentTypeId(equipment.getId()); is \n" + equipment.getId()+ "\n\n");
        System.out.println(listOfReviewModels.size());
        List<ReviewViewModel> reviewViewModels = listOfReviewModels.stream()
                .map(r -> new ReviewViewModel(
                        new UserViewModel
                                (r.getUser().getFirstName() + " " + r.getUser().getLastName(),
                                r.getUser().getEmail(),
                                        r.getUser().getRole().toString()
                                ),
                        r.getRate(),
                        r.getHeader(),
                        r.getAdvantages(),
                        r.getDisadvantages(),
                        r.getCommentary()
                )).toList();


        String categoryName = equipment.getCategory();
        var firstUpperCaseCategoryName =  categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1);
        EquipmentTypePageViewModel viewModel = new EquipmentTypePageViewModel(
                createBaseViewModel("Page of " + name),
                equipment.getId(),
                "" + firstUpperCaseCategoryName,
                "" + categoryName,
                equipment.getName(),
                equipment.getRating(),
                equipment.getDescription(),
                equipment.getPricePerDay(),
                equipment.getImageUrl(),
                equipment.getCharacterization(),
                reviewViewModels

        );

        //System.out.println(reviewViewModels.stream().toString());
        for (int i = 0; i < reviewViewModels.size(); i++) {
            System.out.println(reviewViewModels.get(i));
        }


        model.addAttribute("model", viewModel);
        return "unit-page";
    }
    @PostMapping("/book")
    public String bookEquipment(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("equipmentId") int equipmentId,
            Model model
    ) {
        try {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atStartOfDay();

            // сюда скеьюрити подключу и норм будет
            User user = getCurrentUser();

            EquipmentUnit equipmentUnit = equipmentUnitService.findById(equipmentId);
            if (equipmentUnit == null) {
                model.addAttribute("error", "Оборудование не найдено");
                return "error/404";
            }

            orderService.createOrder(user, List.of(equipmentUnit), start, end);

            return "redirect:/orders/success";
        } catch (Exception e) {
            model.addAttribute("error", "Произошла ошибка при бронировании");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "error/rent";
        }
    }

    private User getCurrentUser() {
        // Заглушка для текущего пользователя
        return new User(
                "89090073399",
                "toopseeecreet))",
                "somemaaail@mail.ru",
                "Belfort",
                "Jordan",
                UserRole.USER
        );
    }
}