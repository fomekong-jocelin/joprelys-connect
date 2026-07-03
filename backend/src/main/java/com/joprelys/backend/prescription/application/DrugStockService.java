package com.joprelys.backend.prescription.application;

import com.joprelys.backend.prescription.api.CreateDrugStockRequest;
import com.joprelys.backend.prescription.api.DrugStockResponse;
import com.joprelys.backend.prescription.infrastructure.persistence.DrugStockEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.DrugStockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Gestion réelle des stocks de médicaments.
 * Utilise le verrouillage optimiste JPA (@Version) pour éviter les conflits de concurrence.
 * STORY-1103.
 */
@Service
public class DrugStockService {

    private final DrugStockRepository drugStockRepository;

    public DrugStockService(DrugStockRepository drugStockRepository) {
        this.drugStockRepository = drugStockRepository;
    }

    @Transactional(readOnly = true)
    public List<DrugStockResponse> listStocks() {
        return drugStockRepository.findAllByOrderByDrugNameAsc()
                .stream()
                .map(DrugStockResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DrugStockResponse> getLowStockAlerts() {
        return drugStockRepository.findBelowThreshold()
                .stream()
                .map(DrugStockResponse::fromEntity)
                .toList();
    }

    @Transactional
    public DrugStockResponse createOrUpdateStock(CreateDrugStockRequest request) {
        DrugStockEntity entity = drugStockRepository
                .findByDrugNameIgnoreCase(request.drugName())
                .orElseGet(() -> new DrugStockEntity(
                        request.drugName(),
                        request.genericName(),
                        request.unit() != null ? request.unit() : "comprime",
                        0,
                        request.minimumThreshold(),
                        request.batchNumber(),
                        request.expiryDate(),
                        request.supplier()
                ));

        entity.setQuantityAvailable(request.quantityAvailable());
        entity.setMinimumThreshold(request.minimumThreshold());
        if (request.genericName() != null) entity.setGenericName(request.genericName());
        if (request.unit() != null) entity.setUnit(request.unit());
        if (request.batchNumber() != null) entity.setBatchNumber(request.batchNumber());
        if (request.expiryDate() != null) entity.setExpiryDate(request.expiryDate());
        if (request.supplier() != null) entity.setSupplier(request.supplier());

        return DrugStockResponse.fromEntity(drugStockRepository.save(entity));
    }

    /**
     * Vérifie la disponibilité et décrémente le stock si suffisant.
     * Lève 409 Conflict si le stock est insuffisant.
     * Le verrouillage optimiste JPA (@Version) gère la concurrence.
     */
    @Transactional
    public void checkAndDecrementStock(String drugName, int quantityRequired) {
        DrugStockEntity stock = drugStockRepository
                .findByDrugNameIgnoreCase(drugName)
                .orElse(null);

        // Si le médicament n'est pas référencé dans les stocks, on l'ignore (stock optionnel)
        if (stock == null) return;

        if (stock.getQuantityAvailable() < quantityRequired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuffisant pour " + drugName +
                    " (disponible: " + stock.getQuantityAvailable() +
                    ", demandé: " + quantityRequired + ")");
        }

        stock.decrementStock(quantityRequired);
        drugStockRepository.save(stock);
    }
}
