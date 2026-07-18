package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.application.DrugStockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des stocks de médicaments en pharmacie.
 * Réservé aux rôles PHARMACIEN et ADMIN_CLINIQUE.
 * STORY-1103.
 */
@RestController
@RequestMapping("/api/pharmacy/stocks")
@PreAuthorize("hasAuthority('PHARMACY_STOCK_MANAGE')")
public class DrugStockController {

    private final DrugStockService drugStockService;

    public DrugStockController(DrugStockService drugStockService) {
        this.drugStockService = drugStockService;
    }

    @GetMapping
    public List<DrugStockResponse> listStocks() {
        return drugStockService.listStocks();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DrugStockResponse createOrUpdateStock(@Valid @RequestBody CreateDrugStockRequest request) {
        return drugStockService.createOrUpdateStock(request);
    }

    @GetMapping("/alerts")
    public List<DrugStockResponse> getLowStockAlerts() {
        return drugStockService.getLowStockAlerts();
    }
}
