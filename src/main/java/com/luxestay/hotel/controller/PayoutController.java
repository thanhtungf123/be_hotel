package com.luxestay.hotel.controller;

import com.luxestay.hotel.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.core.Page;
import vn.payos.model.v1.payouts.GetPayoutListParams;
import vn.payos.model.v1.payouts.GetPayoutListParams.GetPayoutListParamsBuilder;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutRequests;
import vn.payos.model.v1.payouts.batch.PayoutBatchItem;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.model.v1.payoutsAccount.PayoutAccountInfo;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {
    private final PayOS payOS;

    @Autowired
    public PayoutController(PayOS payOS) {
        this.payOS = payOS;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Payout>> createPayout(@RequestBody PayoutRequests body) {
        try {
            if (body.getReferenceId() == null || body.getReferenceId().isEmpty()) {
                body.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
            }
            Payout payout = payOS.payouts().create(body);
            return ResponseEntity.ok(ApiResponse.success(payout));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create payout: " + e.getMessage()));
        }
    }

    @PostMapping("/batch/create")
    public ResponseEntity<ApiResponse<Payout>> createBatchPayout(@RequestBody PayoutBatchRequest body) {
        try {
            if (body.getReferenceId() == null || body.getReferenceId().isEmpty()) {
                body.setReferenceId("payout_" + (System.currentTimeMillis() / 1000));
            }

            List<PayoutBatchItem> payoutsList = body.getPayouts();
            if (payoutsList == null || payoutsList.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Payout list cannot be empty."));
            }
            for (int i = 0; i < payoutsList.size(); i++) {
                PayoutBatchItem batchItem = payoutsList.get(i);
                if (batchItem.getReferenceId() == null) {
                    batchItem.setReferenceId("payout_" + (System.currentTimeMillis() / 1000) + "_" + i);
                }
            }

            Payout payout = payOS.payouts().batch().create(body);
            return ResponseEntity.ok(ApiResponse.success(payout));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create batch payout: " + e.getMessage()));
        }
    }

    @GetMapping("/{payoutId}")
    public ResponseEntity<ApiResponse<Payout>> retrievePayout(@PathVariable String payoutId) {
        try {
            Payout payout = payOS.payouts().get(payoutId);
            return ResponseEntity.ok(ApiResponse.success(payout));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to retrieve payout: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Payout>>> retrievePayoutList(
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String approvalState,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        try {
            GetPayoutListParamsBuilder paramsBuilder =
                    GetPayoutListParams.builder()
                            .referenceId(referenceId)
                            .category(category)
                            .limit(limit)
                            .offset(offset);

            if (fromDate != null && !fromDate.isEmpty()) {
                paramsBuilder.fromDate(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                paramsBuilder.toDate(toDate);
            }

            PayoutApprovalState parsedApprovalState = null;
            if (approvalState != null && !approvalState.isEmpty()) {
                try {
                    parsedApprovalState = PayoutApprovalState.valueOf(approvalState.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid approval state: " + approvalState));
                }
                paramsBuilder.approvalState(parsedApprovalState);
            }

            GetPayoutListParams params = paramsBuilder.build();
            List<Payout> data = new ArrayList<>();
            Page<Payout> page = payOS.payouts().list(params);
            page.autoPager().stream().forEach(data::add);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to retrieve payout list: " + e.getMessage()));
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<PayoutAccountInfo>> getAccountBalance() {
        try {
            PayoutAccountInfo accountInfo = payOS.payoutsAccount().balance();
            return ResponseEntity.ok(ApiResponse.success(accountInfo));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to get account balance: " + e.getMessage()));
        }
    }
}